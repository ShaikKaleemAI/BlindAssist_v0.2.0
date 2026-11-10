package com.kaleem.blindassist

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.min

/**
 * Runs the whole assistant on the phone, with the screen off and the phone in a pocket:
 * camera -> detector -> range fusion -> tracker -> alerts -> speech / panned beeps / vibration.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
class AssistService : LifecycleService() {
    companion object {
        const val ACTION_STOP = "com.kaleem.blindassist.STOP"
        @Volatile var running = false
        @Volatile private var crashHandlerInstalled = false
    }

    private val main = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadExecutor()
    private var speech: Speech? = null
    private var ocr: OcrReader? = null
    private val ocrExec = Executors.newSingleThreadExecutor()
    @Volatile private var lastFrame: Bitmap? = null
    private var wakeT = 0.0
    private var motion: MotionMonitor? = null
    private val voice: VoiceEngine = NullVoice()
    private val sos = SosCountdown(15.0)
    @Volatile private var sosAlarming = false
    private var earcons: Earcons? = null
    private var sensors: PocketSensors? = null
    private var detector: Detector? = null
    @Volatile private var pipeline: Pipeline? = null
    private var wake: PowerManager.WakeLock? = null
    private var session: MediaSession? = null
    private var metrics: MetricsLogger? = null

    @Volatile private var lastFrameT = 0.0
    @Volatile private var armedAt = Double.MAX_VALUE
    private var cameraLost = false
    private var slowCount = 0
    private var darkSince = 0.0
    private var lastWarnT = 0.0
    private val lang get() = Cfg.lang

    private fun nowS() = SystemClock.elapsedRealtime() / 1000.0

    override fun onCreate() {
        super.onCreate()
        running = true
        val sp = getSharedPreferences("ba", Context.MODE_PRIVATE)
        Cfg.lang = sp.getString("lang", "en") ?: "en"
        Cfg.camHeightM = sp.getFloat("height_cm", 110f) / 100.0
        startFg()
        getSharedPreferences("ba", Context.MODE_PRIVATE).edit().putBoolean("running_flag", true).apply()
        if (!crashHandlerInstalled) {                                   // install once per process, not per service start
            crashHandlerInstalled = true
            val app = applicationContext
            val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                try { vibrateOnce(app) } catch (_: Throwable) {}         // last-gasp haptic: the app is dying silently
                prevHandler?.uncaughtException(t, e)
            }
        }
        metrics = try { MetricsLogger(java.io.File(filesDir, "metrics.csv")) } catch (e: Exception) { null }
        speech = Speech(this, Cfg.lang)
        earcons = Earcons()
        sensors = PocketSensors(this).also { it.start() }
        motion = MotionMonitor(this) { main.post { onFall() } }.also { it.start() }
        if (voice.available) voice.start { text -> main.post { onTranscript(text) } }
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "blindassist:run").also { it.acquire(4 * 60 * 60 * 1000L) }
        detector = createDetector(this)
        if (detector?.limited == true) {
            speech?.say(I18n.t(lang, "limited_mode"), P_CRITICAL, 15.0, "limited")
            vibrateOnce(this)
            main.postDelayed(limitedNag, 60_000)
        }
        speech?.say(I18n.t(lang, "starting"), P_ANSWER, 30.0, "start")
        armedAt = nowS() + 10.0
        wakeT = nowS()
        main.postDelayed({ speech?.say(I18n.t(lang, "ready"), P_ANSWER, 5.0, "ready") }, 10_000)
        bindCamera()
        setupSession()
        main.postDelayed(watchdog, 2000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    private fun startFg() {
        val ch = "blindassist"
        if (Build.VERSION.SDK_INT >= 26)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(ch, "Blind Assist", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, AssistService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, ch).setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Blind Assist is running").setOngoing(true).addAction(0, "Stop", stop).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        else startForeground(1, n)
    }

    private fun bindCamera() {
        val f = ProcessCameraProvider.getInstance(this)
        f.addListener({
            try {
                val provider = f.get()
                val rs = ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()
                val analysis = ImageAnalysis.Builder().setResolutionSelector(rs)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(exec) { img -> analyze(img) }
                provider.unbindAll()
                val cam = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
                readHfov(cam)?.let { Cfg.hfovDeg = it }
                pipeline = Pipeline(Cam(Cfg.hfovDeg, Cfg.camHeightM, Cfg.defaultPitchDeg), Cfg.lang)
                lastFrameT = nowS()
            } catch (e: Exception) { Log.e("BA", "camera bind failed", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    /** Real horizontal field of view in PORTRAIT (uses the sensor's short side). */
    private fun readHfov(cam: Camera): Double? = try {
        val c2 = Camera2CameraInfo.from(cam.cameraInfo)
        val fl = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
        val sz = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        if (fl != null && sz != null) {
            val d = Math.toDegrees(2 * atan(min(sz.width, sz.height) / (2.0 * fl)))
            if (d in 30.0..90.0) d else null
        } else null
    } catch (e: Exception) { null }

    private fun analyze(img: ImageProxy) {
        try {
            if (thermalSkip > 0 && (frameCounter++ % (thermalSkip + 1)) != 0) return   // thermal throttling
            val p = pipeline ?: return
            val det = detector ?: return
            val sen = sensors ?: return
            val t0 = SystemClock.elapsedRealtime()
            val now = t0 / 1000.0
            lastFrameT = now
            val rot = img.imageInfo.rotationDegrees + if (sen.upsideDown()) 180 else 0
            val raw = img.toBitmap()
            val up = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
            lastFrame = up
            checkHealth(up, now, sen.pitchDeg())
            val evs = p.process(det.detect(up), now, up.width.toDouble(), up.height.toDouble(), sen.pitchDeg())
            dispatch(evs, now)
            val ms = SystemClock.elapsedRealtime() - t0
            logMetrics(now, ms.toDouble())
            slowCount = if (ms > 450) slowCount + 1 else 0
            if (slowCount == 5 && now >= armedAt) speech?.say(I18n.t(lang, "slow"), P_ANSWER, 5.0, "slow")
        } catch (e: Throwable) { Log.e("BA", "analyze", e) } finally { img.close() }
    }

    private var lastBatT = -1e9
    private var lastTemp = Double.NaN
    private var lastPct = -1
    private var lastHotT = -1e9
    @Volatile private var thermalSkip = 0
    private var frameCounter = 0

    private fun logMetrics(now: Double, ms: Double) {
        try {
            if (now - lastBatT >= 5.0) {                       // battery/temperature polled every 5 s, not per frame
                lastBatT = now
                val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val raw = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
                lastTemp = if (raw == Int.MIN_VALUE) Double.NaN else raw / 10.0
                val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                lastPct = if (lvl < 0) -1 else lvl * 100 / scale
                thermalSkip = ThermalPolicy.frameSkip(lastTemp)
                if (ThermalPolicy.shouldWarn(lastTemp) && now - lastHotT > 300) {
                    lastHotT = now
                    speech?.say(I18n.t(lang, "hot"), P_ANSWER, 10.0, "hot")
                }
            }
            metrics?.frame(now, ms, lastTemp, lastPct, detector?.limited == true)
        } catch (_: Exception) {}
    }

    private fun dispatch(evs: List<Ev>, now: Double) {
        if (now < armedAt) return
        for (e in evs) when (e) {
            is Ev.Speech -> speech?.say(e.text, e.pri, e.ttl, e.key)
            is Ev.Beep -> earcons?.beep(e.pan, e.freq, e.dur)
            Ev.Vibrate -> vibrateOnce(this)
        }
    }

    private fun checkHealth(b: Bitmap, now: Double, pitch: Double) {
        if (now < armedAt) return
        var sum = 0.0
        val sx = b.width / 16; val sy = b.height / 16
        for (yy in 0 until 16) for (xx in 0 until 16) {
            val c = b.getPixel(xx * sx + sx / 2, yy * sy + sy / 2)
            sum += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
        }
        if (sum / 256 < 15) {
            if (darkSince == 0.0) darkSince = now
            if (now - darkSince > 2 && now - lastWarnT > 10) { lastWarnT = now; speech?.say(I18n.t(lang, "covered"), P_ANSWER, 5.0, "covered") }
        } else darkSince = 0.0
        if (abs(pitch) > 55 && now - lastWarnT > 10) { lastWarnT = now; speech?.say(I18n.t(lang, "not_upright"), P_ANSWER, 5.0, "upright") }
    }

    /** In limited mode (no detector) repeat the warning every minute: the user must never forget it is blind. */
    private val limitedNag = object : Runnable {
        override fun run() {
            speech?.say(I18n.t(lang, "limited_mode"), P_CRITICAL, 15.0, "limited")
            vibrateOnce(this@AssistService)
            main.postDelayed(this, 60_000)
        }
    }

    private val watchdog = object : Runnable {
        override fun run() {
            val now = nowS()
            if (now - wakeT > 3 * 3600) {                         // wake lock has a 4 h timeout: refresh before it lapses
                wakeT = now
                try { if (wake?.isHeld == true) wake?.release(); wake?.acquire(4 * 60 * 60 * 1000L) } catch (_: Exception) {}
            }
            if (now > armedAt) {
                val stalled = now - lastFrameT > 1.5
                if (stalled && !cameraLost) { cameraLost = true; speech?.say(I18n.t(lang, "camera_lost"), P_CRITICAL, 5.0, "lost") }
                if (!stalled && cameraLost) { cameraLost = false; speech?.say(I18n.t(lang, "camera_back"), P_ANSWER, 5.0, "back") }
            }
            main.postDelayed(this, 1000)
        }
    }

    /** Earphone button (single press) = "what is around me". */
    private fun setupSession() {
        val s = MediaSession(this, "BlindAssist")
        s.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = summarize()
            override fun onPause() = summarize()
            override fun onSkipToNext() = readThis()
        })
        s.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
            .setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
        s.isActive = true
        session = s
    }

    // ---- fall -> countdown -> LOCAL alarm only. No message/call is sent anywhere (not implemented). ----
    private val sosTick = object : Runnable {
        override fun run() {
            if (!sos.active) return
            if (sos.tick(nowS())) sosAlarm() else main.postDelayed(this, 1000)
        }
    }

    private fun onFall() {
        if (sos.active || sosAlarming) return
        speech?.say(I18n.t(lang, "fall_alert"), P_CRITICAL, 20.0, "fall")
        vibrateOnce(this)
        sos.start(nowS()); main.postDelayed(sosTick, 1000)
    }

    private fun sosAlarm() {
        sosAlarming = true
        speech?.say(I18n.t(lang, "sos_expired"), P_CRITICAL, 30.0, "sos")
        vibrateOnce(this)
        main.postDelayed({ if (sosAlarming) sosAlarm() }, 10_000)
    }

    private fun cancelSos() { sos.cancel(); sosAlarming = false; main.removeCallbacks(sosTick) }

    // ---- voice: wake word -> parser -> actions (no speech engine is shipped, see docs/VOICE_PLAN.md) ----
    private fun onTranscript(text: String) {
        val rest = WakeWord.strip(lang, text) ?: return
        CommandDispatcher.dispatch(VoiceCommands.parse(lang, rest), actions)
    }

    private fun na() { speech?.say(I18n.t(lang, "not_available"), P_ANSWER, 10.0, "na") }

    private val actions = object : AssistActions {
        override fun whatsAhead() = summarize()
        override fun readThis() = this@AssistService.readThis()
        override fun describe() = na()                       // no scene-description model offline
        override fun find(what: String) = na()
        override fun navigate(place: String) = na()          // no GPS source / routing yet
        override fun lost() { speech?.say(I18n.t(lang, "lost_msg"), P_CRITICAL, 20.0, "lostmsg") }
        override fun callFamily() = na()
        override fun setVerbosity(v: Verbosity) { Cfg.verbose = (v == Verbosity.DETAILED) }
        override fun stop() { stopSelf() }
        override fun unknown() {}
    }

    /** Earphone "next" (usually double press) = read text in the current camera frame. OCR is Latin script only. UNVERIFIED on device. */
    private fun readThis() {
        cancelSos()
        val frame = lastFrame
        if (frame == null) { speech?.say(I18n.t(lang, "nothing"), P_ANSWER, 10.0, "read"); return }
        ocrExec.execute {
            val r = ocr ?: OcrReader().also { ocr = it }
            speech?.say(ReadThis.speak(r.read(frame)), P_ANSWER, 20.0, "read")
        }
    }

    private fun summarize() {
        cancelSos()
        val now = nowS()
        val near = (pipeline?.lastTracks ?: emptyList()).filter { it.z != null && now - it.tLast < 1.0 }
            .sortedBy { it.z }.take(3)
        val text = if (near.isEmpty()) I18n.t(lang, "nothing")
        else near.joinToString(" ") {
            I18n.t(lang, "obstacle", "label" to I18n.label(lang, it.label),
                "dir" to I18n.direction(lang, it.bearing), "dist" to I18n.dist(lang, it.z))
        }
        speech?.say(text, P_ANSWER, 10.0, "summary")
    }

    override fun onDestroy() {
        running = false
        main.removeCallbacksAndMessages(null)
        speech?.say(I18n.t(lang, "stopped"), P_CRITICAL, 3.0, "bye")
        val sp = speech
        Handler(Looper.getMainLooper()).postDelayed({ sp?.shutdown() }, 2500)
        session?.release()
        sensors?.stop()
        earcons?.shutdown()
        exec.shutdown()
        ocrExec.shutdown()
        motion?.stop()
        voice.stop()
        main.removeCallbacks(sosTick)
        sosAlarming = false
        ocr?.close()
        getSharedPreferences("ba", Context.MODE_PRIVATE).edit().putBoolean("running_flag", false).apply()
        try { detector?.close() } catch (_: Exception) {}
        try { if (wake?.isHeld == true) wake?.release() } catch (_: Exception) {}
        super.onDestroy()
    }
}

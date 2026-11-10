package com.kaleem.blindassist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** One giant START/STOP button (TalkBack friendly) + two settings set once by a sighted helper. */
class MainActivity : AppCompatActivity() {
    private lateinit var btn: Button
    private lateinit var status: TextView
    private lateinit var heightEt: EditText
    private lateinit var rg: RadioGroup
    private val langs = listOf("en" to "English", "hi" to "हिन्दी", "te" to "తెలుగు")

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startAssist()
        else status.text = "Camera permission is required."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences("ba", Context.MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }

        root.addView(TextView(this).apply { text = "Blind Assist"; textSize = 28f })
        root.addView(TextView(this).apply {
            text = "Camera height from the ground when the phone is in your pocket (cm). Shirt pocket ≈ 120, trouser pocket ≈ 85."
            textSize = 14f
        })
        heightEt = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(sp.getFloat("height_cm", 110f).toInt().toString())
            contentDescription = "Camera height in centimetres"
        }
        root.addView(heightEt)

        rg = RadioGroup(this)
        val saved = sp.getString("lang", "en")
        langs.forEachIndexed { i, p ->
            rg.addView(RadioButton(this).apply { id = 100 + i; text = p.second; textSize = 20f; isChecked = p.first == saved })
        }
        root.addView(rg)

        btn = Button(this).apply { textSize = 32f; gravity = Gravity.CENTER; setOnClickListener { toggle() } }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        status = TextView(this).apply {
            textSize = 14f
            text = "This is an aid, not a replacement for your white cane or guide dog. Phone must be upright, back camera facing forward and uncovered."
        }
        root.addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        OemHints.hintFor(Build.MANUFACTURER)?.let { status.append("\n" + it) }
        if (sp.getBoolean("running_flag", false) && !AssistService.running) {
            status.append("\nBlind Assist stopped unexpectedly last time. Check battery settings.")
            sp.edit().putBoolean("running_flag", false).apply()
        }
        root.addView(Button(this).apply {
            text = "Battery settings (keep running with screen off)"
            setOnClickListener { try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {} }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val on = AssistService.running
        btn.text = if (on) "STOP" else "START"
        btn.contentDescription = if (on) "Stop Blind Assist" else "Start Blind Assist. You will have ten seconds to put the phone in your pocket."
    }

    private fun toggle() {
        if (AssistService.running) {
            startService(Intent(this, AssistService::class.java).setAction(AssistService.ACTION_STOP))
            btn.postDelayed({ refresh() }, 400)
            return
        }
        val h = heightEt.text.toString().toFloatOrNull()?.coerceIn(50f, 160f) ?: 110f
        val lang = langs.getOrNull(rg.checkedRadioButtonId - 100)?.first ?: "en"
        getSharedPreferences("ba", Context.MODE_PRIVATE).edit().putFloat("height_cm", h).putString("lang", lang).apply()
        val need = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) need += Manifest.permission.POST_NOTIFICATIONS
        val missing = need.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startAssist() else permLauncher.launch(missing.toTypedArray())
    }

    private fun startAssist() {
        ContextCompat.startForegroundService(this, Intent(this, AssistService::class.java))
        btn.text = "STOP"
        status.text = "Running. Put the phone in your pocket. Lock the screen if you like; it keeps running."
        btn.postDelayed({ refresh() }, 600)
    }
}

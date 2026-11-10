package com.kaleem.blindassist

import kotlin.math.abs
import kotlin.math.roundToInt

/** NOTE: Hindi/Telugu wording has not been reviewed by a native speaker - please review before real use. */
object I18n {
    /** Hindi and Telugu strings are machine-drafted. NEEDS NATIVE REVIEW before any real-world use. */
    const val NEEDS_NATIVE_REVIEW = true

    private val S = mapOf(
        "en" to mapOf(
            "step" to "{n} step", "steps" to "{n} steps",
            "obstacle" to "{label}, {dir}, {dist}.", "approaching" to "{label} approaching, {dir}, {dist}.",
            "critical" to "Stop. {label}, {dir}, {dist}.",
            "move_left" to "Move left.", "move_right" to "Move right.", "blocked" to "Blocked. Stop.",
            "camera_lost" to "Camera lost. Stop and use your cane.", "camera_back" to "Camera is back.",
            "slow" to "Processing is slow. Be extra careful.",
            "nothing" to "I don't detect anything right now. That does not mean the way is safe.",
            "starting" to "Blind assist starting. Put the phone in your pocket, camera facing forward. Ready in ten seconds. Keep using your cane.",
            "ready" to "Ready.", "stopped" to "Blind assist stopped.",
            "covered" to "Camera is covered. Adjust the phone.", "not_upright" to "Phone is not upright. Adjust it.",
            "no_model" to "Detection model missing. Cannot start.",
            "fall_alert" to "Possible fall detected. If you are fine, press the earphone button. An alarm will sound in 15 seconds.",
            "sos_expired" to "Fall alarm. I could not confirm this person is safe. Please help. No message has been sent.",
            "not_available" to "That feature is not available yet.",
            "lost_msg" to "I cannot tell where you are. Stop if it is safe, use your cane, and ask someone nearby for help.",
            "hot" to "Phone is very hot. Detection is slowed down. Be extra careful and use your cane.",
            "limited_mode" to "Warning. Limited mode. The object detector is not loaded. I cannot see obstacles. Use your cane.",
            "voice_missing" to "The voice for your language is not installed. Spoken alerts may be wrong. Use your cane.",
            "left" to "left", "right" to "right", "slight_left" to "slightly left", "slight_right" to "slightly right",
            "ahead" to "ahead", "far_left" to "far left", "far_right" to "far right"),
        "hi" to mapOf(
            "step" to "{n} कदम", "steps" to "{n} कदम",
            "obstacle" to "{label}, {dir}, {dist}।", "approaching" to "{label} पास आ रहा है, {dir}, {dist}।",
            "critical" to "रुकिए। {label}, {dir}, {dist}।",
            "move_left" to "बाएं मुड़िए।", "move_right" to "दाएं मुड़िए।", "blocked" to "रास्ता बंद है। रुकिए।",
            "camera_lost" to "कैमरा बंद हो गया। रुकिए और छड़ी का इस्तेमाल कीजिए।", "camera_back" to "कैमरा चालू हो गया।",
            "slow" to "प्रोसेसिंग धीमी है। सावधान रहिए।",
            "nothing" to "अभी मुझे कुछ नहीं दिख रहा। इसका मतलब यह नहीं कि रास्ता सुरक्षित है।",
            "starting" to "ब्लाइंड असिस्ट शुरू हो रहा है। फ़ोन जेब में रखिए, कैमरा सामने। दस सेकंड में तैयार। छड़ी का इस्तेमाल जारी रखिए।",
            "ready" to "तैयार।", "stopped" to "ब्लाइंड असिस्ट बंद।",
            "covered" to "कैमरा ढका हुआ है। फ़ोन ठीक कीजिए।", "not_upright" to "फ़ोन सीधा नहीं है। ठीक कीजिए।",
            "no_model" to "मॉडल नहीं मिला। शुरू नहीं हो सकता।",
            "fall_alert" to "गिरने की आशंका। अगर आप ठीक हैं तो ईयरफ़ोन का बटन दबाइए। 15 सेकंड में अलार्म बजेगा।",
            "sos_expired" to "गिरने का अलार्म। मैं पुष्टि नहीं कर सका कि यह व्यक्ति सुरक्षित है। कृपया मदद कीजिए। कोई संदेश नहीं भेजा गया।",
            "not_available" to "यह सुविधा अभी उपलब्ध नहीं है।",
            "lost_msg" to "मुझे आपकी जगह का पता नहीं है। सुरक्षित हो तो रुकिए, छड़ी का इस्तेमाल कीजिए और पास के किसी व्यक्ति से मदद माँगिए।",
            "hot" to "फ़ोन बहुत गरम है। पहचान धीमी हो गई है। सावधान रहिए और छड़ी का इस्तेमाल कीजिए।",
            "limited_mode" to "चेतावनी। सीमित मोड। वस्तु पहचान चालू नहीं है। मुझे रुकावटें नहीं दिख रहीं। छड़ी का इस्तेमाल कीजिए।",
            "left" to "बाएं", "right" to "दाएं", "slight_left" to "थोड़ा बाएं", "slight_right" to "थोड़ा दाएं",
            "ahead" to "सामने", "far_left" to "बहुत बाएं", "far_right" to "बहुत दाएं"),
        "te" to mapOf(
            "step" to "{n} అడుగు", "steps" to "{n} అడుగులు",
            "obstacle" to "{label}, {dir}, {dist}.", "approaching" to "{label} దగ్గరకు వస్తోంది, {dir}, {dist}.",
            "critical" to "ఆగండి. {label}, {dir}, {dist}.",
            "move_left" to "ఎడమవైపు వెళ్ళండి.", "move_right" to "కుడివైపు వెళ్ళండి.", "blocked" to "దారి మూసుకుపోయింది. ఆగండి.",
            "camera_lost" to "కెమెరా పనిచేయడం లేదు. ఆగి, కర్ర ఉపయోగించండి.", "camera_back" to "కెమెరా తిరిగి పనిచేస్తోంది.",
            "slow" to "ప్రాసెసింగ్ నెమ్మదిగా ఉంది. జాగ్రత్తగా ఉండండి.",
            "nothing" to "ఇప్పుడు నాకు ఏమీ కనిపించడం లేదు. దారి సురక్షితం అని దీని అర్థం కాదు.",
            "starting" to "బ్లైండ్ అసిస్ట్ మొదలవుతోంది. ఫోన్‌ను జేబులో పెట్టండి, కెమెరా ముందుకు. పది సెకన్లలో సిద్ధం. కర్రను వాడుతూనే ఉండండి.",
            "ready" to "సిద్ధం.", "stopped" to "బ్లైండ్ అసిస్ట్ ఆగింది.",
            "covered" to "కెమెరా కప్పబడింది. ఫోన్‌ను సరిచేయండి.", "not_upright" to "ఫోన్ నిటారుగా లేదు. సరిచేయండి.",
            "no_model" to "మోడల్ లేదు. మొదలుపెట్టలేను.",
            "fall_alert" to "పడిపోయిన అనుమానం. మీరు బాగుంటే ఇయర్‌ఫోన్ బటన్ నొక్కండి. 15 సెకన్లలో అలారం మోగుతుంది.",
            "sos_expired" to "పడిపోయిన అలారం. ఈ వ్యక్తి సురక్షితంగా ఉన్నారో నిర్ధారించలేకపోయాను. దయచేసి సహాయం చేయండి. ఎలాంటి సందేశం పంపలేదు.",
            "not_available" to "ఆ సదుపాయం ఇంకా అందుబాటులో లేదు.",
            "lost_msg" to "మీరు ఎక్కడ ఉన్నారో నాకు తెలియదు. సురక్షితమైతే ఆగండి, కర్రను వాడండి, దగ్గర ఉన్నవారిని సహాయం అడగండి.",
            "hot" to "ఫోన్ చాలా వేడిగా ఉంది. గుర్తింపు నెమ్మదించింది. జాగ్రత్తగా ఉండండి, కర్రను వాడండి.",
            "limited_mode" to "హెచ్చరిక. పరిమిత మోడ్. వస్తువులను గుర్తించే మోడల్ లోడ్ కాలేదు. నాకు అడ్డంకులు కనిపించడం లేదు. కర్రను వాడండి.",
            "left" to "ఎడమవైపు", "right" to "కుడివైపు", "slight_left" to "కొంచెం ఎడమవైపు", "slight_right" to "కొంచెం కుడివైపు",
            "ahead" to "ముందు", "far_left" to "చాలా ఎడమవైపు", "far_right" to "చాలా కుడివైపు"))

    private val LABELS = mapOf(
        "hi" to mapOf("person" to "व्यक्ति", "car" to "कार", "bicycle" to "साइकिल", "motorcycle" to "मोटरसाइकिल",
            "bus" to "बस", "truck" to "ट्रक", "chair" to "कुर्सी", "couch" to "सोफा", "bench" to "बेंच",
            "dining table" to "मेज़", "dog" to "कुत्ता", "cat" to "बिल्ली", "cow" to "गाय",
            "stairs" to "सीढ़ियाँ", "steps" to "सीढ़ी", "pothole" to "गड्ढा", "open drain" to "खुली नाली", "manhole" to "मैनहोल",
            "curb" to "फुटपाथ का किनारा", "speed bump" to "स्पीड ब्रेकर", "pole" to "खंभा", "glass door" to "कांच का दरवाज़ा", "auto rickshaw" to "ऑटो"),
        "te" to mapOf("person" to "మనిషి", "car" to "కారు", "bicycle" to "సైకిల్", "motorcycle" to "బైక్",
            "bus" to "బస్సు", "truck" to "లారీ", "chair" to "కుర్చీ", "couch" to "సోఫా", "bench" to "బెంచ్",
            "dining table" to "బల్ల", "dog" to "కుక్క", "cat" to "పిల్లి", "cow" to "ఆవు",
            "stairs" to "మెట్లు", "steps" to "మెట్టు", "pothole" to "గుంత", "open drain" to "తెరిచిన కాలువ", "manhole" to "మ్యాన్‌హోల్",
            "curb" to "కాలిబాట అంచు", "speed bump" to "స్పీడ్ బ్రేకర్", "pole" to "స్తంభం", "glass door" to "గాజు తలుపు", "auto rickshaw" to "ఆటో"))

    fun t(lang: String, key: String, vararg kv: Pair<String, Any>): String {
        var s = S[lang]?.get(key) ?: S["en"]!![key] ?: key
        for ((k, v) in kv) s = s.replace("{$k}", v.toString())
        return s
    }

    fun label(lang: String, name: String) = LABELS[lang]?.get(name) ?: name

    fun dist(lang: String, z: Double?): String {
        if (z == null) return ""
        val n = maxOf(1, (z / Cfg.STEP_M).roundToInt())
        return t(lang, if (n == 1) "step" else "steps", "n" to n)
    }

    fun direction(lang: String, b: Double): String {
        if (lang == "en") return "${clockHour(b)} o'clock"
        val a = abs(b)
        if (a < 8) return t(lang, "ahead")
        val side = if (b < 0) "left" else "right"
        return when { a < 20 -> t(lang, "slight_$side"); a < 38 -> t(lang, side); else -> t(lang, "far_$side") }
    }
}

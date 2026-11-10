package com.kaleem.blindassist

enum class Cmd { WHATS_AHEAD, READ_THIS, DESCRIBE, FIND_OBJECT, NAVIGATE, LOST, CALL_FAMILY, QUIET, DETAILED, STOP, UNKNOWN }

data class Parsed(val cmd: Cmd, val arg: String? = null)

/**
 * Text -> command parser (pure Kotlin). Input is the transcript AFTER the wake word; speech recognition
 * (Vosk/Whisper) and the wake word are NOT implemented yet (Stage 5 runtime = TODO).
 * Hindi/Telugu phrases are machine-drafted: NEEDS NATIVE REVIEW. Matching is substring-based on lowercase text.
 * STOP must be an exact short utterance to limit accidental shutdowns from mis-heard speech.
 */
object VoiceCommands {
    private val en = listOf(
        Cmd.WHATS_AHEAD to listOf("what's ahead", "whats ahead", "what is ahead", "what is around"),
        Cmd.READ_THIS to listOf("read this", "read it", "read the"),
        Cmd.DESCRIBE to listOf("describe the scene", "describe scene", "describe"),
        Cmd.LOST to listOf("i'm lost", "i am lost", "im lost"),
        Cmd.CALL_FAMILY to listOf("call my family", "call family"),
        Cmd.QUIET to listOf("quiet mode", "be quiet"),
        Cmd.DETAILED to listOf("detailed mode", "more detail"))
    private val hi = listOf(
        Cmd.WHATS_AHEAD to listOf("आगे क्या है", "सामने क्या है"),
        Cmd.READ_THIS to listOf("इसे पढ़ो", "यह पढ़ो"),
        Cmd.DESCRIBE to listOf("दृश्य बताओ", "वर्णन करो"),
        Cmd.LOST to listOf("मैं खो गया", "मैं खो गई"),
        Cmd.CALL_FAMILY to listOf("परिवार को फ़ोन", "परिवार को फोन"),
        Cmd.QUIET to listOf("शांत मोड"),
        Cmd.DETAILED to listOf("विस्तार मोड"))
    private val te = listOf(
        Cmd.WHATS_AHEAD to listOf("ముందు ఏముంది"),
        Cmd.READ_THIS to listOf("ఇది చదువు"),
        Cmd.DESCRIBE to listOf("దృశ్యం వివరించు"),
        Cmd.LOST to listOf("నేను తప్పిపోయాను"),
        Cmd.CALL_FAMILY to listOf("కుటుంబానికి ఫోన్"),
        Cmd.QUIET to listOf("నిశ్శబ్ద మోడ్"),
        Cmd.DETAILED to listOf("వివరమైన మోడ్"))

    private val stopWords = mapOf("en" to setOf("stop", "stop it"), "hi" to setOf("रुको", "बंद करो"), "te" to setOf("ఆపు", "ఆపండి"))
    private val navPrefix = mapOf("en" to listOf("take me to ", "navigate to "), "hi" to listOf("मुझे ले चलो "), "te" to listOf("నన్ను తీసుకెళ్ళు "))
    private val findPrefix = mapOf("en" to listOf("find my ", "find the ", "find "), "hi" to listOf("मेरी खोजो ", "खोजो "), "te" to listOf("నా వెతుకు ", "వెతుకు "))

    fun parse(lang: String, raw: String): Parsed {
        val t = raw.trim().lowercase().trimEnd('.', '?', '!', '।')
        if (t.isEmpty()) return Parsed(Cmd.UNKNOWN)
        if (t in (stopWords[lang] ?: emptySet())) return Parsed(Cmd.STOP)
        for (p in navPrefix[lang] ?: emptyList()) if (t.startsWith(p) && t.length > p.length) return Parsed(Cmd.NAVIGATE, t.removePrefix(p).trim())
        for (p in findPrefix[lang] ?: emptyList()) if (t.startsWith(p) && t.length > p.length) return Parsed(Cmd.FIND_OBJECT, t.removePrefix(p).trim())
        val table = when (lang) { "hi" -> hi; "te" -> te; else -> en }
        for ((c, phrases) in table) if (phrases.any { t.contains(it) }) return Parsed(c)
        return Parsed(Cmd.UNKNOWN)
    }
}

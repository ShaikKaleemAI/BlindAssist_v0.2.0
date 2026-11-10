package com.kaleem.blindassist

/**
 * Post-processing for OCR text. The OCR engine itself (ML Kit) is NOT integrated yet:
 * TODO(user): add `implementation("com.google.mlkit:text-recognition:16.0.0")` (bundled, offline, Latin only);
 * Devanagari/Telugu need separate script models. Misreads are likely: always treat results as candidates.
 */
object Currency {
    private val denominations = setOf(10, 20, 50, 100, 200, 500, 2000)
    private val rx = Regex("(?<![0-9])(\\d{2,4})(?![0-9])")

    /** Candidate INR denominations found in the text, in order, distinct. Not a substitute for a currency reader/tactile marks. */
    fun candidates(text: String): List<Int> =
        rx.findAll(text).map { it.groupValues[1].toInt() }.filter { it in denominations }.distinct().toList()

    fun speak(text: String): String {
        val c = candidates(text)
        return if (c.isEmpty()) "I could not read a note value. Do not rely on me for money."
        else "Possibly " + c.joinToString(" or ") { "$it rupees" } + ". Please verify by touch or ask someone."
    }
}

object BusNumber {
    private val rx = Regex("\\b(\\d{1,3}[A-Z]{0,2})\\b")

    fun candidates(text: String): List<String> =
        rx.findAll(text.uppercase()).map { it.groupValues[1] }.filter { it.any(Char::isDigit) }.distinct().toList()
}

object MedicineLabel {
    fun speak(text: String): String =
        if (text.isBlank()) "I could not read the label."
        else "Label text, may contain errors: ${text.trim().take(300)}. Do not use this for dosing. Ask a pharmacist or a person you trust."
}

/** What "read this" speaks. Short text with a note value -> currency candidates; otherwise hedged raw text. */
object ReadThis {
    fun speak(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return "I could not read any text."
        if (t.length < 40 && Currency.candidates(t).isNotEmpty()) return Currency.speak(t)
        return "Text, may contain errors: ${t.take(300)}"
    }
}

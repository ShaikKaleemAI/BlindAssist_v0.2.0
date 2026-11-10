package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class AssistTest {
    private class Fake : AssistActions {
        val calls = ArrayList<String>()
        override fun whatsAhead() { calls += "ahead" }
        override fun readThis() { calls += "read" }
        override fun describe() { calls += "describe" }
        override fun find(what: String) { calls += "find:$what" }
        override fun navigate(place: String) { calls += "nav:$place" }
        override fun lost() { calls += "lost" }
        override fun callFamily() { calls += "call" }
        override fun setVerbosity(v: Verbosity) { calls += "verb:$v" }
        override fun stop() { calls += "stop" }
        override fun unknown() { calls += "unknown" }
    }

    private fun run(lang: String, s: String): List<String> {
        val f = Fake(); CommandDispatcher.dispatch(VoiceCommands.parse(lang, s), f); return f.calls
    }

    @Test fun endToEndEnglish() {
        assertEquals(listOf("ahead"), run("en", "what's ahead"))
        assertEquals(listOf("nav:the bus stop"), run("en", "take me to the bus stop"))
        assertEquals(listOf("find:keys"), run("en", "find my keys"))
        assertEquals(listOf("verb:QUIET"), run("en", "quiet mode"))
        assertEquals(listOf("stop"), run("en", "stop"))
        assertEquals(listOf("unknown"), run("en", "banana"))
    }

    @Test fun oemHints() {
        assertNotNull(OemHints.hintFor("Samsung")); assertNotNull(OemHints.hintFor("Xiaomi"))
        assertNotNull(OemHints.hintFor("OPPO")); assertNotNull(OemHints.hintFor("vivo"))
        assertNull(OemHints.hintFor("Google")); assertNull(OemHints.hintFor(null))
    }

    @Test fun readThisBehaviour() {
        assertEquals("I could not read any text.", ReadThis.speak("  "))
        assertTrue(ReadThis.speak("500").contains("rupees"))
        assertTrue(ReadThis.speak("Platform 3 Express to Delhi").startsWith("Text, may contain errors"))
        assertTrue(ReadThis.speak("x".repeat(900)).length < 340)
    }
}

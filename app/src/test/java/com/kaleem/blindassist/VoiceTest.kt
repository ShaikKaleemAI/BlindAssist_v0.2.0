package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class VoiceTest {
    @Test fun stripsWakePhrase() {
        assertEquals("what's ahead", WakeWord.strip("en", "Hey assist, what's ahead"))
        assertEquals("stop", WakeWord.strip("en", "assist stop"))
        assertEquals("", WakeWord.strip("en", "assist"))
    }
    @Test fun requiresWakePhrase() {
        assertNull(WakeWord.strip("en", "what's ahead")); assertNull(WakeWord.strip("en", "stop"))
    }
    @Test fun wordBoundaryRespected() {
        assertNull(WakeWord.strip("en", "assistant please")); assertNull(WakeWord.strip("hi", "असिस्टेंट रुको"))
    }
    @Test fun hindiAndTelugu() {
        assertEquals("रुको", WakeWord.strip("hi", "असिस्ट रुको")); assertEquals("ఆపు", WakeWord.strip("te", "అసిస్ట్ ఆపు"))
    }
    @Test fun wakeThenParseEndToEnd() {
        val rest = WakeWord.strip("en", "hey assist find my keys")!!
        assertEquals(Parsed(Cmd.FIND_OBJECT, "keys"), VoiceCommands.parse("en", rest))
    }
    @Test fun nullVoiceIsUnavailable() { assertFalse(NullVoice().available) }
}

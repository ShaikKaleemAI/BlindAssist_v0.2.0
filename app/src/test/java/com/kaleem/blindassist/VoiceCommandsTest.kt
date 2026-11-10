package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class VoiceCommandsTest {
    private fun c(s: String, lang: String = "en") = VoiceCommands.parse(lang, s)

    @Test fun englishCommands() {
        assertEquals(Cmd.WHATS_AHEAD, c("What's ahead?").cmd)
        assertEquals(Cmd.READ_THIS, c("read this").cmd)
        assertEquals(Cmd.DESCRIBE, c("Describe the scene").cmd)
        assertEquals(Cmd.LOST, c("I'm lost").cmd)
        assertEquals(Cmd.CALL_FAMILY, c("call my family").cmd)
        assertEquals(Cmd.QUIET, c("quiet mode").cmd)
        assertEquals(Cmd.DETAILED, c("detailed mode").cmd)
    }

    @Test fun argumentsExtracted() {
        assertEquals(Parsed(Cmd.NAVIGATE, "the bus stop"), c("take me to the bus stop"))
        assertEquals(Parsed(Cmd.FIND_OBJECT, "keys"), c("find my keys"))
    }

    @Test fun stopIsExactOnly() {
        assertEquals(Cmd.STOP, c("stop").cmd)
        assertNotEquals(Cmd.STOP, c("please do not stop at the shop").cmd)
    }

    @Test fun unknownAndEmpty() {
        assertEquals(Cmd.UNKNOWN, c("banana").cmd); assertEquals(Cmd.UNKNOWN, c("  ").cmd)
        assertEquals(Cmd.UNKNOWN, c("take me to").cmd)
    }

    @Test fun hindiAndTelugu() {
        assertEquals(Cmd.STOP, c("रुको", "hi").cmd)
        assertEquals(Cmd.WHATS_AHEAD, c("आगे क्या है", "hi").cmd)
        assertEquals(Cmd.STOP, c("ఆపండి", "te").cmd)
        assertEquals(Cmd.LOST, c("నేను తప్పిపోయాను", "te").cmd)
    }
}

package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class AlertEngineTest {
    @Test fun closeObjectInPathIsCritical() {
        assertEquals(Level.CRITICAL, classify(trackAt("person", 1.0)))
    }

    @Test fun farObjectIsNone() {
        assertEquals(Level.NONE, classify(trackAt("person", 10.0)))
    }

    @Test fun criticalProducesSpeechAndVibration() {
        val tr = trackAt("person", 1.0)
        val ev = AlertEngine("en").update(listOf(tr), listOf(tr), 10.0)
        assertTrue(ev.any { it is Ev.Speech && it.pri == P_CRITICAL })
        assertTrue(ev.any { it === Ev.Vibrate })
    }

    @Test fun nothingNearProducesNoEvents() {
        val tr = trackAt("person", 10.0)
        assertTrue(AlertEngine("en").update(listOf(tr), listOf(tr), 10.0).isEmpty())
    }

    @Test fun criticalSpeechIsNotRepeatedEveryFrame() {
        val tr = trackAt("person", 1.0)
        val e = AlertEngine("en")
        e.update(listOf(tr), listOf(tr), 10.0)
        val again = e.update(listOf(tr), listOf(tr), 10.1)
        assertTrue(again.none { it is Ev.Speech && it.key.startsWith("crit") })
    }

    @Test fun vehicleWarnedEarlierThanPerson() {
        // vehicles use a 2x distance scale
        assertEquals(Level.CRITICAL, classify(trackAt("car", 2.3)))
        assertNotEquals(Level.CRITICAL, classify(trackAt("person", 2.3)))
    }

    /** SAFETY: the app must never tell the user the way is clear/safe. */
    @Test fun noSpokenStringClaimsClearOrSafe() {
        val banned = listOf("path is clear", "way is clear", "safe to walk", "all clear", "you are safe", "route is safe")
        for (lang in listOf("en", "hi", "te")) for (k in listOf("nothing", "starting", "ready", "limited_mode", "blocked",
            "move_left", "move_right")) {
            val s = I18n.t(lang, k).lowercase()
            for (b in banned) assertFalse("$lang/$k contains '$b'", s.contains(b))
        }
    }

    @Test fun nothingDetectedMessageDisclaimsSafety() {
        assertTrue(I18n.t("en", "nothing").contains("does not mean"))
    }
}

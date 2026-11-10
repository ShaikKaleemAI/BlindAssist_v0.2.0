package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class I18nTest {
    private val keys = listOf("step", "steps", "obstacle", "approaching", "critical", "move_left", "move_right", "blocked",
        "camera_lost", "camera_back", "slow", "nothing", "starting", "ready", "stopped", "covered", "not_upright",
        "no_model", "hot", "fall_alert", "sos_expired", "not_available", "lost_msg", "limited_mode", "left", "right", "slight_left", "slight_right", "ahead", "far_left", "far_right")

    @Test fun placeholdersAreSubstituted() {
        assertEquals("person, 12 o'clock, 2 steps.",
            I18n.t("en", "obstacle", "label" to "person", "dir" to "12 o'clock", "dist" to "2 steps"))
    }

    @Test fun stepDistanceRounding() {
        assertEquals("1 step", I18n.dist("en", 0.75)); assertEquals("2 steps", I18n.dist("en", 1.5))
        assertEquals("1 step", I18n.dist("en", 0.1))
    }

    @Test fun englishUsesClockDirection() {
        assertEquals("12 o'clock", I18n.direction("en", 0.0)); assertEquals("11 o'clock", I18n.direction("en", -30.0))
    }

    @Test fun allKeysPresentInAllLanguages() {
        // English words "left", "right" and "ahead" legitimately equal their own keys.
        val sameAsKeyInEnglish = setOf("left", "right", "ahead")
        for (lang in listOf("en", "hi", "te")) for (k in keys) {
            if (lang == "en" && k in sameAsKeyInEnglish) continue
            assertNotEquals("$lang missing '$k'", k, I18n.t(lang, k))
        }
    }

    @Test fun hindiAndTeluguDifferFromEnglish() {
        for (lang in listOf("hi", "te")) for (k in listOf("critical", "nothing", "blocked", "camera_lost", "limited_mode"))
            assertNotEquals("$lang/$k falls back to English", I18n.t("en", k), I18n.t(lang, k))
    }

    @Test fun nonEnglishFlaggedForNativeReview() { assertTrue(I18n.NEEDS_NATIVE_REVIEW) }
}

package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class OcrTest {
    @Test fun currencyCandidates() {
        assertEquals(listOf(500), Currency.candidates("₹500 Reserve Bank"))
        assertEquals(listOf(100, 2000), Currency.candidates("100 ... 2000 ... 100"))
        assertTrue(Currency.candidates("serial 12345678 and 75").isEmpty())
    }
    @Test fun currencySpeechHedges() {
        assertTrue(Currency.speak("500").contains("verify")); assertTrue(Currency.speak("xyz").contains("not rely"))
    }
    @Test fun busNumbers() {
        assertEquals(listOf("10H", "216"), BusNumber.candidates("route 10h to 216 Koti"))
        assertTrue(BusNumber.candidates("Koti Express").isEmpty())
    }
    @Test fun medicineLabelWarnsAndTruncates() {
        assertTrue(MedicineLabel.speak("Paracetamol 500mg").contains("Do not use this for dosing"))
        assertTrue(MedicineLabel.speak("x".repeat(1000)).length < 450)
        assertEquals("I could not read the label.", MedicineLabel.speak(" "))
    }
}

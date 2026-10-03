package com.joakim.rfidmanager.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxPaletteTest {

    @Test
    fun `contrastRatio svart mot vitt ar 21`() {
        assertEquals(21.0, OutboxPalette.contrastRatio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, OutboxPalette.contrastRatio(0xFF336699, 0xFF336699), 0.0001)
    }

    private fun assertAllReadable(name: String, p: OutboxPalette) {
        val labels = listOf("text", "secondaryText", "pauseText", "helpText", "pending", "sent", "failed", "errorText")
        p.textColors().forEachIndexed { i, c ->
            val r = OutboxPalette.contrastRatio(c, p.cardBackground)
            assertTrue("$name ${labels[i]} kontrast $r < ${OutboxPalette.MIN_CONTRAST}", r >= OutboxPalette.MIN_CONTRAST)
        }
    }

    @Test
    fun `morkt lage har kontrast minst 4_5 for alla utkorgsfarger`() = assertAllReadable("dark", OutboxPalette.Dark)

    @Test
    fun `ljust lage har kontrast minst 4_5 for alla utkorgsfarger`() = assertAllReadable("light", OutboxPalette.Light)

    @Test
    fun `morkt lage ger tydligt vit pausrad och hjalptext`() {
        // Minst #F2F4F7 i alla kanaler
        for (c in listOf(OutboxPalette.Dark.pauseText, OutboxPalette.Dark.helpText)) {
            assertTrue((c shr 16) and 0xFF >= 0xF2)
            assertTrue((c shr 8) and 0xFF >= 0xF4)
            assertTrue(c and 0xFF >= 0xF7)
        }
    }

    @Test
    fun `vantar syns mot kortet i bada lagen - gamla secondary gjorde det inte`() {
        // Regressionsvarden: colorScheme.secondary mot Material3:s standardkort
        assertTrue(OutboxPalette.contrastRatio(0xFF1A1D20, 0xFF36343A) < 1.5)
        assertTrue(OutboxPalette.contrastRatio(0xFFE5E7EB, 0xFFE5E0E8) < 1.1)
        assertTrue(OutboxPalette.contrastRatio(OutboxPalette.Dark.pending, OutboxPalette.Dark.cardBackground) >= 4.5)
        assertTrue(OutboxPalette.contrastRatio(OutboxPalette.Light.pending, OutboxPalette.Light.cardBackground) >= 4.5)
    }

    @Test
    fun `forDark valjer ratt palett`() {
        assertEquals(OutboxPalette.Dark, OutboxPalette.forDark(true))
        assertEquals(OutboxPalette.Light, OutboxPalette.forDark(false))
    }
}

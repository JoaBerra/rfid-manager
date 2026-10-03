package com.joakim.rfidmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.pow

/**
 * Explicita färger för utkorgens text i omgångskortet och hjälptexten (pausrad, status, felorsak).
 *
 * Varför egna färger: Material-kortet fick tidigare sin bakgrund från Material3:s standardvärde
 * (surfaceContainerHighest, ej definierat i appens färgscheman → #36343A mörkt / #E5E0E8 ljust),
 * medan texten valdes ur colorScheme.secondary (#1A1D20 / #E5E7EB) – i praktiken samma färg som
 * kortet, alltså osynlig ("Väntar" hade kontrast 1,0–1,4:1). Här anges både kortets bakgrund och
 * texten explicit, och enhetstestet [com.joakim.rfidmanager.ui.theme.OutboxPaletteTest] kräver
 * kontrast ≥ 4,5:1 för varje par i båda lägena.
 *
 * Färgerna är ARGB-Long (ren Kotlin) så att kontrasten kan testas utan Compose.
 */
data class OutboxPalette(
    val cardBackground: Long,
    val text: Long,
    val secondaryText: Long,
    val pauseText: Long,
    val helpText: Long,
    val pending: Long,
    val sent: Long,
    val failed: Long,
    val errorText: Long
) {
    /** Alla text-/bakgrundspar som måste ha kontrast ≥ [MIN_CONTRAST]. */
    fun textColors(): List<Long> = listOf(text, secondaryText, pauseText, helpText, pending, sent, failed, errorText)

    companion object {
        const val MIN_CONTRAST = 4.5

        /** Kortets bakgrund är oförändrad mot tidigare utseende, men nu explicit. */
        val Dark = OutboxPalette(
            cardBackground = 0xFF36343A,
            text = 0xFFFFFFFF,
            secondaryText = 0xFFD1D5DB,
            pauseText = 0xFFFFFFFF,
            helpText = 0xFFFFFFFF,
            pending = 0xFFFBBF24,
            sent = 0xFF00FF88,
            failed = 0xFFFF8A80,
            errorText = 0xFFFF8A80
        )

        val Light = OutboxPalette(
            cardBackground = 0xFFE5E0E8,
            text = 0xFF111827,
            secondaryText = 0xFF1F5130,
            pauseText = 0xFF111827,
            helpText = 0xFF111827,
            pending = 0xFF92400E,
            sent = 0xFF004225,
            failed = 0xFF9B1C1C,
            errorText = 0xFF9B1C1C
        )

        fun forDark(dark: Boolean): OutboxPalette = if (dark) Dark else Light

        /** WCAG-kontrastkvot (1–21) mellan två ogenomskinliga ARGB-färger. */
        fun contrastRatio(a: Long, b: Long): Double {
            val la = relativeLuminance(a)
            val lb = relativeLuminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        private fun relativeLuminance(argb: Long): Double {
            fun channel(shift: Int): Double {
                val v = ((argb shr shift) and 0xFF) / 255.0
                return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
    }
}

fun Long.toComposeColor(): Color = Color(this)

/** Appens eget ljus/mörk-läge (themeMode, inte bara systemläget): härleds ur det aktiva färgschemat. */
@Composable
fun rememberOutboxPalette(): OutboxPalette =
    OutboxPalette.forDark(MaterialTheme.colorScheme.background.luminance() < 0.5f)

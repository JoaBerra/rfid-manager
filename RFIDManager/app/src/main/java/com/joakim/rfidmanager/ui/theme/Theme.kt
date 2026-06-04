package com.joakim.rfidmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * # Design Layer — RFIDManagerTheme (Architecture: Theme System)
 *
 * Tvingar alltid mörkt läge (darkTheme=true default) + använder våra Figma-tokens (Color.kt).
 * Edge-to-edge via enableEdgeToEdge i MainActivity + WindowCompat (statusBarColor deprecated).
 *
 * **Koppling:** Används i MainActivity.setContent { RFIDManagerTheme { RFIDManagerScreen(...) } }.
 * MaterialTheme ger Primary etc via colorScheme, men vi använder ofta de råa val:na direkt för monospace/ tight design.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    secondary = Secondary,
    onSecondary = SecondaryForeground,
    tertiary = Accent,
    onTertiary = AccentForeground,
    background = Background,
    onBackground = Foreground,
    surface = Card,
    onSurface = CardForeground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    error = Destructive,
    onError = DestructiveForeground,
    outline = Border,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    secondary = Secondary,
    onSecondary = SecondaryForeground,
    tertiary = Accent,
    onTertiary = AccentForeground,
    background = Background,
    onBackground = Foreground,
    surface = Card,
    onSurface = CardForeground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    error = Destructive,
    onError = DestructiveForeground,
    outline = Border,
)

@Composable
fun RFIDManagerTheme(
    darkTheme: Boolean = true, // Force dark theme to match Figma design
    dynamicColor: Boolean = false, // Disable dynamic color to use our custom palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // statusBarColor is deprecated; edge-to-edge is handled via enableEdgeToEdge() + insets controller
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
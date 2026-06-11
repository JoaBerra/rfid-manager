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
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.joakim.rfidmanager.data.settings.ThemeMode

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
    secondary = Color(0xFFE5E7EB),
    onSecondary = Color(0xFF1F2937),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    error = Destructive,
    onError = DestructiveForeground,
    outline = Color(0x14000000),
)

@Composable
fun RFIDManagerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
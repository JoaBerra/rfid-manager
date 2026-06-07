package com.joakim.rfidmanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * # Design Layer — Color tokens (Figma-to-Compose + Architecture Theme System)
 *
 * Extraherade från Figma-export (React/Tailwind) → manuellt till Compose (se [[Figma-to-Compose]]).
 * Används i RFIDManagerTheme (Material3 darkColorScheme) + direkt i skärmar (Primary etc).
 *
 * **Centrala ID (Design):**
 * - Primary = #00FF88 (neon green) → knappar (START SCAN), UID highlights, radar, type badges, selected accent.
 * - Accent = #F59E0B (orange/amber) → status "SCANNING", lock warnings, dataPreview, write status messages.
 * - Monospace tungt (se Type.kt) + tight Radius=2.dp för "terminal/hacker" estetik.
 * - Mörk bakgrund #0A0C0E + Card #111416.
 *
 * Förankring i Architecture: Theme System (Color + Typography) är ett eget lager som UI screens använder.
 * I källkod: t.ex. Button containerColor=Primary, Text color=Accent i WriteTagForm status.
 *
 * Exakt matchning möjliggjorde att UI kändes "hyfsat användbart" efter scroll/selected/dedupe-fixarna juni 2026.
 */
val Background = Color(0xFF0A0C0E)
val Foreground = Color(0xFFE8EAED)
val Card = Color(0xFF111416)
val CardForeground = Color(0xFFE8EAED)
val Popover = Color(0xFF111416)
val PopoverForeground = Color(0xFFE8EAED)

val Primary = Color(0xFF00FF88)
val PrimaryForeground = Color(0xFF0A0C0E)

val Secondary = Color(0xFF1A1D20)
val SecondaryForeground = Color(0xFFE8EAED)

val Muted = Color(0xFF1A1D20)
val MutedForeground = Color(0xFF6B7280)

val Accent = Color(0xFFF59E0B)
val AccentForeground = Color(0xFF0A0C0E)

val Destructive = Color(0xFFEF4444)
val DestructiveForeground = Color(0xFFFFFFFF)

val Border = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
val Input = Color.Transparent
val InputBackground = Color(0xFF1A1D20)
val Ring = Color(0xFF00FF88)

val Sidebar = Color(0xFF0D0F11)
val SidebarForeground = Color(0xFFE8EAED)
val SidebarPrimary = Color(0xFF00FF88)
val SidebarPrimaryForeground = Color(0xFF0A0C0E)
val SidebarAccent = Color(0xFF1A1D20)
val SidebarAccentForeground = Color(0xFFE8EAED)
val SidebarBorder = Color(0x0FFFFFFF) // rgba(255,255,255,0.06)
val SidebarRing = Color(0xFF00FF88)

// Chart colors (kept from design)
val Chart1 = Color(0xFF00FF88)
val Chart2 = Color(0xFFF59E0B)
val Chart3 = Color(0xFF3B82F6)
val Chart4 = Color(0xFFA855F7)
val Chart5 = Color(0xFFEF4444)

// Fas 2 - Type badges (from Figma Design Specification)
val BadgeRFID = Color(0xFF10B981)
val BadgeEAN = Color(0xFF3B82F6)
val BadgeUID = Color(0xFF8B5CF6)

// Status colors (used in PersistedListItem etc.)
val StatusConnected = Color(0xFF10B981)

// Radius from design: 0.125rem = 2.dp
val Radius = 2.dp
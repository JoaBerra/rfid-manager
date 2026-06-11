# RFID Manager — Release 2026-06-Fas4

> **Senaste milstolpe (2026-06-11):** Fas 4 UAT godkänd av Kund.  
> Alla 8 punkter implementerade, testade på fysisk Samsung Galaxy Note 10 och signerade.  
> Se tag `fas-4-sign-off-2026-06-11`.

**Lokaliseringssystem (i18n), inställningar (fontstorlek, sök/filter, paginering), export (CSV/JSON), haptik + ljud, riktig MQTT-anslutning, dark mode-toggle.**

---

## Vad ingår i Fas 4

| # | Punkt | Status |
|---|-------|--------|
| 4.1 | Lokaliseringssystem (i18n) — JSON-lexikon svenska/engelska, runtime byte | ✅ |
| 4.2 | Font size-slider — 1.0–1.8×, påverkar alla vyer | ✅ |
| 4.3 | Sök/filtrering i Readings — wildcards (`*`, `?`), kombinerat med type-filter | ✅ |
| 4.4 | Export CSV/JSON — Android Share Sheet | ✅ |
| 4.5 | Haptic + ljud vid scan — toggles i Settings | ✅ |
| 4.6 | Riktig MQTT-anslutning — MqttConnectionManager, persistent, auto-reconnect | ✅ |
| 4.7 | Dark mode-toggle — Switch, LIGHT/DARK, alla skärmar migrerade till `MaterialTheme.colorScheme.*` | ✅ |
| 4.8 | Paginering i Readings — "Ladda fler"-knapp, konfigurerbar sidstorlek (10–50) | ✅ |

---

## Snabbstart

```bash
# 1. Öppna i Android Studio eller bygg från terminal
cd RFIDManager
./gradlew assembleDebug

# 2. Installera på ansluten enhet
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Starta "RFID Manager" på telefonen, slå på NFC
```

## Innehåll i paketet

- `RFIDManager/` — fullständig källkod (Kotlin 2.2.10, Compose, Material 3)
  - `data/mqtt/MqttConnectionManager.kt` — persistent MQTT-anslutning (NY)
  - `data/settings/AppSettings.kt` — 6 StateFlows (fontSizeScale, hapticEnabled, soundEnabled, themeMode, pageSize)
  - `data/localization/LocalizationManager.kt` — i18n (NY)
  - `data/export/ReadingExporter.kt` — CSV/JSON-export (NY)
  - `nfc/AndroidNfcManager.kt` — haptik + ljud
  - `ui/theme/Theme.kt` — LIGHT/DARK-stöd
  - `ui/viewmodel/ReadingsViewModel.kt` — sök, filter, paginering
  - `ui/viewmodel/ConnectivityViewModel.kt` — ingen demo-data, läser från MqttConnectionManager
  - `ui/screens/SettingsScreen.kt` — alla inställningar (språk, font, haptik, ljud, dark mode, sidstorlek, export)
  - `assets/strings_sv.json`, `strings_en.json` — 75+ nycklar var

- `llm-wiki/` — hela projektdokumentationen (Karpathy-style)
  - Uppdaterad med Fas 4 sign-offs, arkitektur, roadmap

- `rfid-setup/` — udev-regler + ADB-skript

---

**Skapad:** 2026-06-11  
**GitHub:** https://github.com/JoaBerra/rfid-manager

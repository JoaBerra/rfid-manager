---
title: Fas 4 Implementation Plan
tags: [fas4, implementation, plan, localization, settings, export, mqtt]
created: 2026-06-10
---

# Fas 4 — Lokalisering, inställningar och användbarhet

**Status:** ✅ Samtliga 8 punkter implementerade, godkända och signerade av Kund 2026-06-11.  
**Lead:** AI-assistenten  
**Kund:** Joa (Projektledare + UAT-testare)

## Övergripande mål

Göra appen redo för kundanpassning: språkbyte, textstorlek, sök, export, dark mode och riktig MQTT-anslutning.

## Prioriterad ordning (Lead-förslag)

1. **Font size-slider** (enkel, snabb vinst)
2. **Sök/filtrering** (små ändringar i läslogik)
3. **Localization-system** (JSON-lexikon + runtime byte)
4. **Export** (CSV/JSON)
5. **Haptic + ljud** (lätt, men kräver permission)
6. **Dark mode** (tema-ändring, liten insats)
7. **MQTT-anslutning** (störst, kräver verklig broker-konfig)
8. **Paginering** (prestanda, sist)

## Lexikon — Lokalisering (i18n)

| Identifierare | Svenska | English (default) |
|---|---|---|
| `screen.scan.title` | SCAN | SCAN |
| `screen.scan.start` | START SKANNING | START SCAN |
| `screen.scan.stop` | STOPPA SKANNING | STOP SCAN |
| `screen.scan.scanning_active` | SKANNING AKTIV – håll taggen mot telefonen | SCANNING ACTIVE – hold tag to the back of the phone |
| `screen.scan.radar_text` | RADAR • LIVE (demo) | RADAR • LIVE (demo) |
| `screen.scan.no_tags` | Inga taggar hittade | No tags detected |
| `screen.scan.listening` | Lyssnar efter taggar… | Listening for tags… |
| `screen.scan.hold_tag` | Håll taggen mot telefonen | Hold tag to the back of the phone |
| `screen.scan.start_scan_hold` | Starta skanning och håll en tagg | Start scan and hold a tag |
| `screen.scan.scanned_tags` | SKANNADE TAGGAR | SCANNED TAGS |
| `screen.scan.persist` | Spara | Persist |
| `screen.scan.persisted` | ✓ Sparad | ✓ Persisted |
| `screen.scan.type` | Typ | Type |
| `screen.readings.title` | SPARADE AVLÄSNINGAR | PERSISTED READINGS |
| `screen.readings.filter_all` | Alla | All |
| `screen.readings.empty_title` | Inga avläsningar än | No readings yet |
| `screen.readings.empty_instructions` | Skanna en RFID-tagg och tryck "Spara" | Scan an RFID tag and tap "Persist" |
| `screen.readings.transmit` | Skicka | Transmit |
| `screen.readings.transmitted` | Skickad | Transmitted |
| `screen.readings.pending` | Väntar | Pending |
| `screen.connectivity.title` | MQTT / Sparkplug | MQTT / Sparkplug |
| `screen.connectivity.status_connected` | SPARKPLUG ANSLUTEN ✓ | SPARKPLUG CONNECTED ✓ |
| `screen.connectivity.status_disconnected` | FRÅNKOPPLAD | DISCONNECTED |
| `screen.connectivity.heartbeat` | Senaste hjärtslag | Last heartbeat |
| `screen.connectivity.readings` | AVLÄSNINGAR | READINGS |
| `screen.connectivity.no_readings` | Inga avläsningar än | No readings yet |
| `screen.settings.title` | INSTÄLLNINGAR | SETTINGS |
| `screen.settings.storage_mode` | Lagringsläge | Storage Mode |
| `screen.settings.storage_json` | JSON-fil – avläsningar överlever omstart | JSON file – readings survive restart |
| `screen.settings.language` | Språk | Language |
| `screen.settings.font_size` | Textstorlek – data | Font size – data |
| `screen.settings.dark_mode` | Mörkt läge | Dark Mode |
| `screen.settings.page_size` | Sidstorlek – läslista | Page size – reading list |
| `screen.readings.load_more` | Ladda fler | Load more |
| `screen.settings.export` | Exportera avläsningar | Export readings |
| `screen.settings.export_csv` | Exportera som CSV | Export as CSV |
| `screen.settings.export_json` | Exportera som JSON | Export as JSON |
| `screen.settings.app_info` | App-info | App Info |
| `screen.settings.version` | Version | Version |
| `screen.settings.build` | Byggnummer | Build Number |
| `screen.settings.haptic` | Vibrering vid skanning | Vibration on scan |
| `screen.settings.sound` | Ljud vid skanning | Sound on scan |
| `common.loading` | Läser in… | Loading… |
| `common.error` | Ett fel uppstod | An error occurred |
| `common.ok` | OK | OK |
| `common.cancel` | Avbryt | Cancel |

Ovanstående tabell fylls på efter hand. Varje ny sträng läggs till i lexikonet innan den används i UI.

## Risker (post-factum — alla hanterade)

- Localization krävde migrering av alla hårdkodade strängar → **genomfört**, alla skärmar migrerade till `str(key)`-mönster.
- MQTT-anslutning startas nu via MqttConnectionManager vid app-start → **ingen nätverksändring krävdes** (fortsatt okrypterat för dev).
- Haptic + ljud: inga runtime permissions krävdes på Android 12 (Samsung Note 10). Fungerar med SoundPool + VibratorService.

## Avvikelser från plan

| Planerat | Levererat | Anledning |
|---|---|---|
| 3-vägs dark mode (Ljust/Mörkt/System) | Switch (På = mörkt, Av = ljust) | FilterChips orsakade osynlig text i ljust läge; Switch är enklare och tydligare |
| Dark mode FilterChips | Switch | Efter UAT: Switch är mer intuitiv |
| Hårdkodad sidstorlek (50) | Slider i Settings (10–50, steg om 10) | Efter UAT-feedback: för få taggar för 50 |

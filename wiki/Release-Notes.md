---
title: Release Notes — RFID Manager
tags: [release, version, changelog, fas]
created: 2026-06-13
---

# Release Notes — RFID Manager

> **Dokument:** 5.3  
> **Senast uppdaterad:** 2026-10-03

---

## Ej släppt — MQTT-inloggning och gren `feature/sqlite` (Room/SQLite)

**Status (2026-10-03):** Inget av detta är taggat eller släppt som APK. Senaste release är fortfarande v1.0.1.

### MQTT-inloggning (på `main`, commit `f08ff22`)

- Brokern på ishtar kräver inloggning sedan 2026-10-03 (`allow_anonymous false`, `password_file`, `acl_file`). `rfid-app` får skriva och `rfid-dashboard` får läsa `rfidmanager/+/telemetry` (ACL). Se README, avsnittet *Nätverk och säkerhet (MQTT)*.
- `passwd` och `acl` ägs av uid 1883 med mode `0600`; lösenord skapas med `mosquitto_passwd -b` via `read -rs`.
- Appen: Inställningar → MQTT-anslutning med *Användarnamn* och *Lösenord* (lagras krypterat). Anslut-knappen ger bekräftelse (*Ansluten* / *Misslyckades* med felorsak) redan vid första tryck (`12012a2`).
- Verifierat end-to-end 2026-10-03: telefonen som `rfid-app`, dashboarden som `rfid-dashboard`, en NFC-avläsning nådde dashboarden. **Inte visuellt verifierat:** felmeddelandet vid fel lösenord.

### Gren `feature/sqlite` (Room/SQLite; commits `eab6b6a..48e8a89`, ej mergad till `main`)

- **Room/SQLite är enda lagringen** (Room 2.8.5 + KSP 2.3.12 med AGP 9.2.1 / Kotlin 2.2.10) — ersätter JSON-fallback och minnesläge. "KSP blockerad" längre ned gäller bara v1.0/v1.0.1 *(historiskt/löst)*.
- **Engångsmigrering** av `readings.json` till Room i en transaktion; filen döps om till `readings.json.migrated` (raderas aldrig). Vid fel behålls JSON-filen, felet loggas (`JsonToRoomMigration`) och visas i Inställningar → Lagring, nytt försök vid nästa start.
- **Verifierad på telefonen 2026-10-03:** 3 poster migrerade, databasen kontrollerad.
- Enhetlig status `transmitted` vid överföring (tidigare `transmitted via Sparkplug` i JSON-läget).
- **Bygginfo (`48e8a89`):** Inställningar → App-info visar version, byggtid och git-commit från `BuildConfig` (`BUILD_TIME`, `GIT_COMMIT`) i stället för hårdkodat *Fas 5 (juni 2026)*.

### Gren `feature/outbox` (utkorg; ej mergad till `main`, bygger på `feature/sqlite`)

- **Utkorg (outbox):** avläsningar sparas först som `PENDING` (*Väntar*) och skickas av WorkManager; `SENT` (*Skickad*) först när brokern bekräftat (QoS 1). Meddelandet har `id`, `deviceId`, `messageId`. Room-schema 2 (migrering 1→2). Se [[Outbox]].
- **Verifierad av Joakim 2026-10-03 på telefon och mot riktiga brokern:** avläsningar blir *Väntar* offline, skickas när nätverk finns (även med appen stängd), kommer fram en gång var på dashboarden, status *Skickad*. Room-migrering 1→2 körd på telefonens riktiga databas: 4 poster blev `SENT`.
- **Dashboard:** dubblettskydd driftsatt på ishtar 2026-10-03 (`c16ef43`; backup `~/backup-dashboard-20261003-115215.tar` på ishtar).
- **Inte provat på enhet:** felvägarna fel lösenord, 12 misslyckade försök och status `FAILED` (endast JVM-testade).

### Kända begränsningar (kvarstående teknisk skuld)

- `markAsTransmitted` anropas oavsett om MQTT-publiceringen lyckades på `feature/sqlite`/`main`. **Rättat på `feature/outbox`** (2026-10-03, ej mergad; **verifierad av Joakim på telefon och mot riktiga brokern 2026-10-03**) — se [[Outbox]].
- Testskript (`test/fas2-mqtt/mqtt/`) och MQTT Explorer ansluter anonymt och behöver användare nu när brokern kräver inloggning.
- `assembleRelease` misslyckas på fakir: `~/.android/debug.keystore` saknas. `assembleDebug` påverkas inte.
- `readings.json.migrated` raderas aldrig automatiskt.

---

## v1.0.1 — Fas D testmiljö (ishtar default broker)

**Tag:** `v1.0.1`  
**Datum:** 2026-07-14  
**Default branch:** `main` (GitHub)  
**APK:** `app-debug.apk` (debug-signerad)

### Nytt i denna version

- **Default MQTT-broker** ändrad till **ishtar** `192.168.50.151:1883` (Fas D / Uppdrag 004) — ersätter legacy falstaff `.107` och sixten `.128`.
- **Appkälla synkad** från sixten (`AndroidStudioProjects/RFIDManager`) — full Fas 6-app med MQTT Settings UI och egen launcher-ikon.
- **fakir dev-baseline** verifierad: `assembleDebug`, `installDebug`, UAT mot ishtar-testlabb (Uppdrag 004 godkänd 2026-07-13).

### Installera

1. Ladda ner `app-debug.apk` från GitHub Release.
2. Installera på Android-enhet (USB eller filöverföring).
3. Broker ska ansluta mot ishtar utan manuell Settings-override om testlabbet kör på `.151`.

### Kända begränsningar

- Oförändrat från v1.0 — se avsnittet nedan (Room/KSP *(löst på `feature/sqlite`)*, MQTT utan TLS, m.m.).
- Debug-signerad APK — inte för produktionsdistribution.

---

## v1.0 — Dokumentation, kvalitet och 1.0-release

**Tag:** `v1.0`  
**Datum:** 2026-06-13  
**Default branch:** `master` (GitHub)

### Nytt i denna version

#### Fas 5 — Dokumentation, kvalitet och radar-estetik

- **Användarmanual** (5.1) — Fullständig PDF-manual på svenska med skärmbilder, write-dialog, hex-krav, minneskarta och låsstatus.
- **Arkitektur-diagram** (5.2) — Uppdaterat App-Architecture med Fas 3+4+5 strukturer.
- **Release notes** (5.3) — Detta dokument.
- **Testplan** (5.4) — 22 manuella testfall i 6 kategorier (Scan, Readings, Connectivity, Settings, Write, E2E). Alla Godkänt.
- **Kodgenomgång** (5.5) — Demo-etiketter borttagna, duplicerat build-block konsoliderat, fas-referenser i kommentarer rensade, hårdkodade färger migrerade till theme, deprecation fixad (`getParcelableExtra`).
- **Dynamisk layout** (5.6) — Kort skalas dynamiskt med fontstorlek, vertikal stapling vid stor text, overflow-skydd.
- **Radar sweep trail** (5.7) — Svepande linje med 72° efterglöd som bleknar gradvis, 72 segment, tag-svansar medsols.

#### Fas 6 — 1.0 Release

- **MQTT-broker-konfiguration i UI** (6.1) — Host/port-fält i Settings, sparas i SharedPreferences, återanslutningsknapp med Snackbar-status.
- **App-ikon** (6.2) — Anpassad launcher-ikon (512×512), adaptive icon för API 26+, mörkblå bakgrund.
- **Release build-setup** (6.3) — ProGuard-minifiering (R8), signering med debug-nyckel, APK-nedladdning för GitHub Release.

### Buggar fixade

| ID | Beskrivning | Testfall |
|---|---|---|
| BUG-001 | Connectivity: detaljvy visas inte vid tryck på meddelande | — |
| BUG-002 | Write-funktion borta efter Fas 3-refaktor | TC-SCAN-006, TC-E2E-003 |
| BUG-003 | Background NDEF — app fångar inte tagg utan aktiv scanning | TC-SCAN-004 |
| BUG-004 | Light mode — svaga/oläsbara texter (primary, onSurfaceVariant) | TC-SETT-004, TC-NAV-004 |
| BUG-005 | Rensa readings-kommando fungerar inte | TC-READ-002, TC-SETT-010 |
| BUG-006 | Testbeskrivning stämde ej med verkligt beteende | TC-SCAN-001 |
| BUG-007 | Låst tagg-gränssnitt — write UI ignorerar låsta block | TC-SCAN-007 |
| BUG-008 | MQTT-miljö-dokumentation + Connectivity textfärg i mörkt läge | TC-READ-006, Connectivity |

### Kända begränsningar

- **Room-databas (historiskt: gällde v1.0/v1.0.1; löst på grenen `feature/sqlite` 2026-10-03, se ovan):** KSP är inte kompatibelt med AGP 9.2.1 + Kotlin 2.2.10. In-memory persistens + JSON-fallback aktiv — data överlever app-omstart. Riktig Room kräver KSP-version som stödjer AGP 9 built-in Kotlin.
- **UHF RFID:** Stöds inte via inbyggt NFC. Kräver extern Bluetooth/USB-läsare.
- **Kryptering:** MQTT utan TLS (dev-läge). Produktion bör använda TLS. *(Tillägg 2026-10-03: inloggning är nu aktiv på brokern, men trafiken är fortfarande klartext.)*
- **Barcode/EAN:** Inte implementerat — planerat i senare version.
- **Write block:** Skrivning till låsta block blockeras i UI; vissa taggar kan ha ytterligare begränsningar på hårdvarunivå.

---

## v0.4 (Fas 4) — Lokalisering, inställningar och användbarhet

**Tag:** `fas-4-uat-godkand-2026-06-11`  
**Datum:** 2026-06-11

### Nytt

- **Lokaliseringssystem (i18n)** med svenska och engelska, språkväljare i Settings, omedelbar omkoppling via StateFlow.
- **Font size-slider** i Settings (1.0× – 1.8×), sparas i SharedPreferences, påverkar alla listor.
- **Sök/filtrering** i Readings (UID, kod, wildcards), kombinerbart med type-filter.
- **Export CSV/JSON** via Android Share Sheet.
- **Haptic + ljud** vid scan, av/på i Settings.
- **Riktig MQTT-anslutning** (ersatt demo-data), automatisk återanslutning.
- **Dark mode-toggle** i Settings, omedelbar applicering.
- **Paginering** i Readings med "Load more"-knapp, konfigurerbar batch size (10–50).

### Buggar fixade

- System chooser vid NFC-detektion — `FLAG_READER_NO_PLATFORM_SOUNDS` + PendingIntent med `FLAG_IMMUTABLE`.
- EPERM på rå TCP-socket (debug build) — manifest + network_security_config + Samsung-inställningar.

---

## v0.3 (Fas 3) — Navigation, ViewModels, spacing och polish

**Tag:** `fas-3-uat-godkand-2026-06-10`  
**Datum:** 2026-06-10

### Nytt

- Bottom navigation med 4 items (Scan | Readings | Connectivity | Settings).
- Dedikerade fullskärmsvyer istället för trånga tabs.
- ViewModel-refaktor för alla skärmar (ScanViewModel, ReadingsViewModel, MqttStatusViewModel, SettingsViewModel).
- Material 3 spacing: minst 16 dp padding, 12 dp mellan listrader, RadarView max 35–40 %.
- Touch targets ≥48 dp, generösa empty states.
- Expandable/collapsible minneskarta i Scan (memory map).
- Lock page (LB0/LB1) parsning — röd/gul indikator för låsta block.

---

## v0.2 (Fas 2) — Persistens och MQTT

**Tag:** `fas-2-uat-godkand-2026-06-07`  
**Datum:** 2026-06-07

### Nytt

- Lokal persistens av läsningar (in-memory + Room-entiteter/DAO/Repository).
- MQTT-kommunikation (Sparkplug-liknande JSON på `rfidmanager/<uid>/telemetry`).
- UI för persisterade läsningar + Transmit ↑ + MQTT-status.
- End-to-end validering på fysisk Samsung Galaxy Note 10.

---

## v0.1 (Fas 1) — NFC-läsning/skrivning och grundläggande UI

**Datum:** 2026-06-04

### Nytt

- Projektstruktur (Kotlin + Jetpack Compose, Material 3).
- Mörkt tema (neon green primary `#00FF88`, orange accent, Inter + JetBrains Mono).
- Dashboard med RadarView (Canvas + animation), StatCards, READ/WRITE tabs.
- Riktig NFC-läsning via AndroidNfcManager (Mifare Classic, auto-dump sektor 0–15).
- Write-formulär för sektor/block med hex-input.
- Figma → Compose designsystem.
- Fysisk deployment via ADB till Samsung Galaxy Note 10.

---

## Installation

```bash
# Bygg och installera debug-APK
cd RFIDManager   # i monorepot (äldre sökväg ~/projects/rfid/rfid-manager-android är historisk)
ANDROID_HOME=~/Android/Sdk ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Bygg release-APK (kräver ~/.android/debug.keystore — saknas på fakir 2026-10-03)
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Källa: `com.joakim.rfidmanager`  
Minsta SDK: 24 (Android 7.0)  
Målsatt SDK: 36 (Android 16)

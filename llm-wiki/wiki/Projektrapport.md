---
title: Projektrapport — RFID Manager (Projekt RF-ID Applikationer på Android)
tags: [rapport, projekt, android, rfid, nfc, kotlin, eskortminne]
created: 2026-06-04
---

# Projektrapport: RFID Manager — Projekt RF-ID Applikationer på Android

**Sammanfattning**  
Vi har byggt en fungerande, kostnadsfri Android-app i Kotlin som kan **läsa och skriva till fysiska eskortminnen** (RFID/NFC 13.56 MHz, främst MIFARE Ultralight/NTAG) med en Samsung Galaxy Note 10 (SM-N970F/DS). Appen har ett "hyfsat användbart" gränssnitt med radar, realtidslista, detaljerad minnesvy, låsbits-visualisering och ett pålitligt "armed write"-flöde som verifierats genom att skriva "test" (74 65 73 74) till page 12 på en riktig tagg.

Allt har dokumenterats i en LLM Wiki enligt Karpathy-mönster (append-only log, syntes-sidor, ID-begrepp som binder lager). Som avslut har vi lagt till **rika kommentarer** i koden som explicit förankrar de tre lagren **Architecture – Design – Källkod** med konsekventa ID-begrepp (t.ex. `selectedId`/`selectedTagId`, `sectorsRead`/`fullSectors`, `pendingWrite`/`armed write`, `handleTag`/`lastTag`, page 2 LB0, `onStartScan`/`onTagSelected`, `enableReaderMode` + `NO_PLATFORM_SOUNDS` etc.).

---

## 1. Tidsperiod

- **Start**: 2026-05-26 — Initial wiki-struktur (index.md, log.md, Projektöversikt, Projektplan, Utvecklingsmiljö, RFID-och-NFC, Eskortminne, Android-NFC-API) + första agent-drivna förberedelser för Android Studio från AUR.
- **Miljö & verktyg**: 2026-05-26 → 2026-05-31 — AUR-installation av android-studio, Hyprland/Omarchy-fixer (windowrule, jetbrains.conf, consent bypass, _JAVA_AWT_WM_NONREPARENTING), udev/ADB-prep, kvm-grupp.
- **Design & UI-bas**: 2026-06-01 — Figma-export (React) → token-extraktion → RFIDManagerTheme (Primary #00FF88, Accent #F59E0B, Radius 2dp, JetBrains Mono), full dashboard (radar Canvas, StatCards, TabRow READ/WRITE, RFIDTagList, WriteTagForm), Typography ctor-fix, rename/cleanup till com.joakim.rfidmanager.
- **Hårdvara & NFC**: 2026-06-01 → 2026-06-02 — Galaxy Note 10 (SM-N970F/DS, Android 12/One UI 4.1, S.LSI NFC) identifierad, USB-menyexakt ("USB kontrolleras av: Den här enheten", "Använd USB till: Överföra filer/Android Auto"), fix-usb-adb.sh + 51-android.rules (04e8/18d1, plugdev), adb install, NFC skeleton (NfcManager interface, Stub → AndroidNfcManager), enableReaderMode, chooser-fallback via onNewIntent + manifest filters.
- **Funktionell NFC (läsa/skriva eskortminne)**: 2026-06-02 → 2026-06-03 — Ultralight/NTAG-stöd (auto page reads 2/4/8/12/16, page 2 lock/OTP), MIFARE Classic skeleton, armed pending write-mönster (för att klara TagLost/Transceive), UI-exponering av fullSectors + lock parser, första writes (misslyckade pga locks på page 4), **lyckad write till page 12 "74 65 73 74"** (verifierad i re-detection + hex-patch i UI), scroll + dedupe + font-förbättringar av SELECTED TAG MEMORY.
- **Avslut & dokumentation**: 2026-06-03 → 2026-06-04 — UI "hyfsat användbart" (användarens ord), **rika kommentarer** i all källkod (förankring Architecture-Design-Källkod + konsekventa ID-begrepp + Kotlin-novis-förklaringar), denna projektrapport, paketering + GitHub-publicering.

Total kalendertid: ~9 dagar (intensivt, iterativt, med real hårdvara från 2026-06-01).

---

## 2. Vad vi gjort (huvudresultat)

### 2.1 Utvecklingsmiljö (helt gratis)
- Arch Linux + Omarchy (Hyprland) som desktop.
- Android Studio installerat via AUR (yay).
- ADB + USB-debugging mot fysisk Samsung Note 10.
- Dedikerad `~/projects/rfid/rfid-manager/setup/` med udev-regler och fix-script (plugdev, 04e8/18d1).
- Sideloading av debug-APK via `adb install -r`.
- All dokumentation i `llm-wiki/` (raw/ + wiki/ följer Karpathy append-only + schema).

### 2.2 Arkitektur (3 lager, konsekventa ID-begrepp)
Se [[App-Architecture]] för Mermaid och detaljer. Koden är nu kommenterad så att en novis kan följa:

- **UI Layer** (`ui/screens`, `ui/theme`, `ui/model`):  
  `RFIDManagerScreen` (split layout, radar, stats, SELECTED + detaljerad minnesvy, TabRow), `RFIDTagList` (LazyColumn med signalbars, type badges, urval), `WriteTagForm` (armed write UI).  
  **ID**: `selectedId: String?`, `onStartScan`, `onTagSelected(RFIDTag)`, `onWrite(text, addr)`, `tags: List<RFIDTag>`, `isScanning`, `writeStatusMessage`.  
  State hoistas till `MainActivity` (detectedTags, selectedTagId, scanningEnabled, pendingWrite).

- **Domain Layer** (`domain/model`):  
  `RfidTag` (uid: ByteArray, type: TagType, sectorsRead: Map<Int, ByteArray>), `TagType` enum.  
  **ID**: `sectorsRead` (nyckel = page för Ultra, sector för Classic), `uidHex`.

- **NFC Layer** (`nfc`):  
  `NfcManager` (interface), `AndroidNfcManager` (riktig), `StubNfcManager` (dev).  
  **ID**: `startScanning(onTagDetected)`, `handleTag`, `lastTag`, `onTagDiscovered`, `readMifareUltralightPages`/`writeMifareUltralightPage`, `enableReaderMode` + `FLAG_READER_NO_PLATFORM_SOUNDS`.

**Koppling mellan lager**: MainActivity (glue) + mappning `DomainRfidTag.toUiTag()` → `ui.model.RFIDTag` (fullSectors som hex-strängar).

### 2.3 Design → Kod (Figma-to-Compose)
Se [[Figma-to-Compose]].  
Tokens extraherade manuellt från Figma-export (React) → `Color.kt` / `Type.kt` / `RFIDManagerTheme`:
- Primary: `#00FF88` (neon) — START SCAN, radar, highlights, selected.
- Accent: `#F59E0B` — status, lock warnings, write feedback.
- Bakgrund/Card: mörk terminal (#0A0C0E / #111416).
- Radius: 2.dp.
- Typsnitt: Inter (rubriker) + JetBrains Mono (allt UID/hex/data — "terminal aesthetic").
- Komponenter: Canvas radar (puls + sweep), StatCard, TabButton, scrollable vänsterpanel, lock-bit parser i SELECTED + WRITE default.

UI-polish per användarfeedback (scroll för P12, dedupe av SELECTED, större fonter i detaljvy, instruktioner).

### 2.4 NFC & Eskortminne (det som faktiskt fungerar)
- Reader mode på Samsung Note 10 (Exynos + S.LSI NFC som stödjer MIFARE).
- Automatisk dump av Ultralight pages (2=Lock/OTP/CC, 4/8/12/16+).
- LB0-parser (lock bits) → UI varnar "pages X locked" + föreslår safe target page.
- **Armed write-mönster**: UI armar → nästa fresh detection exekverar (lösning på stale Tag-problem). Verifierat: page 12 → "74 65 73 74" (ASCII "test") på tagg med NDEF lanabgroup.se. Re-detection + hex-patch visar ändringen direkt i SELECTED.
- Fallback för system chooser (NDEF/TECH intent-filters + onNewIntent + processDiscoveredTag).
- Loggning + vibrate på varje detection.
- Stöd för både Ultralight (user tags) och Classic (skeleton + default keys).

### 2.5 Wiki & Process (Karpathy pattern)
- Append-only `log.md` med dagliga entries (setup, fixes, nfc-milstolpar, user quotes, kodändringar).
- Syntes-sidor: App-Architecture.md, Figma-to-Compose.md, Hardware-Testenheter.md (exakta svenska USB-menytexter, udev, Note 10 spec).
- ID-begrepp används konsekvent i wiki + kod + loggar → gör det lätt att spåra t.ex. "selectedId" från Figma → MainActivity → lista → SELECTED-vy → armed write.

### 2.6 Rika kommentarer (detta avslutande steg)
Alla källfiler har nu:
- KDoc med `# Lager — Klass (Architecture: X)` rubriker.
- Listor över **ID-begrepp** som "används genomgående".
- Förankringar till specifika wiki-sidor ([[App-Architecture]], [[Figma-to-Compose]], log.md datum).
- Kotlin-novis-förklaringar (data class, LaunchedEffect, state hoisting, suspend/runBlocking, lastTag-pattern etc.).
- Exempel från verklig användning (page 12 success, chooser-fix, scroll-fix).

---

## 3. Teknikstack

**Host / Desktop**
- Arch Linux + Omarchy (Hyprland 0.55.x)
- Android Studio (AUR, panda4-patch1)
- Android SDK (platform 36, build-tools etc.)
- ADB / platform-tools
- bash + udev + pacman/yay
- Git (för framtida)

**App (Kotlin / Android)**
- Språk: Kotlin 2.2.10
- Build: Gradle + Kotlin DSL (settings.gradle.kts, build.gradle.kts), AGP 9.2.1, libs.versions.toml
- UI: Jetpack Compose (BOM 2026.02.01), Material 3, androidx.activity.compose 1.8.0
  - Canvas + InfiniteTransition (radar)
  - LazyColumn + verticalScroll + rememberScrollState
  - TabRow, Card, Button, OutlinedTextField, Text (monospace)
  - LaunchedEffect, mutableStateOf / mutableStateListOf, remember, SideEffect
- NFC: Android SDK (API 24+)
  - `android.nfc.NfcAdapter`, `enableReaderMode` (FLAG_READER_NFC_A|B|F|V + NO_PLATFORM_SOUNDS)
  - `Tag`, `NfcA`, `MifareUltralight` (readPages, writePage, connect/close), `MifareClassic` (authenticate, readBlock, writeBlock)
  - NDEF/TECH intent filters + onNewIntent fallback
  - Vibrator (VibrationEffect)
- Min/target: minSdk 24, targetSdk 36, compileSdk 36
- Debug: edge-to-edge (enableEdgeToEdge), no minify

**NFC-hårdvara (testad)**
- Samsung Galaxy Note 10 SM-N970F/DS (Exynos 9825, Android 12, One UI 4.1, NFC chip S.LSI 4.5.11)
- MIFARE Ultralight / NTAG (7-byte UID, 4-byte pages, lock bytes i page 2, NDEF URI payloads)
- MIFARE Classic 1K/4K (skeleton, default keys)

**Design & process**
- Figma (React/Tailwind export som spec)
- LLM (Grok) som "wiki-maintainer" + kodagent + dokumentatör
- Append-only log + syntes (Karpathy pattern)
- Iterativ: user feedback → wiki → kod → adb install → test på fysisk tagg → log

**Deployment**
- Gratis: adb sideloading (ingen Play Store publicering ännu)
- Framtida: 25 USD engångsavgift för Play Store (valfritt)

---

## 4. Projektstruktur (efter detta arbete)

```
llm-wiki/
├── wiki/
│   ├── index.md
│   ├── log.md (append-only, 2026-05-26 → 2026-06-04)
│   ├── App-Architecture.md
│   ├── Figma-to-Compose.md
│   ├── Hardware-Testenheter.md
│   ├── Projektrapport.md (denna)
│   └── ...
├── raw/ (eventuella källfiler)
└── schema.md

projects/rfid/rfid-manager-android/
├── app/src/main/
│   ├── java/com/joakim/rfidmanager/
│   │   ├── MainActivity.kt (glue + armed write + mapping)
│   │   ├── nfc/
│   │   │   ├── NfcManager.kt (interface)
│   │   │   ├── AndroidNfcManager.kt (handleTag, lastTag, page2, writes)
│   │   │   └── StubNfcManager.kt
│   │   ├── domain/model/RfidTag.kt + TagType
│   │   ├── ui/model/RFIDTag.kt
│   │   ├── ui/screens/
│   │   │   ├── RFIDManagerScreen.kt (huvudskärm + WriteTagForm inlined)
│   │   │   └── RFIDTagList.kt
│   │   └── ui/theme/{Color,Type,Theme}.kt
│   ├── AndroidManifest.xml
│   ├── res/xml/nfc_tech_filter.xml
│   └── res/values/strings.xml
└── build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml ...

rfid-manager/setup/
├── fix-usb-adb.sh
└── 51-android.rules
```

---

## 5. Nyckelerfarenheter & "varför det funkar"

- **Armed write + lastTag** är det mönster som löste "write transceive failed" på Ultralight (lock bits + Tag-livstid).
- **Page 2 alltid läst** + LB0-parser i UI gör writes säkra och användarvänliga ("användaren ser vilka pages som är låsta").
- **Reader mode + NO_PLATFORM_SOUNDS + chooser fallback** = appen får taggen istället för Chrome.
- **State hoisting till Activity** = lifecycle (onNewIntent, onResume) kan påverka samma state som Compose.
- **Wiki som minne** (log + syntes + ID-begrepp) gör att man kan återuppta efter månader och förstå exakt varför koden ser ut som den gör.
- Rika kommentarer med **Architecture-Design-Källkod + ID** gör koden tillgänglig för novis på Kotlin men van programmerare.

---

## 6. Status & framtida steg (vid tidpunkten för rapporten)

**Klart & verifierat:**
- Läsa alla Ultralight pages (inkl. lock) på riktiga eskortminnen.
- Skriva till skrivbar page (page 12 "test" lyckades + verifierats i UI + re-scan).
- Användbart UI (scroll, urval, lock-viz, armed feedback, radar, lista).
- Full dokumentation + rika kommentarer.

**Möjliga nästa steg (ej utförda här):**
- Fler taggtyper / custom keys för Classic.
- Exportera full dump till fil / dela.
- Bakgrunds-NFC / foreground service.
- Bättre RSSI / närvarokontroll.
- GitHub Actions för debug-build.
- Play Store publicering (25 USD).

---

**Avslutande ord**  
Projektet har gått från "tom wiki + ingen Android Studio" till "fungerande app som faktiskt läser och skriver eskortminnen på riktig hårdvara" på ~9 dagar, helt gratis, med stark dokumentation och nu väldokumenterad källkod som binder Architecture (wiki), Design (Figma-tokens + layouter) och Källkod (ID-begrepp i kommentarer) samman.

Allt är redo för paketering och publicering på GitHub.

---

**Senast uppdaterad:** 2026-06-04  
**Rapportförfattare:** Grok 4.3 (xAI) som LLM Wiki-maintainer + kodagent på uppdrag av användaren.  
**Källor:** llm-wiki/log.md (alla entries), App-Architecture.md, Figma-to-Compose.md, Hardware-Testenheter.md, den kommenterade källkoden i RFIDManager-projektet.
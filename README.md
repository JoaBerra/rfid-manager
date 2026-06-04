# RFID Manager — Projekt RF-ID Applikationer på Android (Release 2026-06)

**Fungerande, dokumenterad, kostnadsfri Android-app för att läsa och skriva eskortminnen via NFC (13.56 MHz MIFARE Ultralight/NTAG + Classic skeleton).**

Verifierat på **Samsung Galaxy Note 10 (SM-N970F/DS, Android 12 / One UI 4.1)**:
- Automatisk läsning av Ultralight pages (inkl. page 2 lock/OTP).
- "Armed write" som gör skrivning pålitlig (t.ex. page 12 "74 65 73 74" = "test" ASCII lyckades 2026-06-03).
- Låsbits-visualisering i UI så du undviker låsta pages.
- Hyfsat användbart gränssnitt (radar, lista, SELECTED minnesdump med scroll, WRITE-form med target page + armed status).

Allt är **helt gratis** (AUR Android Studio, sideloading via ADB, ingen Play Store nödvändig).

---

## Snabbstart (host: Arch/Omarchy)

1. **Förbered telefonen** (Galaxy Note 10 eller liknande med NFC):
   - Inställningar → Om telefonen → tryck 7x på "Byggnummer".
   - Utvecklaralternativ → USB-felsökning PÅ + "OEM unlocking" (valfritt).
   - Anslut USB-A till USB-C.
   - Dra ner notifikationsrullgardinen → USB → "USB kontrolleras av: Den här enheten" + "Använd USB till: Överföra filer/Android Auto".

2. **Host setup (ADB + udev)** — kör skriptet:
   ```bash
   cd rfid-setup
   ./fix-usb-adb.sh
   # Logga ut och in (för plugdev-grupp)
   adb kill-server && adb devices   # ska visa din enhet
   ```

3. **Bygg & installera appen**:
   - Öppna `RFIDManager/` i Android Studio (från AUR).
   - Eller från terminal (efter att du har ANDROID_HOME etc.):
     ```bash
     cd RFIDManager
     ./gradlew assembleDebug
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
   - Starta "RFID Manager" på telefonen.
   - Slå på NFC på telefonen.

4. **Användning**:
   - Tryck START SCAN.
   - Håll eskortminne mot baksidan av Note 10 (när det "klickar" vibrerar den).
   - Taggar dyker upp i höger READ LOG (UID, typ, preview, signalbars).
   - Välj en rad → vänster SELECTED visar UID + detaljerad "SELECTED TAG MEMORY" (hex för P2, P4, P8, P12... + låsbits för P2).
   - Gå till WRITE-fliken → välj target page (rekommenderas från lock info), skriv text, "WRITE TO TAG" (armar).
   - Håll taggen omedelbart → nästa detection exekverar writen.
   - Verifiera: re-presentera taggen eller se patched hex i SELECTED.

Se fulla instruktioner i `llm-wiki/wiki/Hardware-Testenheter.md` och `Projektrapport.md`.

---

## Innehåll i paketet

- `RFIDManager/` — ren källkod till Android-appen (inga build-artefakter).
  - `app/src/main/java/com/joakim/rfidmanager/...` — all Kotlin (MainActivity, NFC-lager, UI, domain, theme).
  - `AndroidManifest.xml`, `nfc_tech_filter.xml`, strings, gradle-filer.
  - **Rika kommentarer** överallt som förankrar Architecture (UI/Domain/NFC), Design (Figma tokens + layouter) och källkod med konsekventa ID-begrepp (selectedId, pendingWrite/armed write, handleTag, lastTag, sectorsRead, page 2 LB0, etc.). Perfekt för Kotlin-noviser som kan programmera.

- `llm-wiki/` — hela wikin (Karpathy-style):
  - `wiki/log.md` (append-only kronologi från 2026-05-26).
  - `wiki/Projektrapport.md` (denna rapports syskon — vad, när, tech stack, lager, ID-begrepp).
  - `wiki/App-Architecture.md`, `Figma-to-Compose.md`, `Hardware-Testenheter.md`, `index.md` m.fl.
  - `wiki/` innehåller också .obsidian/ (valfritt för dig som använder Obsidian).

- `rfid-setup/` — udev-regler + bash-script för ADB/USB på Arch (04e8 Samsung + 18d1 Google).

---

## Arkitektur i ett nötskal (ID-begrepp)

Se `llm-wiki/wiki/App-Architecture.md` + de rika KDoc-kommentarerna i koden.

**Tre lager (Architecture):**
- **UI**: Compose screens + theme + ui.model.RFIDTag. Callbacks: onStartScan, onTagSelected, onWrite.
- **Domain**: domain.model.RfidTag (uid, type, sectorsRead: Map<Int,ByteArray>).
- **NFC**: nfc.NfcManager (interface) + AndroidNfcManager (handleTag, lastTag, page2 lock read, armed writes via Ultralight.writePage / Classic).

**Nyckel-ID som löper genom allt:**
- `selectedId` / `selectedTagId` — urval som driver vänster SELECTED + WRITE target.
- `sectorsRead` (domain) / `fullSectors` (ui, hex) — det lästa minnet.
- `pendingWrite` + "armed" — UI armar, nästa fresh detection (med live lastTag) exekverar.
- `handleTag` + `lastTag` — NFC-lagrets hjärta (läser medan Tag lever, lagrar för writes).
- Page 2 LB0 parser — låsbits i UI (både SELECTED och default target i WRITE).
- `enableReaderMode` + `NO_PLATFORM_SOUNDS` + onNewIntent chooser-fallback.

**Design (Figma → Compose):**
- Primary `#00FF88`, Accent `#F59E0B`, mörk terminal, JetBrains Mono, Radius 2dp.
- Radar (Canvas), TabRow READ/WRITE, scrollable SELECTED med lock-varningar.

---

## Bygg & utveckling

- Öppna `RFIDManager` i Android Studio.
- Run på ansluten enhet (rekommenderas; adb install är långsammare ibland).
- Logcat visar all NFC-data (AndroidNfcManager TAG).
- För dev utan telefon: koden har haft StubNfcManager (ersattes av AndroidNfcManager när hårdvara kom).

Gradle wrapper finns (`gradlew`).

---

## GitHub & bidrag

Detta paket är vad som ska läggas upp på GitHub (se separata steg i loggen / Projektrapporten).

Rekommenderad repo-struktur vid publicering:
- Root README (denna).
- `app/` (flytta RFIDManager/app hit eller behåll som submodule).
- `wiki/` (eller länk till separat llm-wiki).
- `rfid-setup/`.
- `.gitignore` (Android + build/ + .obsidian om du vill).

Se `llm-wiki/wiki/Projektrapport.md` för full historik, tech stack och lärdomar.

---

## Licens & kostnad

- App: din egen (du äger koden).
- Verktyg: gratis (Android Studio gratis, sideloading gratis).
- Eventuell publicering på Play Store: engångsavgift 25 USD (Google).

Inga andra licenser/kostnader för utveckling eller privat deployment.

---

**Skapad:** 2026-06-04 av Grok 4.3 (xAI) på uppdrag av användaren som LLM Wiki-maintainer + kodagent.  
**Källor:** Allt i llm-wiki/log.md (2026-05-26 till 2026-06-04), kommenterad källkod, Figma-export, fysisk test på Note 10 + riktiga eskortminnen.

Lycka till med eskortminnena!
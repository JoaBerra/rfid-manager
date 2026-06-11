---
title: Steg-för-steg guide: Bygg Figma-prototypen för Fas 2 (RFID + Barcode + MQTT/Sparkplug)
tags: [figma, guide, fas2, build, prototype, mqtt, persistence]
created: 2026-06-04
---

# Steg-för-steg guide: Bygg Figma-prototypen för Fas 2

**Syfte:** Du bygger den faktiska Figma-designen i din app (gratis eller uppgraderad licens). Jag (Grok) har förberett **exakta namn** från [[Nomenclature-Figma-Android]] så att när du är klar kan vi direkt extrahera tokens och implementera 1:1 i Android Compose (med rich comments, industrial estetik från identsys.se + Fas 1 tokens).

**Estetik:** Industriell/professionell (inspirerat av identsys.se): Ren, funktionell, hög kontrast, fokus på spårbarhet (UID, timestamps, status), ikoner för RFID/Barcode/MQTT, tabular data-känsla, minimal clutter, Primary #00FF88 för action/success, navy/grå bas + accent.

**Filer att ha redo:**
- Öppna din Figma.
- Skapa ny fil eller ny Page: "Fas2-RFID-IoT-Prototype".
- Ha Fas 1 tokens redo (Primary #00FF88, Accent #F59E0B, mörk bakgrund #0A0C0E, JetBrains Mono, Radius 2).

**Tips innan du börjar:**
- Använd Auto Layout överallt (vertical/horizontal, spacing, padding).
- Skapa Components tidigt för återanvändning.
- Använd Variants för states (t.ex. badge Connected/Disconnected).
- Namnge exakt som i guiden – detta är nyckeln till kod-mappning.
- Använd ikoner: Figma har basic icons, eller sök "RFID tag", "barcode", "cloud", "database". Håll enkla line icons som på identsys.
- Testa prototyping (clickable flows).

## Steg 0: Förbered Design System (Variables + Styles)

1. Gå till **Assets** eller **Styles** panel (höger sida).
2. Skapa **Color Styles** (Local styles):
   - `Primary` = #00FF88 (behåll från Fas 1 för continuity)
   - `PrimaryForeground` = #0A0C0E
   - `Accent` = #F59E0B
   - `Background` = #0A0C0E (mörk terminal, anpassa till industrial navy om du vill: #1A2A3A)
   - `Card` = #111416
   - `Foreground` = #E8EAED
   - `MutedForeground` = #6B7280
   - `StatusConnected` = #10B981 (grön, industrial)
   - `StatusDisconnected` = #EF4444
   - `StatusSending` = #F59E0B
   - `IndustrialNavy` = #1A2A3A (ny för industrial touch)
   - `IndustrialSlate` = #2D3748

3. Skapa **Text Styles**:
   - `Heading/Monospace` : Font = JetBrains Mono (eller system monospace), Weight Medium, Size 14-16, Line height 20
   - `Body/Monospace` : JetBrains Mono, Regular, Size 12-13
   - `Label/Monospace` : JetBrains Mono, Medium, Size 10-11
   - Vanliga sans för rubriker om du vill (Inter-liknande).

4. Skapa **Variables** (Design tokens, under Styles eller Variables panel):
   - Color variables som ovan (binda till styles).
   - Number: `--radius-sm` = 2
   - String: `--font-monospace` = "JetBrains Mono"

5. Spara som **Team Library** om du har Professional (annars Local).

## Steg 1: Skapa Atomic Components (återanvändbara byggstenar)

Skapa dessa som **Components** (högerklicka > Create Component). Namn exakt!

### 1.1 PrimaryButton
- Frame: Auto Layout horizontal, padding 12 24, radius 2, fill Primary.
- Text inside: "START SCAN" eller generisk, font Monospace Medium 14, fill PrimaryForeground.
- Variants: Default, Disabled (opacity 0.5).
- Property: Label (text).

### 1.2 MqttStatusBadge
- Frame: Auto Layout, padding 4 8, radius 2.
- Variants:
  - Connected: fill StatusConnected, text "SPARKPLUG CONNECTED" (monospace 10, white).
  - Disconnected: fill StatusDisconnected, text "DISCONNECTED".
  - Sending: fill StatusSending, text "SENDING...".
- Icon left: Simple circle or cloud icon (use Figma icon tool or rectangle).

### 1.3 PersistedListItem
- Frame: Auto Layout vertical or horizontal, padding 12, fill Card, stroke 1px Border.
- Left: Icon (RFID tag icon or barcode icon – use vector or emoji placeholder first). Match the icons visible in the generated mocks (chip/waves for RFID, barcode lines for EAN).
- Center (metadata fields - make these explicit text layers matching the images):
  - UID / Code: "300833B2E1A4F7C9" or "5907789123456" (monospace 13-14, Foreground) – primary identifier.
  - Timestamp: "2024-11-18 14:37:22" (monospace 10-11, MutedForeground) – include full date-time as in mocks.
  - Data preview: "P12: 74 65 73 74" or equivalent hex snippet (monospace 11, Accent).
  - Source / Location: "Source: Gate 3 - Warehouse A" or "Pallet 47-B" or "Asset 112" (smaller text, Muted or industrial label style) – metadata for traceability.
  - Additional context (if space in mock): "Temp: 4.2°C" or "Location: Dock 3" (monospace small).
- Right: MqttStatusBadge (variant, e.g. "Sent via Sparkplug" or "Persisted") + SecondaryButton "Transmit" or "Details".
- Bottom or badge area: Sparkplug version hint like "v4.12.3 - Sparkplug B connected" (small monospace).
- Properties: uid (text), dataPreview (text), timestamp (text), source (text), status (variant), type (RFID/EAN icon switch), transmitted (boolean for badge).
- Match exactly the fields visible in fas2-persisted-readings-list.jpg and fas2-main-rfid-screen.jpg (use the OCR'd examples or adapt to your project data like 0479981A8A6A80 + page 12).

### 1.4 RfidReadingCard (detalj)
- Larger card: Auto Layout vertical.
- Header: UID in large monospace Primary (e.g. "3004E8F92A1C" or "E4:9A:7C:2B:11:FF").
- Metadata section (visible in mocks):
  - Site / Context: "Site: Goteborg Terminal A" or "Pallet 47-B" (small text).
  - Timestamp / Last read: "14:36:12" or full "2025-02-18T10:41:29.881Z".
  - Data details: "Memory pages (hex)" or "memoryBank: 3, address: 0, length: 64" (monospace).
  - Payload preview: hex snippet like "4D4F44454C3A45532D523132..." or "P12: 74 65 73 74".
  - "Lock bits" text in Accent if applicable (from Fas 1).
  - Sparkplug flag: "sparkplug: true" or "Transmitted".
- "Last read" or "SCAN ACTIVE 3.2s ago * 14 tags detected" (as in main mock).
- Button: PrimaryButton "TRANSMIT TO IOT" (onTransmit).
- Properties: uid (text), timestamp (text), site (text), memoryBank/address/length/payload (texts for Sparkplug data), lockInfo (text), transmitted (boolean).
- Match fields from fas2-main-rfid-screen.jpg and fas2-mqtt-sparkplug-status.jpg (use the detailed JSON structure from the LAST TRANSMITTED area in the MQTT mock).

### 1.5 StatCard (återanvänd från Fas 1, utöka)
- Small card: Label (Total Readings), Value (42), monospace.
- Variants for Persisted count, MQTT status.

### 1.6 RadarView (återanvänd exakt från Fas 1)
- Canvas-like: Pulsing circles + sweep (använd Figma's ellipse + rotation eller beskriv som vector).

### 1.7 TabButton / SegmentedControl
- Horizontal Auto Layout of buttons.
- Selected: fill Primary, text PrimaryForeground.
- Unselected: transparent.

## Steg 2: Bygg huvudskärmen RFIDManagerScreen

1. Skapa **Frame** (mobile size 375 x 812 eller 390x844 för modern phone). Namn: `RFIDManagerScreen`.
2. Fill: Background.
3. Auto Layout vertical, padding 16.

**Top bar:**
- Horizontal Auto Layout.
- Left: Segmented [READ | WRITE | PERSISTED] – använd 3 TabButton components. Default PERSISTED highlighted for Fas2 proof.
- Right: PrimaryButton "START SCAN".

**Vänster kolumn (0.55 weight simulerat med två frames side by side):**
- Använd två vertical frames side-by-side med constraints.
- Övre: SystemStatusCard (timestamp + "STANDBY" or "SCAN ACTIVE 3.2s ago * 14 tags detected" + MQTT StatusBadge "SPARKPLUG CONNECTED" or "v4.12.3 - Sparkplug B connected").
- Site metadata: "Site: Goteborg Terminal A" or "RFID ESCORT TRACKER".
- RadarView frame (square, aspect 1:1).
- StatCards grid: 2x2 (Total Readings, Persisted, MQTT Status, Last read UID like "3004E8F92A1C").
- SELECTED TAG MEMORY card (stor, med full hex list + lock info, monospace) – include "Last read: 3004E8F92A1C".
- LAST DETECTED card with recent readings list (e.g. "3004E8F92A1C * 14:36:12  Pallet 47-B  Location: Dock 3", "A7BIC2DAESF6 * 14:35:48  Asset 112  Temp: 4.2°C", "F1129A4C883D * 14:35:19  Container XJ-19  Transmitted"). Match the "Recent RFID Readings" visible in the main mock image.

**Höger kolumn:**
- TabRow: READ LOG | WRITE TAG | PERSISTED READINGS.
- Under: 
  - READ LOG: RFIDTagList (återanvänd komponenter från Fas1, lägg till persist button).
  - WRITE TAG: WriteEscortForm (utökad med checkbox "Persist after write" + target page).
  - PERSISTED READINGS: PersistedReadingsList (använd PersistedListItem).

**Prototyping:**
- Klicka på PERSISTED tab → visar listan.
- Klicka på item i list → "navigera" till detaljvy (använd overlay eller ny frame länk).

## Steg 3: Bygg PersistedReadingsScreen (dedikerad eller i tab)

1. Ny Frame: `PersistedReadingsScreen`.
2. Header: Text "PERSISTED READINGS" (monospace 14, Muted), count text, "Clear All" (destructive text button).
3. Filter: Horizontal tabs "All / RFID / EAN".
4. Main content: Vertical Auto Layout of PersistedListItem (5-6 exempel). Match metadata visible in the images (fas2-persisted-readings-list.jpg and fas2-main-rfid-screen.jpg):
   - Exempel data (use realistic traceability fields from mocks):
     - RFID: UID `300833B2E1A4F7C9`, Data `P12: 74 65 73 74` or memoryBank/address, Timestamp `2024-11-18 14:37:22`, Source `Gate 3 - Warehouse A` or `Pallet 47-B`, Status "Sent via Sparkplug" or "v4.12.3 - Sparkplug B connected", Transmit/Details button.
     - EAN: Code `5907789123456`, Timestamp `2024-11-18 14:29:51`, Source `Receiving Dock B` or `Line 7 - Palletizer`, Status "Persisted" or "Not sent".
     - Additional visible in main mock: "3004E8F92A1C * 14:36:12  Pallet 47-B  Location: Dock 3", "A7BIC2DAESF6 * 14:35:48  Asset 112  Temp: 4.2°C", "F1129A4C883D * 14:35:19  Container XJ-19  Transmitted".
5. Empty state frame: Icon + "No persisted readings yet..." + "Scan now" button.
6. Bottom: Info text "Data persisted locally with Room. Transmit uses Sparkplug B over MQTT." + version "v4.12.3 - identsys.se * Traceability Engine".

**Interactions:** 
- Transmit button on item → change badge to "Sending..." then "Sent", show toast-like "Transmitted to test env".

## Steg 4: Bygg MqttStatus / Controls (bottom sheet eller egen frame)

1. Frame `MqttStatusSheet` (height ~400).
2. Large MqttStatusBadge ("SPARKPLUG CONNECTED" with "Last heartbeat: 10:41:37.224 | Session: 14h 22m").
3. Section "Connection": Buttons "Connect" / "Disconnect" (Secondary).
4. "LAST TRANSMITTED": Card with full JSON preview exactly matching the mock (visible fields):
   {
     "type": "ReadEscortMemory",
     "uid": "E4:9A:7C:2B:11:FF",
     "timestamp": "2025-02-18T10:41:29.881Z",
     "data": {
       "memoryBank": 3,
       "address": 0,
       "length": 64,
       "payload": "4D4F44454C3A45532D523132..."
     },
     "sparkplug": true
   }
5. "RECENT MQTT MESSAGES": Vertical list of MqttMessageLogItem (include examples from mock: "ReadEscortMemory E4:9A:7C:2B:11:FF 10:41:29 {"type":"ReadEscortMemory","uid":"E4:9A..."} PUB", "NodeBirth EdgeNode-PlantA-RFIDOT 10:39:12", "DeviceData Status update 10:34:51" with PUB/SUB indicators).
6. Test button: "Test Publish Reading" / "View Log" (triggers onTransmitReadings with the JSON above).

**Metadata fields to expose (visible in fas2-mqtt-sparkplug-status.jpg and cross-referenced in other mocks):**
- type (e.g. "ReadEscortMemory" – active verb + noun)
- uid (hex with or without colons)
- timestamp (ISO or simple time)
- data/memoryBank, address, length, payload (hex string)
- sparkplug: true / "Sent via Sparkplug"
- heartbeat, session duration
- seq / correlationId (via messages or explicit)
- PUB / SUB / NodeBirth / DeviceData labels for Sparkplug B compliance
- Version "v4.12.3 - Sparkplug B connected" or "identsys.se * Traceability Engine"

**Variants for sheet:** Connected, Disconnected, Sending.

**Nya komponenter:**
- `MqttStatusBadge(status: MqttConnectionState)`
- `MqttMessageLogItem(message: MqttLogEntry)`
- `MqttConnectionControls`
- `LastTransmittedJsonCard` (with the exact JSON structure and fields listed above)

## Steg 5: Lägg till ikoner och finishing touches (industrial feel)

- Använd Figma's Icon tool eller sök "linear icons" för:
  - RFID tag (chip med waves).
  - Barcode scanner.
  - Cloud/MQTT (för status).
  - Database (persist).
  - Checkmark for transmitted.
- Placera ikoner vänster i list items och badges.
- Lägg till subtila borders, shadows (low for industrial clean).
- Testa dark mode consistency.
- Lägg till "From identsys.se inspiration" comment i Figma för dig själv.

## Steg 6: Prototyping & Flows (användarresa)

- Huvudflöde:
  1. RFIDManagerScreen → klicka PERSISTED tab → visar lista.
  2. Klicka Transmit på item → badge uppdateras + logg i Mqtt sheet uppdateras.
  3. Klicka item → overlay med RfidReadingCard (med Transmit button).
- Lägg till "Simulate Scan" button som lägger till dummy persisted item.
- Använd Figma's Prototype mode: Drag arrows mellan frames/screens.
- Lägg till "Hold tag to scan" modal för realism.

## Steg 7: Organisera & Förbered för handoff

- Använd **Pages**: 
  - Design System
  - Components (alla atomic)
  - Screens (RFIDManagerScreen, Persisted..., Mqtt...)
- **Naming**: Exakt som i guiden (t.ex. component "PersistedListItem").
- Lägg till **Comments** i Figma: "This maps to @Composable PersistedListItem in Compose. See Nomenclature in wiki."
- Exportera när klar:
  - Styles/Variables (för tokens).
  - Hela filen som ZIP eller använd Dev Mode för specs.
  - Skärmdumpar av flöden.

## Steg 8: Efter byggandet (när du är klar imorgon)

1. Exportera tokens/styles (som i Fas 1).
2. Dela export eller screenshots med mig.
3. Jag:
   - Uppdaterar Compose theme med nya industrial variables.
   - Implementerar skärmarna i Android (återanvänd RadarView, StatCard etc. från Fas 1).
   - Kopplar Room persistence + Sparkplug/MQTT client (Paho + vår payload struktur).
   - Lägger till EAN scanning placeholder.
   - Testar mot din lokala Docker MQTT + Python scripts.
4. Vi validerar flödet: Scan → Persist → Transmit → ta emot i test env.

**De genererade UI-mock-bilderna (från igår):**
Jag har kopierat dem till lättillgängliga platser så du kan ladda upp dem direkt till Figmas AI-verktyg (t.ex. image-to-design, variations, eller FigJam AI):

- `~/Fas2-Figma-UI-Mocks/fas2-main-rfid-screen.jpg`  (huvudskärm med radar, status, lista)
- `~/Fas2-Figma-UI-Mocks/fas2-persisted-readings-list.jpg` (persisted readings list med filter, items, status badges)
- `~/Fas2-Figma-UI-Mocks/fas2-mqtt-sparkplug-status.jpg` (MQTT/Sparkplug status, log, test publish)

Ytterligare kopior finns i:
- `~/rfid-manager/test/fas2-mqtt/figma-mocks/`
- `~/rfid-manager/releases/2026-06-Fas2/Fas2-Figma-UI-Mocks/`

Dessa är de tre bilderna som skapades baserat på den industriella estetiken (identsys-inspiration + Fas1 Primary #00FF88 + clean professional layout). Använd dem som referens eller ladda upp till Figma AI för att generera/iterera lager i din design.

**Exempel på metadata fields (använd i din design - matcha exakt de som är synliga i de tre genererade bilderna):**
- Från persisted list & main screen (fas2-persisted-readings-list.jpg, fas2-main-rfid-screen.jpg):
  - UID/Code: `300833B2E1A4F7C9` or `5907789123456` or `3004E8F92A1C` (monospace primary)
  - Timestamp: `2024-11-18 14:37:22` or `14:36:12`
  - Data preview: `P12: 74 65 73 74` or hex snippet
  - Source/Location/Context: `Source: Gate 3 - Warehouse A`, `Pallet 47-B`, `Location: Dock 3`, `Asset 112`, `Temp: 4.2°C`, `Container XJ-19`
  - Status: `Sent via Sparkplug`, `Persisted`, `Transmitted`, `v4.12.3 - Sparkplug B connected`
  - Site: `Site: Goteborg Terminal A` or `RFID ESCORT TRACKER`
  - Scan info: `SCAN ACTIVE 3.2s ago * 14 tags detected`, `Last read: 3004E8F92A1C`
- Från MQTT status (fas2-mqtt-sparkplug-status.jpg):
  - Full JSON: type "ReadEscortMemory", uid "E4:9A:7C:2B:11:FF", timestamp ISO, data {memoryBank: 3, address: 0, length: 64, payload: "4D4F44454C3A45532D523132..."}, sparkplug: true
  - Heartbeat/Session: `Last heartbeat: 10:41:37.224 | Session: 14h 22m`
  - Messages: type + uid + time + short JSON + PUB/SUB, NodeBirth "EdgeNode-PlantA-RFIDOT", DeviceData "Status update"
  - Buttons: `Test Publish Reading`, `View Log`
- Allmänna: correlationId/seq via messages, version hints like "v4.12.3 - identsys.se * Traceability Engine", nav labels "Scan Readings Dashboard Settings" or "Readings Assets"

Använd dessa som text layers i komponenterna. Anpassa data till ditt projekt men behåll fältstrukturen för traceability (UID, timestamp, source, data details, Sparkplug metadata). Se bilderna i ~/Fas2-Figma-UI-Mocks/ för exakt visuell placering.

**Begränsningar & Tips:**
- Gratis konto: Funkar bra för detta (components, auto layout, prototype). Uppgradera om du vill ha bättre version history eller team.
- Håll det enkelt först – bygg kärnkomponenterna, iterera.
- Om du fastnar på en specifik del: Skicka screenshot eller beskriv, så ger jag mer detaljerade instruktioner.

Bygg detta steg för steg imorgon. När du är klar säger du "Figma klar, exportera" så tar vi nästa steg i koden och testmiljön.

Allt är förberett för sömlös övergång från Figma → Android med exakta namn och industrial estetik.

Sov gott! Vi ses imorgon. 

(Detta är sparat i wikin för referens: [[Figma-Steps-Fas2-Build-Guide]]. Uppdaterad log.md med "körde steg-för-steg-guiden".)
---
title: Figma Prototype Spec – Fas 2 Proof of Concept (Mobile App)
tags: [figma, design, prototype, fas2, mqtt, persistence, rfid]
created: 2026-06-04
---

# Figma Prototype Spec – Fas 2 Proof of Concept

**Mål:** En mobilapp i Figma som demonstrerar:
- Läsa/skriva eskortminne (återanvänd + utöka Fas 1).
- Lista persisterade läsningar (RFID + framtida Barcode).
- Skicka data via MQTT till testmiljö (med status).
- Namn är **exakta** och kommer att återanvändas 1:1 i Android Compose-kod (se [[Nomenclature-Figma-Android]]).

**Grok driver namnen.** Du (användaren) bygger detta i din lokala Figma-app (gratis konto). När klar: exportera som i Fas 1 för tokens + vi bygger Compose med samma namn + rich comments.

## Design System (återanvänd från Fas 1 + nya)

- Färger: Primary #00FF88 (neon), Accent #F59E0B, mörk terminal bakgrund.
- Typsnitt: JetBrains Mono tungt för UID/hex/data.
- Radius: 2dp.
- Nya tokens/variabler (enligt Nomenclature):
  - `--status-connected` (grön)
  - `--status-disconnected` (grå/röd)
  - `--status-sending` (accent)
  - `--card-persisted`

## Huvudskärmar (Frames)

### 1. RFIDManagerScreen (utökad från Fas 1)
- Top bar: Segmented [READ | WRITE | PERSISTED] + START SCAN (grön PrimaryButton)
- Vänster: RadarView (oförändrat) + StatCards (Total Readings, Persisted, MQTT Status)
- Höger: TabRow eller segmented för:
  - READ LOG (Fas 1 RFIDTagList)
  - WRITE TAG (WriteEscortForm – utökad med "Persist after write" toggle)
  - **PERSISTED READINGS** (ny): ReadingList med filter "All / RFID / Barcode"

**Interaktioner:**
- Tap på reading i lista → navigerar till detalj (RfidReadingCard)
- "Transmit" knapp på persisted item → anropar onTransmitReadings (visar MqttStatusBadge "Sending..." → "Sent")

### 2. PersistedReadingsScreen (dedikerad vy, kan vara modal eller egen screen)
- Header: "Persisted Readings" + count + "Clear All" (destructive)
- ReadingList (Lazy vertical):
  - Varje item: PersistedListItem
    - Icon (RFID eller Barcode)
    - UID / data preview (monospace)
    - Timestamp
    - MqttStatusBadge (Not sent / Sent)
    - "Transmit" SecondaryButton
- Empty state: "No persisted readings yet. Scan or read an escort memory."

**Component names (exakta för Compose):**
- `PersistedReadingsList`
- `PersistedListItem(reading: PersistedReading, onTransmit: () -> Unit, onClick: () -> Unit)`
- `RfidReadingCard` (detalj: full hex, lock info från Fas 1, "Transmit to MQTT" PrimaryButton)

### 3. MqttStatusScreen / Bottom sheet (för proof)
- MqttStatusBadge stort (Connected / Disconnected / Sending)
- "Connect" / "Disconnect" knapp
- Logg: MqttMessageLog (lista över skickade/mottagna JSON-meddelanden, med "type": "ReadEscortMemory" etc.)
- Test-knapp: "Simulate Publish Reading" (anropar onTransmitReadings med dummy data)

**Nya komponenter:**
- `MqttStatusBadge(status: MqttConnectionState)`
- `MqttMessageLogItem(message: MqttLogEntry)`
- `MqttConnectionControls`

## Exempel på en läsning (RFID)
- UID: 0479981A8A6A80 (monospace, Primary färg)
- Data: P12: 74 65 73 74 ... (från Fas 1 framgång)
- Persisted: Yes, timestamp
- MQTT: Sent via "ReadEscortMemory" message type (JSON)

## Flöde i prototypen (användarresa)
1. Öppna app → Radar snurrar (Fas 1).
2. START SCAN → "Hold tag" (Fas 1).
3. Tagg dyker i READ LOG.
4. Välj → SELECTED + "Persist this reading" (ny knapp, använder onPersistReading).
5. Gå till PERSISTED tab → ser den i listan (PersistedListItem).
6. Tap Transmit → MqttStatusBadge visar "Sending..." (accent), sedan "Sent" (connected).
7. (I testmiljö) Subscribern tar emot "ReadEscortMemory" och persisterar i sin SQLite.

## Nästa steg efter att du byggt i Figma
- Exportera (ZIP eller tokens).
- Vi extraherar tokens (som i Fas 1 Figma-to-Compose).
- Bygger Compose-screens med exakta namn + Room + MQTT (Paho) + rich comments.
- Testar mot den lokala Docker-Mosquitto + Python scripts vi redan har igång.

**Status:** Detta är den konkreta "tekniska bevisningen" du bad om. Namn är låsta i [[Nomenclature-Figma-Android]] så de kan återanvändas direkt i Android Studio.

Bygg detta i din Figma nu – jag väntar på export eller feedback för att fortsätta med kod + mer testskript.

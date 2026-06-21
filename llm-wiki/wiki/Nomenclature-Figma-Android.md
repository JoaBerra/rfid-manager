---
title: Nomenclature – Namnsättning Figma + Android Studio (Fas 1 + Fas 2)
tags: [nomenklatur, naming, figma, android, kotlin, mqtt, arkitektur]
created: 2026-06-04
---

# Nomenclature – Namnsättning för Figma och Android (Projekt RF-ID / Fas 1 & Fas 2)

**Syfte:** Definiera konsekvent namnsättning så att:
- Grok kan driva Figma-design med exakta komponent-/variabelnamn.
- Samma namn återanvänds direkt i Android Studio (Compose, ViewModels, repositories, MQTT message types, DB entities).
- Lätt att navigera mellan design och kod.
- Återanvänds från Fas 1 (Primary, RadarView, SELECTED TAG MEMORY, armed write etc.).

All namnsättning följer **verb + substantiv** för action-orienterade begrepp (inspirerat av användarens krav på semantiska meddelanden) där det är naturligt.

## 1. Övergripande principer

- **PascalCase** för typer/komponenter (Figma + Kotlin).
- **camelCase** för variabler, funktioner, topics-delar.
- **kebab-case** eller **snake** undviks i kod; används bara i MQTT topics om nödvändigt för läsbarhet.
- **Verb + Noun** för actions: `ReadEscortMemory`, `PersistBarcodeScan`, `TransmitReadings`.
- **Figma → Code mapping**: Varje Figma Component / Variant / Variable får exakt motsvarande namn i Compose (t.ex. `RfidReadingCard` @Composable).
- Återanvänd Fas 1: `Primary` (#00FF88), `Accent`, `RadarView`, `StatCard`, `onStartScan`, `selectedId`, `armedWrite` / `pendingWrite`.
- Alla nya begrepp dokumenteras här + i rich comments i koden + i wiki-arkitektur.

## 2. Figma-namnsättning (Grok driver dessa)

### Frames / Screens (huvudskärmar)
- `RFIDManagerScreen` (huvud, återanvänd från Fas 1)
- `PersistedReadingsScreen` (ny: lista över lokalt sparade RFID + Barcode)
- `MqttTestScreen` (ny: status, send test, receive log – för proof-of-concept)

### Komponenter (återanvändbara, exakta namn som blir @Composable)
- `RfidTagCard` (utökad från Fas 1 RFIDTagList item)
- `RfidReadingCard` (detaljvy för en läsning, inkl. raw hex + parsed)
- `BarcodeReadingCard`
- `PersistedListItem` (rad i lista över sparade)
- `MqttStatusBadge` (Connected / Disconnected / Sending, med färg)
- `MqttMessageLogItem` (rad i logg av skickade/mottagna)
- `PrimaryButton` (grön, från Fas 1 Primary)
- `SecondaryButton`
- `ScanButton` (för RFID + framtida Barcode)
- `ReadingList` (Lazy list)
- `WriteEscortForm` (utökad WriteTagForm)

### Variabler / Tokens (Design System, återanvänd + nya)
- Befintliga från Fas 1: `--color-primary` (#00FF88), `--color-accent` (#F59E0B), `--font-monospace`, `--radius-sm` (2px)
- Nya för Fas 2:
  - `--status-connected` (grön)
  - `--status-disconnected` (röd/grå)
  - `--status-sending` (accent)
  - `--card-persisted` (lätt variant av Card)
  - `--mqtt-topic` (monospace highlight)

### Interaktioner / States
- `onReadEscortMemory`
- `onWriteEscortMemory`
- `onPersistReading`
- `onTransmitReadings`
- `onScanBarcode` (framtida)
- `isMqttConnected`, `lastMqttError`

**Regel:** När du bygger i Figma, namnge Component exakt som ovan. Använd Variants för states (e.g. MqttStatusBadge / Connected, Disconnected).

## 3. Android / Kotlin namnsättning (återanvänd + utökad från Fas 1)

### Package-struktur (utökad)
```
com.joakim.rfidmanager
├── ui
│   ├── screens
│   │   ├── RFIDManagerScreen.kt          # Fas 1 huvud
│   │   ├── PersistedReadingsScreen.kt    # Ny
│   │   ├── MqttStatusScreen.kt           # Ny (proof)
│   │   └── components/                   # Delade (RfidReadingCard etc.)
│   ├── theme/                            # Oförändrat + nya tokens
│   └── model/                            # UI-modeller (utökas)
├── domain
│   ├── model/
│   │   ├── RfidTag.kt                    # Fas 1
│   │   ├── BarcodeReading.kt             # Ny
│   │   ├── RfidReading.kt                # Ny (domän för persisterad)
│   │   └── MessageType.kt                # Ny: enum ReadEscortMemory, etc.
│   └── repository/                       # Ny lager (tidigare "Data")
│       ├── ReadingRepository.kt
│       └── MqttRepository.kt
├── data
│   ├── local/                            # Room
│   │   ├── AppDatabase.kt
│   │   ├── entities/
│   │   │   ├── RfidReadingEntity.kt
│   │   │   └── BarcodeReadingEntity.kt
│   │   └── dao/
│   ├── mqtt/
│   │   ├── MqttClient.kt                 # Wrapper runt Paho/Hive
│   │   └── MqttMessage.kt                # Wrapper med semantic type
│   └── nfc/                              # Oförändrat från Fas 1 (NfcManager etc.)
├── mqtt/                                 # Hög-nivå (kan flyttas)
└── MainActivity.kt                       # Glue, utökas med ViewModels
```

### Klassenamn / Typer (Verb + Noun)
- `ReadEscortMemory` (message type / use case)
- `WriteEscortMemory`
- `PersistRfidReading`
- `PersistBarcodeScan`
- `TransmitReadingsBatch`
- `RfidReadingEntity`
- `BarcodeReadingEntity`
- `MqttReadingEnvelope` (wrapper med correlationId, timestamp, type)

### MQTT-specifikt (Sparkplug B locked for industrial)
- **Topics**: Follow Sparkplug B namespace for compatibility: `spBv1.0/RFID-Group/NDATA/Edge-Device/{deviceId}` for telemetry (or NCMD for commands). Simple fallback `rfidmanager/{deviceId}/telemetry`.
- **Payload**: Sparkplug B structure (timestamp, metrics array with name/datatype/value/timestamp, seq). Our verb+noun as metric name, e.g. "ReadEscortMemory". Value can be JSON string for our data or structured metrics.
  Example (Sparkplug-inspired):
  ```json
  {
    "timestamp": 1780603072194,
    "metrics": [
      {
        "name": "ReadEscortMemory",
        "timestamp": 1780603072194,
        "dataType": "String",
        "value": "{\"uid\": \"0479981A8A6A80\", \"page\": 12, \"dataHex\": \"74657374\"}"
      }
    ],
    "seq": 1
  }
  ```
- Full Sparkplug compliance (birth/death, aliases, protobuf option) later; start with JSON over MQTT as per simplicity.
- **MessageType** enum i domain: `ReadEscortMemory`, `WriteEscortMemory`, `PersistReading`, `TransmitReadings`, `ScanBarcode`, `MqttStatusUpdate`.
- See updated test scripts in ~/projects/rfid/rfid-manager/test/fas2-mqtt/mqtt/ for examples.

### Compose / UI
- `RfidReadingCard(reading: RfidReading, onTransmit: () -> Unit)`
- `PersistedReadingsList(readings: List<...>, onItemClick: ...)`
- `MqttStatusBadge(status: MqttConnectionState)`

## 4. Mappning Figma → Android (Grok ansvar)

När Grok designar i Figma-spec:
- Component "RfidReadingCard" → `@Composable fun RfidReadingCard(...)`
- Variable `--status-connected` → `val StatusConnected = Color(...)` + använd i Theme + direkt i badge.
- Interaction "onTransmitReadings" → callback i Composable + anrop till ViewModel/MqttRepository.

Detta gör att design och kod är 1:1 och lätt att hålla synkade.

## 5. Semantisk meddelandemodell (rekommendation)

**Verb + Substantiv** som "type" i JSON-payload (enligt din önskan).

Exempel på giltiga typer (utökas i Fas 2):
- `ReadEscortMemory`
- `WriteEscortMemory`
- `PersistRfidReading`
- `PersistBarcodeScan`
- `TransmitReadings`
- `RequestPersistedReadings` (från testmiljö till mobil)

**Alternativ standard:** Om kunden kräver industriell interoperabilitet → **Sparkplug B** (se research i log). Den använder predefined Message Types (NDATA, NCMD) + metrics. Vi kan mappa våra verb till Sparkplug om så önskas.

**JSON alltid** som payload-format (enligt dig).

---

**Senast uppdaterad:** 2026-06-04 (Fas 2 prep start)
**Kopplingar:** Se [[App-Architecture]] (uppgraderad sektion), log.md, Projektrapport.md. Används i alla rich comments i kommande kod.
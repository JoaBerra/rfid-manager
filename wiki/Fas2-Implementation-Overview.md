# Fas 2 Implementation Overview

**Mål:** Utöka den befintliga RFIDManager-appen med:
- Lokal persistens (Room) av RFID- och EAN-läsningar.
- MQTT/Sparkplug B-kommunikation för att skicka läsningar till testmiljö.
- UI för att visa persisterade läsningar och MQTT-status.
- Stöd för EAN-streckkoder (kamera).

**Filosofi:** All design och nomenclature är gjord för 1:1-mappning till Jetpack Compose. Vi använder den detaljerade Figma Design Specification (från Figma AI) + referensbilder + vår Nomenclature som källa. Inget behov av Figma-filer längre.

## Planerad package-struktur (utöver Fas 1)

```
com.joakim.rfidmanager
├── data
│   ├── local
│   │   ├── AppDatabase.kt
│   │   ├── dao
│   │   │   └── PersistedReadingDao.kt
│   │   └── entities
│   │       └── PersistedReadingEntity.kt
│   ├── mqtt
│   │   ├── MqttClient.kt                 # Wrapper runt Paho + Sparkplug payload
│   │   └── SparkplugPayload.kt           # Helpers för metrics, NBIRTH etc.
│   └── repository
│       └── PersistedReadingRepository.kt
├── domain
│   └── model
│       ├── PersistedReading.kt
│       └── (eventuellt BarcodeReading, SparkplugMessage)
├── ui
│   ├── components
│   │   ├── PersistedListItem.kt
│   │   ├── ReadingListItem.kt
│   │   ├── MqttStatusBadge.kt
│   │   ├── MqttMessageLogItem.kt
│   │   ├── LastTransmittedJsonCard.kt
│   │   └── RadarView.kt (återanvänd från Fas 1)
│   └── screens
│       ├── PersistedReadingsScreen.kt
│       └── MqttStatusScreen.kt
└── (MainActivity.kt utökas med DB + MQTT + navigation)
```

## Steg-för-steg (A1–A4 som valdes)

### A1 – Wire up databasen
- Skapa enkel DatabaseProvider (eller singleton via MainActivity).
- Injicera i Repository.
- Se till att housekeeping kan köras.

### A2 – UI-komponenter & screens
- Bygg atomiska komponenter exakt enligt nomenclature + Figma-spec (metadata-fält, states, Sparkplug-beteckningar).
- Bygg PersistedReadingsScreen och MqttStatusScreen.
- Återanvänd befintliga Fas 1-komponenter (RadarView, StatCard, PrimaryButton etc.).

### A3 – MQTT/Sparkplug-sändning
- Skapa MqttClient wrapper som skickar i Sparkplug B-format (metrics-array, seq, timestamp).
- Koppla "Transmit" från UI till repository + mqtt.
- Hantera pending readings.

### A4 – Housekeeping + integration med NFC
- Efter lyckad read/write → auto-persist (använd befintlig NFC-flöde).
- Housekeeping-rutin (radera gamla läsningar).
- UI: "Persist after write"-toggle, lista med filter RFID/EAN, status-badges.

## Referenser
- [[Figma-Design-Spec-Fas2]] (den detaljerade handoff-dokumentet från Figma AI)
- [[Nomenclature-Figma-Android]] (exakta namn)
- [[Figma-Steps-Fas2-Build-Guide]] (steg-för-steg för den som vill återskapa i Figma)
- De tre referensbilderna i `~/Fas2-Figma-UI-Mocks/`

## Status (2026-06-05)
- Data-lager (entities, dao, repo, database) på plats.
- Build-filer uppdaterade för Room + KSP + Paho.
- Nästa: A1 (wire up) → A2 (UI) → A3 (MQTT) → A4 (integration).

All kod följer Fas 1:s stil: rich comments, exakta namn från nomenclature, industrial estetik (identsys-inspiration), Sparkplug B som meddelandemodell.

**Status (2026-06-05):**
- Data-lager (entities, dao, repo, database) på plats.
- Build-filer uppdaterade för Room + KSP + Paho.
- A1–A4 genomförda:
  - A1: Database wired via DatabaseProvider + AppContainer i MainActivity.
  - A2: PersistedListItem + PersistedReadingsScreen integrerad i tredje tab. Persist toggle i Write.
  - A3: MqttSender + första MqttStatusScreen.
  - A4: Auto-persist efter write + housekeeping metoder.
- Wiki: Ny [[Fas2-Implementation-Overview]], uppdaterad log och index.
- Nästa: End-to-end test, ViewModel, mer Mqtt integration.

**Status (2026-06-07) – UAT av Kund:**
- Full end-to-end validerad: NFC write → persist → Transmit → MQTT (Sparkplug-stil) → subscriber + SQLite på testdator.
- EPERM-problem på Samsung debug build löst (manifest + network_security_config + Samsung-inställningar).
- **UAT godkänd av Kund för Fas 2** (se [[Kundrelationer-och-Acceptans]] för formell tidsstämplad sign-off 2026-06-07).
- Öppna punkter vid godkännande: EAN, ViewModel, riktig Room (KSP), polish + kryptering i prod (dev okrypterat godkänt).
- Se även den formella buggrapporten [[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] (Resolved).


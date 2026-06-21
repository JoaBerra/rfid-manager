# RFID Manager

**Industriell Android-applikation för RFID/eskortminne-hantering + IoT-integration**

## Projektöversikt

En kostnadsfri Android-app utvecklad på Arch Linux + Omarchy (Hyprland) för:
- Läsa/skriva **eskortminnen** (MIFARE Ultralight, Classic m.fl.) via NFC
- Lokal persistens av läsningar (Room)
- MQTT/Sparkplug B-kommunikation till testmiljö
- Dashboard och MCP-server för realtidsövervakning

## Struktur

- **`RFIDManager-android/`** – Android Studio-källkod (Kotlin + Jetpack Compose)
- **`llm-wiki/`** – Karpathy-style kunskapsbas och dokumentation
- **`releases/`** – Versionerade snapshots och tarballs
- **`test/fas2-mqtt/`** – Testmiljö (Mosquitto, Python subscriber, simulatorer)
- **`dashboard/`** – Realtids webb-dashboard
- **`mcp-server/`** – MCP-server för AI-assistenter
- **`manual/`** – Användarmanual och screenshots
- **`setup/`** – Installationsskript (ADB, USB-regler etc.)

## Snabbstart

### Android-app
```bash
cd RFIDManager-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
MQTT Testmiljö
Bashcd test/fas2-mqtt
docker compose up -d
# Starta subscriber
./mqtt/test_subscriber_persist.py
Dashboard
Bashcd dashboard
docker compose up --build
# Öppna http://localhost:8000
Status (2026-06-21)

Fas 2: UAT-godkänd (persistens + MQTT)
Fas 3: Pågående (navigation, andrum, Room)
Fas 300: MCP-server och dashboard implementerad

Se releases/ och wiki/ för detaljerad dokumentation.
Länkar

GitHub: https://github.com/JoaBerra/rfid-manager
Wiki: llm-wiki/ (Obsidian)
Android-kod: https://github.com/JoaBerra/rfid-manager-android

Utvecklat med Grok + lokal LLM-hjälp på Arch Linux/Omarchy.

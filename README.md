# RFID Manager — Projektöversikt

> **Senaste milstolpe (2026-06-19):** Fas 300 — MCP-server för RFID Manager Dashboard.  
> Exponerar dashboardens API som MCP-verktyg för AI-assistenten: statistik, meddelanden, publicering, live-händelser.

---

## Projektstatus

| Fas | Innehåll | Status |
|-----|----------|--------|
| **Fas 1–6** | NFC-läsning/skrivning, MQTT, navigation, i18n, dark mode, v1.0 release | ✅ Alla klara |
| **Fas-100** | MQTT-infrastruktur — fördjupning (pågående) | 🔄 ~30% |
| **Fas-101** | MQTT-klientkonfiguration i appen | 📋 Planerad |
| **Fas 200** | MQTT Realtidsdashboard — FastAPI, SSE, Docker Compose | ✅ Godkänd 2026-06-19 |
| **Fas 300** | MCP-server — AI-assistentåtkomst till dashboard + MQTT | 🔄 Implementerad, UAT pågår |
| **Fas 400** | Teknikmiljö validering — Git clone, verifiering, bootstrap | 📋 Planerad |

---

## Snabbstart — Android-app

```bash
cd RFIDManager
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Snabbstart — MQTT-demo

```bash
# 1. Starta broker
docker run -d --rm --name rfid-mqtt-test -p 1883:1883 \
  -v ~/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf \
  eclipse-mosquitto mosquitto -c /mosquitto/config/mosquitto.conf

# 2. Starta dashboard
cd ~/rfid-manager/dashboard
docker compose up --build
# Öppna http://localhost:8000

# 3. Transmit från telefonen → meddelanden syns i dashboardens live-flöde
```

---

## Innehåll

- `RFIDManager/` — Android-app (Kotlin, Compose, Material 3)
  - NFC-läsning/skrivning, MQTT (Paho), persistens, i18n, dark mode
- `llm-wiki/` — Projektdokumentation (Karpathy-style, Obsidian)
  - [[wiki/MQTT-Manual]] — Praktisk MQTT-bruksanvisning
  - [[wiki/Fas-200-Web-Dashboard]] — Dashboardens arkitektur
- `rfid-setup/` — UDEV-regler + ADB-skript
- `~/rfid-manager/dashboard/` — Realtidsdashboard (FastAPI + SSE)
- `~/rfid-manager/mcp-server/` — MCP-server (AI-assistentåtkomst till dashboard + MQTT)

## MCP-server (AI-assistentåtkomst)

```bash
# Starta (kräver dashboard + broker igång)
cd ~/rfid-manager/mcp-server
../dashboard/.venv/bin/python server.py

# Används automatiskt av opencode via konfigurationen i ~/.config/opencode/opencode.json
# Fråga assistenten: "Vad är status på dashboarden?" eller "Visa senaste MQTT-meddelandena"
```

---

**GitHub:** <https://github.com/JoaBerra/rfid-manager>  
**Wiki-index:** [wiki/index.md](wiki/index.md)
=======

# rfid-manager – Projektstöd och artefakter

Detta är den logiska samlingsplatsen för stödjande filer, testmiljöer, release-snapshots och artefakter för projektet **RFID Manager** (GitHub: [JoaBerra/rfid-manager](https://github.com/JoaBerra/rfid-manager)).

## Struktur

- **setup/**  
  Udev-regler och ADB-fix-skript för Samsung Note 10 (plugdev, USB debugging).  
  Används vid hårdvaruinstallation.

- **test/fas2-mqtt/**  
  Dedikerad testmiljö för Fas 2 MQTT/Sparkplug.  
  Innehåller:
  - Docker Mosquitto-konfiguration
  - Python subscriber + simulate scripts (`test_subscriber_persist.py`, `simulate_mobile_publish.py`)
  - Test-databas (`test_persisted_readings.db`)
  - Figma-mocks (kopia)

- **releases/2026-06-Fas2/**  
  Paketerad release-snapshot för taggen `fas-2-uat-godkand-2026-06-07`.  
  Detta är den katalog som används för `git push` till GitHub (innehåller kopior av källkod, llm-wiki, setup, Figma-mocks).  
  Se `releases/2026-06-Fas2/PUSH-TO-GITHUB.md` för instruktioner.

- **artifacts/**  
  Byggda tarballer, genererade PDF-rapporter (t.ex. buggrapporter) och äldre release-paket.

- **llm-wiki/** (ligger bredvid denna mapp)  
  Källan till all projektdokumentation (Karpathy-style wiki).  
  Aktiv utveckling sker här. Innehållet kopieras in i release-snapshots.

## Koppling till huvudprojektet

- Android-källkod utvecklas i `~/AndroidStudioProjects/RFIDManager` (standard för Android Studio).
- Dokumentation utvecklas i `~/llm-wiki`.
- Release-processen paketerar allt i `releases/2026-06-Fas2/` och pushar till GitHub.
- Taggen `fas-2-uat-godkand-2026-06-07` markerar Fas 2 UAT-godkännande av Kund (2026-06-07).

## Vanliga kommandon

```bash
# Starta MQTT testmiljö
cd ~/rfid-manager/test/fas2-mqtt/mqtt
# (se instruktioner i wiki/log.md eller App-Architecture.md)

# Kör setup för telefon
cd ~/rfid-manager/setup
./fix-usb-adb.sh

# Arbeta med aktuell release-snapshot
cd ~/rfid-manager/releases/2026-06-Fas2
git status
# ... redigera, commit, push enligt PUSH-TO-GITHUB.md
```

Uppdaterad 2026-06-07 efter Fas 2 UAT-godkännande.
>>>>>>> 64f3e2a (Initial commit: rfid-manager release & documentation repo)

# RFID Manager — Projektöversikt

> **Senaste milstolpe (2026-06-19):** Fas 200 — MQTT Realtidsdashboard godkänd av Kund.  
> Webbaserad dashboard med FastAPI + SSE, live-flöde och statistik för MQTT-meddelanden.

---

## Projektstatus

| Fas | Innehåll | Status |
|-----|----------|--------|
| **Fas 1–6** | NFC-läsning/skrivning, MQTT, navigation, i18n, dark mode, v1.0 release | ✅ Alla klara |
| **Fas-100** | MQTT-infrastruktur — fördjupning (pågående) | 🔄 ~30% |
| **Fas-101** | MQTT-klientkonfiguration i appen | 📋 Planerad |
| **Fas 200** | MQTT Realtidsdashboard — FastAPI, SSE, Docker Compose | ✅ Godkänd 2026-06-19 |

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

---

**GitHub:** https://github.com/JoaBerra/rfid-manager  
**Wiki-index:** [wiki/index.md](wiki/index.md)

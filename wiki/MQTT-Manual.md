---
title: "MQTT-manual — Praktisk bruksanvisning"
tags: [mqtt, manual, broker, dashboard, verktyg, demo]
created: 2026-06-19
---

# MQTT-manual — Praktisk bruksanvisning

> **Syfte:** Steg-för-steg för att starta MQTT-infrastrukturen, överföra data från telefonen och visualisera i realtidsdashboarden.

## Innehåll

1. [Starta brokern](#1-starta-brokern)
2. [Starta dashboarden](#2-starta-dashboarden)
3. [Skicka data från telefonen](#3-skicka-data-från-telefonen)
4. [Se data i dashboarden](#4-se-data-i-dashboarden)
5. [Felsökning](#5-felsökning)
6. [Kommandoreferens](#6-kommandoreferens)

---

## 1. Starta brokern

```bash
docker run -d --rm --name rfid-mqtt-test \
  -p 1883:1883 \
  -v ~/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf \
  eclipse-mosquitto \
  mosquitto -c /mosquitto/config/mosquitto.conf
```

**Verifiera:** `docker ps | grep rfid-mqtt-test` → containern ska vara `Up`.

**Loggar:** `docker logs rfid-mqtt-test`

---

## 2. Starta dashboarden

### Alternativ A: Docker Compose (rekommenderat för demo)

```bash
cd ~/rfid-manager/dashboard
docker compose up --build
```

Detta startar både Mosquitto och dashboarden. Öppna http://localhost:8000.

### Alternativ B: Manuellt (för utveckling)

```bash
cd ~/rfid-manager/dashboard
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Öppna http://localhost:8000. Förutsätter att Mosquitto redan är igång på localhost:1883.

---

## 3. Skicka data från telefonen

1. **Öppna RFID Manager** på Samsung Galaxy Note 10
2. **Scanna en tagg** — START SCAN → håll tagg mot telefonen
3. **Gå till PERSISTED-fliken** — taggens data finns sparad
4. **Tryck Transmit ↑** på önskad rad

Meddelandet publiceras på topic `rfidmanager/<uid>/telemetry`.

### Verifiera manuellt från PC

```bash
# Lyssna på alla rfidmanager-meddelanden
mosquitto_sub -h localhost -p 1883 -t "rfidmanager/#" -v

# Eller via Docker
docker exec rfid-mqtt-test mosquitto_sub -h localhost -p 1883 -t "rfidmanager/#" -v
```

Simulera ett meddelande:

```bash
docker exec rfid-mqtt-test mosquitto_pub -h localhost -p 1883 \
  -t "rfidmanager/047B05CA885884/telemetry" \
  -m '{"type":"ReadEscortMemory","uid":"047B05CA885884","timestamp":1780816291332,"source":"Manual write page 8","sparkplug":true,"data":{"memoryBank":3,"address":8,"length":4,"payload":"74657374"}}'
```

---

## 4. Se data i dashboarden

Öppna http://localhost:8000 (eller 8001) i webbläsaren.

### Vyer

| Vy | Beskrivning |
|----|-------------|
| **Live-flöde** | Varje inkommande meddelande dyker upp inom <1 sekund. Visa UID, typ, topic, tidsstämpel. |
| **Statistik** | Totalt meddelanden, unika UID:n, meddelanden/minut, driftid. |
| **Detaljvy** | Klicka på ett meddelande i live-flödet för att se tolkade fält och rå JSON. |
| **Anslutningsstatus** | Grön prick = ansluten till broker. Röd = frånkopplad. |

### Demo-flöde (steg-för-steg)

1. Starta broker + dashboard (steg 1–2 ovan)
2. Öppna dashboarden i webbläsaren på PC
3. Ta telefonen, scanna en RFID-tagg
4. Tryck Transmit i appen
5. Inom 1 sekund syns läsningen i dashboardens live-flöde + statistik uppdateras

---

## 5. Felsökning

| Symptom | Orsak | Lösning |
|---------|-------|---------|
| Dashboarden visar "Ej ansluten" | Broker inte igång | `docker start rfid-mqtt-test` eller starta om compose |
| Inga meddelanden i live-flöde | Telefonen når inte brokern | Kontrollera att båda är på samma nätverk. `adb reverse tcp:1883 tcp:1883` om via USB |
| Port 8000 upptagen | Annan process | Använd port 8001: `--port 8001` |
| Dashboarden svarar inte | Processen har dött | `ps aux \| grep uvicorn` och starta om |
| Broker-loggar visar "permission denied" | Fel i mosquitto.conf | Kontrollera sökvägar i config-filen |

### Verktyg för felsökning

```bash
# Testa att broker svarar på TCP
echo "" | nc -v localhost 1883

# Se alla MQTT-meddelanden i terminalen
docker exec rfid-mqtt-test mosquitto_sub -h localhost -p 1883 -t "#" -v

# Dashboard-loggar
cat /tmp/dashboard.log

# Se om dashboarden svarar
curl -s http://localhost:8000/api/stats
```

---

## 6. Kommandoreferens

### Broker

| Kommando | Beskrivning |
|----------|-------------|
| `docker run ... eclipse-mosquitto ...` | Starta broker |
| `docker start rfid-mqtt-test` | Starta stoppad broker (utan `--rm`) |
| `docker stop rfid-mqtt-test` | Stoppa broker |
| `docker logs rfid-mqtt-test` | Visa loggar |
| `docker logs -f rfid-mqtt-test` | Följ loggar i realtid |

### Dashboard

| Kommando | Beskrivning |
|----------|-------------|
| `docker compose up --build` | Starta dashboard + broker |
| `uvicorn app.main:app --port 8000` | Starta dashboard (dev) |
| `curl http://localhost:8000/api/stats` | Verifiera att dashboarden svarar |

### MQTT-klienter

| Kommando | Beskrivning |
|----------|-------------|
| `mosquitto_sub -h localhost -t "rfidmanager/#" -v` | Lyssna på alla topics |
| `mosquitto_pub -h localhost -t "test" -m "hello"` | Publicera testmeddelande |
| `docker exec rfid-mqtt-test mosquitto_sub ...` | Kör sub i Docker-containern |
| `MQTT Explorer` | GUI-verktyg (se [[MQTT-Explorer]]) |

### Testverktyg

| Sökväg | Beskrivning |
|--------|-------------|
| `~/rfid-manager/test/fas2-mqtt/mqtt/test_subscriber_persist.py` | Python-subscriber med SQLite-persistens |
| `~/rfid-manager/test/fas2-mqtt/mqtt/simulate_mobile_publish.py` | Simulera app-publicering |
| `~/rfid-manager/dashboard/` | Webbdashboard (FastAPI + SSE) |
| `~/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf` | Broker-konfiguration |

---

## Se även

- [[MQTT-Infrastruktur]] — Fördjupning i MQTT-protokollet
- [[Fas-200-Web-Dashboard]] — Dashboardens arkitektur
- [[MQTT-Explorer]] — GUI-verktyg för MQTT
- [[Fas-101-MQTT-Configuration]] — MQTT-inställningar i appen
- [[Testplan]] — Testfall för MQTT

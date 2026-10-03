---
title: "Startup Procedure — Kallstart till alla tjänster uppe"
tags: [drift, startup, broker, dashboard, mqtt, mcp, ops]
created: 2026-06-23
---

# Startup Procedure — Kallstart till alla tjänster uppe

> **Historiskt dokument (falstaff, juni 2026):** Beskriver kallstart på den tidigare falstaff-miljön med en **anonym** broker (`docker run … eclipse-mosquitto:2`). Falstaff är avvecklad ([[Fas-D-falstaff-avveckling]]). Gällande drift: broker och dashboard på ishtar enligt [[Testmiljo-hulda]], **med inloggning och ACL sedan 2026-10-03** (README, *Nätverk och säkerhet (MQTT)*). `mosquitto_pub`/`-sub` utan användare nekas där.

> **Syfte:** Steg-för-steg för att få upp hela utvecklingsmiljön från kallstart (datorn avstängd → alla tjänster igång).
>
> Varje objekt verifieras innan nästa påbörjas. Dokumentation uppdateras om fel upptäcks.

## Förutsättningar

- Ubuntu 24.04 (falstaff)
- Docker installerad (`docker.io` + `docker-compose-v2`)
- Användaren `joakim` i `docker`-gruppen (för att köra Docker utan sudo)
- Monorepo: `~/projects/rfid/rfid-manager/`

---

## Objekt 1: MQTT Broker (Eclipse Mosquitto)

**Verifierad:** 2026-06-23 ✅

### Starta

```bash
docker run -d --name rfid-mqtt \
  -p 1883:1883 \
  -p 9001:9001 \
  eclipse-mosquitto:2
```

### Verifiera

```bash
# 1. Kör containern?
docker ps | grep rfid-mqtt
# → Förväntat: "Up" status

# 2. Lyssnar på port 1883?
ss -tlnp | grep 1883
# → Förväntat: LISTEN 0.0.0.0:1883

# 3. Publicera + prenumerera (end-to-end)
docker exec rfid-mqtt mosquitto_pub -h localhost \
  -t "rfidmanager/test/telemetry" \
  -m '{"status":"broker live"}'
# → Förväntat: inget fel, exit 0

# 4. Loggar
docker logs --tail 5 rfid-mqtt
# → Förväntat: "New connection from ... on port 1883"
```

### Om containern redan finns (efter omstart)

```bash
docker start rfid-mqtt
```

### Om containern måste återskapas

```bash
docker rm -f rfid-mqtt
# Kör sedan start-kommandot ovan
```

### Fel som åtgärdades under verifiering

| Fel | Orsak | Lösning |
|-----|-------|---------|
| `mosquitto.conf` var en tom katalog | Docker skapade katalogen vid mount av icke-existerande fil | Använde default-konfig (utan custom mount) |

### Länkar

- [[MQTT-Manual]] — Fullständig bruksanvisning
- [[MQTT-Infrastruktur]] — Protokoll och arkitektur
- [[Testplan]] — Testfall för MQTT

---

## Objekt 2: Python Subscriber (test_subscriber_persist.py)

**Verifierad:** 2026-06-23 ✅

Lyssnar på MQTT-meddelanden på `rfidmanager/+/telemetry` och sparar till SQLite.

### Förutsättningar

- MQTT Broker igång (Objekt 1)
- Python .venv skapat: `test/fas2-mqtt/.venv/`
- Data-katalog: `~/projects/rfid/rfid-manager/data/`

### Starta

```bash
cd ~/projects/rfid/rfid-manager
test/fas2-mqtt/.venv/bin/python test/fas2-mqtt/mqtt/test_subscriber_persist.py
```

För att filtrera på specifik UID:

```bash
test/fas2-mqtt/.venv/bin/python test/fas2-mqtt/mqtt/test_subscriber_persist.py --uid 047B05CA885884
```

### Verifiera

```bash
# 1. Starta subscribern (se ovan)
# 2. Skicka ett testmeddelande från en annan terminal:
docker exec rfid-mqtt mosquitto_pub -h localhost \
  -t "rfidmanager/047B05CA885884/telemetry" \
  -m '{"type":"ReadEscortMemory","uid":"047B05CA885884","timestamp":1780816291332,"source":"Verifiering","sparkplug":true,"data":{"memoryBank":3,"address":12,"length":4,"payload":"74657374"}}'

# 3. I subscriber-terminalen ska du se:
#    ● INFO  Database ready: .../data/rfid_readings.db
#    ✔ OK   Connected
#    ● INFO  Subscribed to rfidmanager/+/telemetry
#    ⬡ RECV ReadEscortMemory  uid=047B05CA885884  topic=...
#    ✔ OK   Persisted uid=047B05CA885884  type=ReadEscortMemory

# 4. Kontrollera databasen:
sqlite3 data/rfid_readings.db "SELECT uid, type, source, received_at FROM readings ORDER BY id DESC LIMIT 5;"

# 5. Avsluta med Ctrl+C → session summary visas
```

### Fel som åtgärdades under verifiering

| Fel | Orsak | Lösning |
|-----|-------|---------|
| Shebang pekade på `/home/joakim/rfid-manager/...` | Gammal sökväg från sixten | Uppdaterad till `~/projects/rfid/rfid-manager/...` |
| `DB_DIR = Path.home() / "rfid-manager" / "data"` | Gammal sökväg från sixten | Uppdaterad till `Path.home() / "projects" / "rfid" / "rfid-manager" / "data"` |
| Inget `.venv` fanns | .venv skapades inte under flytten | `python3 -m venv .venv && .venv/bin/pip install paho-mqtt` |

### Länkar

- [[MQTT-Manual]] — Fullständig bruksanvisning (inkl. subscriber-kommando)
- [[MQTT-Infrastruktur]] — Protokoll och arkitektur

--- 

## Objekt 3: Webbdashboard (FastAPI + SSE)

**Verifierad:** 2026-06-23 ✅

Dark-theme webbdashboard som prenumererar på MQTT och visar live-flöde via SSE (Server-Sent Events).

### Arkitektur

```
MQTT Broker (1883) → dashboard (paho-mqtt, in-memory)
                          → REST API /api/stats, /api/messages
                          → SSE /api/events → webbläsare (index.html)
```

Ett meddelande parkopplas med sin fulla JSON och realtidsdata — all statistik är in-memory.

### Förutsättningar

- Python .venv skapat: `dashboard/.venv/` (fastapi, uvicorn, paho-mqtt)
- MQTT Broker igång (Objekt 1)

### Starta

```bash
cd ~/projects/rfid/rfid-manager/dashboard
.venv/bin/python -m app.main
```

Öppna i webbläsaren: [http://localhost:8000](http://localhost:8000)

Anslutning visas i headern (grön prick = ansluten). Live-flöde visas omedelbart.

### Verifiera

```bash
# 1. Starta dashboard (se ovan)
# 2. Skicka ett MQTT-meddelande från annan terminal:
docker exec rfid-mqtt mosquitto_pub -h localhost \
  -t "rfidmanager/047B05CA885884/telemetry" \
  -m '{"type":"ReadEscortMemory","uid":"047B05CA885884","timestamp":1780816291332,"source":"Verifiering","sparkplug":true,"data":{"memoryBank":3,"address":12,"length":4,"payload":"74657374"}}'

# 3. Via API (utan GUI):
curl http://localhost:8000/api/stats     # total, unique_uids, connected
curl http://localhost:8000/api/messages  # senaste 50 meddelandena
```

### Installation (första gången)

```bash
cd ~/projects/rfid/rfid-manager/dashboard
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

### Fel som åtgärdades under verifiering

| Fel | Orsak | Lösning |
|-----|-------|---------|
| Inget `.venv` fanns | .venv skapades inte under flytten | `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt` |

### Länkar

- [[MQTT-Manual]] — Fullständig bruksanvisning

---

## Objekt 4: MQTT Explorer (fakir)

**Aktiv setup:** 2026-07-12 (Fas D)

GUI-klient på fakir — ansluter till ishtar `192.168.50.151:1883`. Ingen bakgrundstjänst.

```bash
mqtt-explorer    # ~/.local/bin, AppImage v0.3.5
```

Se [[MQTT-Explorer]] och `setup/install-mqtt-explorer-fakir.sh`.

---

## Objekt 4b: MCP Server — parkerad

**Historik:** Verifierad 2026-06-23 på falstaff/OpenCode. **Ej** del av aktiv drift 2026-07-12.

MCP flyttad till idé i Andra Hjärnan: [2026-07-12-rfid-manager-mcp-server](https://github.com/JoaBerra/andra-hjarna/blob/main/Ideer/2026-07-12-rfid-manager-mcp-server.md). Kod: `mcp-server/`.

---

## Objekt 5: Android App (rfid-manager-android)

**Verifierad:** 2026-06-23 ✅

Android-appen publicerar NFC-avläsningar till MQTT-brokern via Eclipse Paho 1.2.5.

### Arkitektur

```
NFC-avläsning → Room DB (persist; Room är enda lagring på `feature/sqlite`, JSON-fil i v1.0.1) → MqttSender → publish rfidmanager/<uid>/telemetry → MQTT Broker
```

Appen ansluter till brokern som MQTT-klient (`rfid-android-client`; sedan 2026-10-03 inloggad som `rfid-app`), publicerar med QoS 1. Den prenumererar inte på några topics (endast publikation).

### Konfiguration

| Parameter | Värde | Fil |
|-----------|-------|-----|
| Broker IP | `192.168.50.151` (ishtar testlab) | `MqttConnectionManager.kt:13`, `MainActivity.kt:75` |
| Port | `1883` | `MqttConnectionManager.kt` |
| Client ID | `rfid-android-client` | `MqttConnectionManager.kt:15` |
| Topic | `rfidmanager/<uid>/telemetry` | `MqttSender.kt:54` |
| QoS | 1 | `MqttSender.kt:52` |

### Nätverkssäkerhet

Android 9+ kräver `network_security_config.xml` för cleartext TCP. Domän `192.168.50.151` (ishtar) är vitlistad. Legacy falstaff avvecklad — [[Fas-D-falstaff-avveckling]].

### Förutsättningar

- Android 9+ (API 28+)
- Appen installerad på enhet med NFC
- MQTT Broker igång (se Objekt 1)

### Verifiering (utan fysisk enhet)

Källkoden är verifierad och default-IP är `192.168.50.151` (ishtar, Uppdrag 004). Tidigare: `.128` (sixten) → `.107` (falstaff). För full E2E-test krävs en fysisk Android-enhet med appen installerad.

### Fel som åtgärdades under verifiering

| Fel | Orsak | Lösning |
|-----|-------|---------|
| Broker IP `192.168.50.128` (sixten) | Gammal IP från utvecklingsmiljön | Uppdaterad till `192.168.50.107` (falstaff) i 5 filer |
| `network_security_config.xml` hade gammal IP | Glömdes vid flytt | IP uppdaterad, cleartext tillåts för rätt domän |

### Länkar

- `rfid-manager-android/` — Appens git-repo
- [[MQTT-Infrastruktur]] — Protokoll och arkitektur

---

## Objekt 6: Git-synk (push till GitHub)

**Status:** ⏳ Inte påbörjad

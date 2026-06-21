---
title: "Fas 200 — MQTT Realtidsdashboard"
tags: [fas-200, mqtt, dashboard, web, fastapi, sse, docker, demo, infrastruktur]
created: 2026-06-19
---

# Fas 200 — MQTT Realtidsdashboard

> **Status:** Implementerad ✅
> **Godkänd av:** Projektledare (plan) 2026-06-19

## Mål

En webbaserad dashboard som visualiserar MQTT-meddelanden i realtid. Dashboarden är en naturlig, förvaltad del av utvecklingsmiljön — ingen one-off.

## Arkitektur

```
Telefon (RFID Manager App)
  │  publish rfidmanager/<uid>/telemetry
  ▼
Mosquitto Broker (:1883)
  │  distributera till prenumeranter
  ▼
Dashboard (FastAPI + paho-mqtt)
  │  SSE (Server-Sent Events)
  ▼
Webbläsare (HTML/CSS/JS)
  └─ Live-flöde med varje nytt meddelande
  └─ Statistik (totalt, unika UID:n, driftid)
  └─ Detaljvy (klicka på meddelande)
```

### Tech stack

| Lager | Val | Anledning |
|-------|-----|-----------|
| Backend | FastAPI + paho-mqtt | Python (återanvänder befintlig venv), inbyggt SSE-stöd via `StreamingResponse` |
| Frontend | Ren HTML/CSS/JS | Inga npm-beroenden, en fil att underhålla |
| Server | Uvicorn | ASGI-server, kompatibel med FastAPI |
| Infra | Docker Compose | Dashboard + Mosquitto i samma nätverk |
| MQTT | paho-mqtt 2.x | Befintligt bibliotek i testmiljön |

### Dataflöde

1. MQTT-klient (paho) ansluter till broker på `rfidmanager/+/telemetry`
2. Varje inkommande meddelande tolkas (JSON) och läggs i en trådsäker kö + deque (max 200)
3. SSE-endpointen (`/api/events`) läser från kön och skickar till webbläsaren
4. Webbläsaren uppdaterar live-flödet och statistiken via JavaScript `EventSource`

## Acceptanskriterier

### ✅ 1. Infrastruktur
- [x] Dashboard startas med `docker compose up` tillsammans med Mosquitto
- [x] Finns i `~/projects/rfid/rfid-manager/dashboard/` — sida vid sida med testinfrastrukturen
- [x] Konfigurerbar via miljövariabler (`MQTT_BROKER`, `MQTT_PORT`, `MQTT_TOPIC`)
- [x] Wiki-dokumentation finns (denna sida)

### ✅ 2. Realtidsflöde
- [x] Prenumererar på `rfidmanager/+/telemetry`
- [x] SSE levererar meddelanden inom <1 sekund
- [x] Live-flöde visar senaste 50 meddelandena med slide-in-animation

### ✅ 3. Dashboard-vyer
- [x] **Live-flöde:** scrollbar lista med UID, typ, topic, tidsstämpel, data-preview
- [x] **Statistik:** totalt meddelanden, unika UID:n, meddelanden/minut, driftid
- [x] **Detaljvy:** klicka på meddelande → expanded vy med tolkade fält + rå JSON
- [x] **Anslutningsstatus:** grön/röd indikator för MQTT-anslutning

### ✅ 4. Demo-redo
- [x] Fungerar på PC med storbildskärm/demo
- [x] Responsiv design (fungerar ner till mobilbredd)
- [x] Mörkt tema som matchar appens industriella estetik (#00FF88 accent)

### ✅ 5. Förvaltning
- [x] Wiki-sida med arkitektur, API-endpoints, uppstart, troubleshooting
- [x] Kanban-kort på boarden
- [x] Loggad i log.md

## API-endpoints

| Metod | Path | Beskrivning |
|-------|------|-------------|
| GET | `/` | Dashboard (HTML) |
| GET | `/api/stats` | Statistik (total, unique_uids, connected, uptime) |
| GET | `/api/messages?limit=50` | Senaste N meddelanden |
| GET | `/api/events` | SSE-ström för realtidsuppdateringar |

## Filer och struktur

```
~/projects/rfid/rfid-manager/dashboard/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI-app, routes, SSE
│   └── mqtt_client.py       # MQTT-klient, thread-safe message buffer
├── static/
│   └── index.html           # Frontend (allt i en fil)
├── Dockerfile               # Python 3.12-slim
├── docker-compose.yml       # dashboard + mosquitto
├── requirements.txt         # fastapi, uvicorn, paho-mqtt
└── .venv/                   # Virtuell miljö (för lokal utveckling)
```

## Startinstruktioner

### Med Docker Compose (rekommenderat för demo)

```bash
cd ~/projects/rfid/rfid-manager/dashboard
docker compose up --build
```

Öppna http://localhost:8000 i webbläsaren.

### Lokal utveckling (utan Docker)

```bash
cd ~/projects/rfid/rfid-manager/dashboard

# Skapa och aktivera venv (görs en gång)
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Starta (kräver Mosquitto på localhost:1883)
MQTT_BROKER=localhost .venv/bin/python -m app.main
```

## Felsökning

| Problem | Orsak | Lösning |
|---------|-------|---------|
| Dashboard visar "Ej ansluten" | Broker inte igång | `docker ps \| grep rfid-mqtt-test` eller starta compose |
| Inga meddelanden i flödet | Fel topic eller broker-adress | Kontrollera `MQTT_TOPIC` och `MQTT_BROKER` |
| SSE kopplar från | Nätverksproblem | Webbläsaren återansluter automatiskt inom 3 sekunder |
| Port 8000 upptagen | Annan process | Ändra port via `docker-compose.yml` eller `--port` |

## MCP-server (Fas-300)

Dashboardens API exponeras via en MCP-server som ger AI-assistenten direkt tillgång till live-data.

**Plats:** `~/projects/rfid/rfid-manager/mcp-server/`

### Verktyg

| MCP Tool | Dashboard-anrop | Beskrivning |
|----------|----------------|-------------|
| `get_stats` | `GET /api/stats` | Statistik (totalt, unika UID, anslutning, per_minute, driftid) |
| `get_messages(limit)` | `GET /api/messages` | Senaste N meddelanden |
| `publish_mqtt(topic, payload)` | direkt mot MQTT-broker | Publicera MQTT-meddelande |
| `get_live_events` | prenumerera på `rfidmanager/+/telemetry` | Lyssna på nya meddelanden i 5 sekunder |

### Arkitektur

```
AI-assistent
  │  MCP-protokoll (stdin/stdout)
  ▼
MCP-server (mcp-server/server.py)
  │  HTTP GET ───→ Dashboard API (port 8001)
  │  MQTT pub/sub ───→ Mosquitto Broker (port 1883)
  ▼
Dashboard + Broker
```

### Starta

```bash
cd ~/projects/rfid/rfid-manager/mcp-server
../dashboard/.venv/bin/python server.py
```

MCP-servern ansluter automatiskt till dashboarden på `DASHBOARD_URL` (default `http://localhost:8001`) samt till MQTT-brokern.

### OpenCode-konfiguration

För att använda MCP-servern i opencode, lägg till i `~/.config/opencode/opencode.json`:

```json
{
  "mcpServers": {
    "rfid-manager": {
      "command": "/home/joakim/projects/rfid/rfid-manager/dashboard/.venv/bin/python",
      "args": ["/home/joakim/projects/rfid/rfid-manager/mcp-server/server.py"],
      "env": {
        "DASHBOARD_URL": "http://localhost:8001",
        "MQTT_BROKER": "localhost",
        "MQTT_PORT": "1883"
      }
    }
  }
}
```

### Relation till andra komponenter

- [[MQTT-Infrastruktur]] — MQTT-protokollet, broker-setup, topologi
- [[MQTT-Explorer]] — GUI-verktyg för MQTT (komplement till dashboarden)
- [[Fas-101-MQTT-Configuration]] — MQTT-klientkonfiguration i Android-appen
- [[Produkt-Roadmap]] — Övergripande roadmap
- [[Kanban]] — Aktuell status
- [[Rollfördelning-och-Arbetsätt]] — Roller och ansvar

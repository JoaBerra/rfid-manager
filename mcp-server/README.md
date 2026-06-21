# RFID Manager MCP-server

MCP-server som exponerar RFID-dashboardens API och MQTT-broker som verktyg för AI-assistenten.

## Verktyg

| Tool | Beskrivning |
|------|-------------|
| `get_stats` | Hämta dashboard-statistik (totalt, unika UID, anslutning) |
| `get_messages(limit)` | Hämta senaste N meddelanden |
| `publish_mqtt(topic, payload)` | Publicera MQTT-meddelande direkt till brokern |
| `get_live_events` | Hämta nya realtidshändelser från SSE-strömmen |

## Starta

```bash
cd ~/rfid-manager/mcp-server
../dashboard/.venv/bin/python server.py
```

## Konfiguration (miljövariabler)

| Variabel | Default | Beskrivning |
|----------|---------|-------------|
| `DASHBOARD_URL` | `http://localhost:8001` | URL till dashboard |
| `MQTT_BROKER` | `localhost` | MQTT broker host |
| `MQTT_PORT` | `1883` | MQTT broker port |

## Användning med opencode

Lägg till följande i `~/.config/opencode/opencode.json`:

```json
{
  "mcpServers": {
    "rfid-manager": {
      "command": "/home/joakim/rfid-manager/dashboard/.venv/bin/python",
      "args": ["/home/joakim/rfid-manager/mcp-server/server.py"]
    }
  }
}
```

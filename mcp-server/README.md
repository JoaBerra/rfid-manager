# RFID Manager MCP-server

> **Parkerad (2026-07-12).** Ej del av aktiv drift. Se [AH-idé](https://github.com/JoaBerra/andra-hjarna/blob/main/Ideer/2026-07-12-rfid-manager-mcp-server.md).

MCP-server som exponerar dashboard-API och MQTT som verktyg för AI-agenter. Kod behålls som referens.

## Verktyg

| Tool | Beskrivning |
|------|-------------|
| `get_stats` | Dashboard `/api/stats` |
| `get_messages(limit)` | Dashboard `/api/messages` |
| `publish_mqtt(topic, payload)` | Direkt mot broker |
| `get_live_events` | MQTT-prenumeration 5 s |

## Om aktiverad mot ishtar

```bash
export DASHBOARD_URL=http://192.168.50.151:8000
export MQTT_BROKER=192.168.50.151
export MQTT_PORT=1883
```

Aktiv testmiljö idag: [[MQTT-Explorer]] + dashboard på ishtar (wiki).
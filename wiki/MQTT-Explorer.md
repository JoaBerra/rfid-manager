---
title: MQTT Explorer — PC-verktyg för transaktionsinspektion
tags: [pc, mqtt, verktyg, test, fakir, ishtar]
created: 2026-06-10
updated: 2026-07-12
---

# MQTT Explorer

Gratis GUI-klient för MQTT — körs **på fakir vid behov**, ansluter till brokern på **ishtar**.

## Installation (fakir)

**Status 2026-07-12:** Installerad via AppImage v0.3.5.

| Fält | Värde |
|------|-------|
| Binär | `~/.local/opt/mqtt-explorer/MQTT-Explorer-0.3.5.AppImage` |
| Kommando | `mqtt-explorer` (symlink i `~/.local/bin`) |
| Skript | `setup/install-mqtt-explorer-fakir.sh` (återinstallera) |

```bash
mqtt-explorer
```

Kräver inga bakgrundstjänster — stäng appen när du inte debuggar (vänligt mot inferens på fakir).

## Anslutning

| Fält | Värde |
|------|-------|
| Host | `192.168.50.151` (ishtar) |
| Port | `1883` |
| Auth | Ingen (dev) |
| Topic | `rfidmanager/#` |

Legacy falstaff: `192.168.50.107` (avvecklas).

## Arbetsflöde med RFID Manager

1. Broker igång på ishtar — se [[Testmiljo-hulda]]
2. Starta `mqtt-explorer` på fakir
3. Anslut till `192.168.50.151:1883`
4. Skicka från appen (Transmit) eller publicera i Explorer
5. Jämför med webb-dashboard: `http://192.168.50.151:8000`

Alternativ CLI-test:

```bash
docker run --rm eclipse-mosquitto:2 mosquitto_pub \
  -h 192.168.50.151 -p 1883 -t rfidmanager/test/telemetry -m '{"type":"test"}'
```

## Verktygsjämförelse

| Aspekt | MQTT Explorer (fakir) | Dashboard (ishtar) | Python subscriber (ishtar) |
|--------|----------------------|--------------------|---------------------------|
| Plats | Lokal GUI-klient | Webb :8000 | SSH/tmux |
| Bäst för | Topic-träd, publish-test | Demo, statistik, SSE | Loggning, SQLite |
| Belastar fakir | Bara när öppen | Nej (webbläsare) | Nej |

## MCP-server

AI-agent-åtkomst via MCP är **parkerad** — se [AH-idé](https://github.com/JoaBerra/andra-hjarna/blob/main/Ideer/2026-07-12-rfid-manager-mcp-server.md). Aktiv drift: Explorer + dashboard räcker.

## Länkar

- [Releases](https://github.com/thomasnordquist/MQTT-Explorer/releases)
- [[Testmiljo-hulda]] — broker + dashboard
- [[Fas-200-Web-Dashboard]] — webb-UI
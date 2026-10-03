---
title: MQTT Explorer — PC-verktyg för transaktionsinspektion
tags: [pc, mqtt, verktyg, test, fakir, ishtar]
created: 2026-06-10
updated: 2026-07-12
---

# MQTT Explorer

Gratis GUI-klient för MQTT — körs **på fakir vid behov**, ansluter till brokern på **ishtar**.

## Installation (fakir)

**Status 2026-07-12:** Installerad (extraherad AppImage v0.3.5). E2E verifierad — Principal: läsning synlig mot ishtar `.151`.

| Fält | Värde |
|------|-------|
| Launcher | `~/.local/opt/mqtt-explorer/mqtt-explorer-launcher.sh` |
| Extraherad | `~/.local/opt/mqtt-explorer/squashfs-root/` |
| Kommando | `mqtt-explorer` (symlink i `~/.local/bin`) |
| Skript | `setup/install-mqtt-explorer-fakir.sh` |

```bash
mqtt-explorer
```

**Arch/Omarchy:** AppImage kräver `fuse2` om den körs direkt. Vi använder **extraherad** kopia (ingen FUSE, inget sudo). Valfritt: `sudo pacman -S fuse2` om du föredrar AppImage-raden.

Kräver inga bakgrundstjänster — stäng appen när du inte debuggar.

## Anslutning

| Fält | Värde |
|------|-------|
| Host | `192.168.50.151` (ishtar) |
| Port | `1883` |
| Auth | Användarnamn/lösenord krävs sedan 2026-10-03 (`rfid-dashboard`, läsrätt) *(historiskt: ingen, anonym dev-broker)* — ännu inte uppsatt i Explorer (teknisk skuld) |
| Topic | `rfidmanager/+/telemetry` (ACL ger läsrätt bara där; `rfidmanager/#` visar inget mer) |

Legacy brokers (sixten `.128`, falstaff `.107`) avvecklade — se [[Fas-D-falstaff-avveckling]].

## Arbetsflöde med RFID Manager

1. Broker igång på ishtar — se [[Testmiljo-hulda]]
2. Starta `mqtt-explorer` på fakir
3. Anslut till `192.168.50.151:1883`
4. Skicka från appen (Transmit) eller publicera i Explorer
5. Jämför med webb-dashboard: `http://192.168.50.151:8000`

Alternativ CLI-test:

```bash
# Historiskt (anonym broker) — nekas sedan 2026-10-03; kräver -u/-P med skrivrätt (rfid-app):
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
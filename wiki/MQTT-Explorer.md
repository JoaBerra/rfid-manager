---
title: MQTT Explorer — PC-verktyg för transaktionsinspektion
tags: [pc, mqtt, verktyg, test, transaktionslogg]
created: 2026-06-10
---

# MQTT Explorer

> **Gratis GUI-verktyg** för att inspektera MQTT-meddelanden i realtid.  
> Rekommenderas som PC-stöd för transaktionsinspektion under utveckling och test.

## Varför MQTT Explorer?

Istället för att enbart läsa rå JSON i terminalen via Python-subscribern ger MQTT Explorer dig:

- **Trädstruktur** över topics — se alla meddelanden sorterade per topic
- **Färgkodade meddelanden** — lätt att skilja på olika typer
- **Historik** — spara och bläddra i tidigare meddelanden
- **Publish-stöd** — skicka testmeddelanden direkt från GUI:t
- **Cross-platform** — Linux, Windows, macOS

## Installation

1. Gå till [MQTT Explorer GitHub releases](https://github.com/thomasnordquist/MQTT-Explorer/releases)
2. Ladda ner rätt version för ditt OS:
   - **Linux**: `.AppImage` (kräver ingen installation — kör direkt)
   - **Windows**: `.exe`-installerare
   - **macOS**: `.dmg`
3. Gör `.AppImage` körbar vid behov: `chmod +x MQTT-Explorer-*.AppImage`

## Anslut till testmiljön

| Fält | Värde |
|---|---|
| Host | `192.168.50.128` (eller `localhost` om subscribern körs på samma maskin som broker) |
| Port | `1883` |
| Anslutning | Ingen autentisering / TLS (okrypterat — endast för utveckling) |

Efter anslutning ser du i realtid alla meddelanden som publiceras på `rfidmanager/#`.

## Användning med RFID Manager

1. Starta Docker-brokern (om inte redan igång):
   ```bash
   docker run -d --rm --name rfid-mqtt-test -p 1883:1883 \
     -v ~/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf \
     eclipse-mosquitto mosquitto -c /mosquitto/config/mosquitto.conf
   ```

2. Starta MQTT Explorer och anslut till brokern.

3. Prenumerera på `rfidmanager/#` i MQTT Explorer.

4. Skicka ett testmeddelande från appen (Transmit ↑) eller från terminalen:
   ```bash
   cd ~/rfid-manager/test/fas2-mqtt/mqtt
   ./.venv/bin/python simulate_mobile_publish.py
   ```

5. Meddelandet syns omedelbart i MQTT Explorer — med full JSON, topic, och tidsstämpel.

## Fördelar jämfört med terminal-subscribern

| Aspekt | Python subscriber | MQTT Explorer |
|---|---|---|
| Realtidsöversikt | Scrollande text | Trädstruktur + färger |
| Filtrering | `--uid`-flagga | Klicka på topic |
| Historik | SQLite-fil | Inbyggd sessionhistorik |
| Publish från PC | `simulate_mobile_publish.py` | Inbyggd "Publish"-flik |
| Installation | Kräver Python + venv | `.AppImage`, ingen setup |
| Bäst för | Automation, loggning | Visuell inspektion, debugging |

## Länkar

- [MQTT Explorer GitHub](https://github.com/thomasnordquist/MQTT-Explorer)
- [Releases](https://github.com/thomasnordquist/MQTT-Explorer/releases)

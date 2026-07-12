---
title: Testmiljö — hulda
tags: [hulda, testmiljo, mqtt, fas-d, infrastruktur]
created: 2026-07-12
updated: 2026-07-12
---

# Testmiljö — hulda

Permanent RFID/MQTT-testlabb i garaget. Del av **Fas D** ([Uppdrag 003](https://github.com/JoaBerra/andra-hjarna/blob/main/Bearbetning/2026-07-12-fas-d-hulda-testmiljo-uppdrag.md)).

## Host

| Fält | Värde |
|------|-------|
| **Hostname** | hulda |
| **IP** | `192.168.50.100` |
| **Plats** | Garage (alltid igång) |
| **Roll** | MQTT-broker, Python-subscriber |
| **Admin från** | fakir via SSH |

**Legacy broker:** falstaff `192.168.50.107` — appens default tills Uppdrag 004 (D.4).

## D.1 — SSH från fakir

Engångs-setup (Principal, interaktivt lösenord):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_hulda -N ""
ssh-copy-id -i ~/.ssh/id_ed25519_hulda.pub joakim@192.168.50.100
```

`~/.ssh/config` på fakir (ej i git):

```
Host hulda
  HostName 192.168.50.100
  User joakim
  IdentityFile ~/.ssh/id_ed25519_hulda
  IdentitiesOnly yes
```

Verifiering:

```bash
ssh hulda true && echo "SSH OK"
```

### Blockerare (2026-07-12)

| Problem | Status |
|---------|--------|
| hulda pingbar på `192.168.50.100` | OK |
| SSH port 22 öppen | OK |
| Nyckelbaserad inloggning | **Väntar på `ssh-copy-id`** (lösenord krävs) |

## D.2 — MQTT-broker (Docker Compose)

Källa: `test/fas2-mqtt/docker-compose.hulda.yml`

### Förutsättningar på hulda

- Docker + `docker compose`
- Repo klonat: `~/Projects/rfid-manager/` (eller synkat från fakir)

```bash
# På hulda (första gången)
git clone https://github.com/JoaBerra/rfid-manager.git ~/Projects/rfid-manager
```

### Starta broker

```bash
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml up -d'
```

### Stoppa / omstart

```bash
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml down'
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml restart'
```

### Loggar

```bash
ssh hulda 'docker logs rfid-mqtt-hulda --tail 50'
```

### Verifiering från fakir

```bash
# Port öppen
python3 -c "import socket; s=socket.socket(); s.settimeout(3); print(s.connect_ex(('192.168.50.100',1883)))"

# Publicera testmeddelande (kräver mosquitto-clients eller Docker)
docker run --rm eclipse-mosquitto:2 mosquitto_pub -h 192.168.50.100 -p 1883 -t test/uppdrag003 -m ok

# Broker lyssnar på hulda
ssh hulda "ss -tlnp | grep 1883"
```

Compose-filen använder `restart: unless-stopped` och en namngiven volym (`mosquitto-data`) — containern överlever omstart.

### Valfri systemd (boot)

Kopiera `test/fas2-mqtt/systemd/rfid-mqtt.service` till hulda:

```bash
scp test/fas2-mqtt/systemd/rfid-mqtt.service hulda:/tmp/
ssh hulda 'sudo cp /tmp/rfid-mqtt.service /etc/systemd/system/ && sudo systemctl daemon-reload && sudo systemctl enable --now rfid-mqtt'
```

## Python-subscriber

På hulda, i `test/fas2-mqtt/mqtt/`:

```bash
ssh hulda
cd ~/Projects/rfid-manager/test/fas2-mqtt/mqtt
python3 -m venv .venv
source .venv/bin/activate
pip install paho-mqtt
python test_subscriber_persist.py
```

Kör i `tmux`/`screen` eller via systemd för persistent drift. Subscriber ansluter till `localhost:1883` på hulda.

## Felsökning

| Symptom | Åtgärd |
|---------|--------|
| `ssh hulda` nekas | Kör `ssh-copy-id` igen; kontrollera `~/.ssh/config` |
| Port 1883 stängd | `docker compose -f docker-compose.hulda.yml up -d` på hulda |
| Telefon når inte broker | Kontrollera Wi-Fi (samma LAN), brandvägg på hulda |
| App visar DISCONNECTED | App pekar fortfarande på falstaff — ändra broker-IP i Settings eller vänta på Uppdrag 004 |

## Relaterat

- [[MQTT-Infrastruktur]] — broker-konfiguration (`mosquitto.conf`)
- [[Utvecklingsmiljö-fakir]] — fakir bygger, hulda testar
- AH: [fas-d-testmiljo-hostar](https://github.com/JoaBerra/andra-hjarna/blob/main/Organisation/Processer/fas-d-testmiljo-hostar.md)
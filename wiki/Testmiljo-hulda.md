---
title: Testmiljö — hulda (Proxmox)
tags: [hulda, proxmox, testmiljo, mqtt, fas-d, infrastruktur]
created: 2026-07-12
updated: 2026-07-12
---

# Testmiljö — hulda (Proxmox)

Permanent RFID/MQTT-testlabb i garaget. Del av **Fas D** ([Uppdrag 003](https://github.com/JoaBerra/andra-hjarna/blob/main/Bearbetning/2026-07-12-fas-d-hulda-testmiljo-uppdrag.md)).

## Arkitektur — två lager

| Lager | IP / åtkomst | Roll |
|-------|--------------|------|
| **Hypervisor** | `192.168.50.100`, Proxmox `:8006` | VM/LXC-drift (root UI) |
| **Testlab-gäst** | TBD i `192.168.50.0/24` | MQTT, subscriber, Docker |

`192.168.50.100` är **inte** MQTT-värden — tjänsterna körs i en vald VM eller LXC.

**Legacy broker:** falstaff `192.168.50.107` — appens default tills Uppdrag 004 (D.4).

## D.0 — Proxmox och gäst-val (Principal)

1. Öppna `http://192.168.50.100:8006/` — logga in som root
2. Inventera VM och LXC (kända: Debian, Slackware, Ubuntu, MariaDB, …)
3. Välj **en gäst** som testlab (rekommendation: Ubuntu eller Debian)
4. Sätt **statisk IP** på gästen (eller DHCP-reservation i router)
5. Fyll i beslut i AH-uppdraget eller tabellen nedan

### Gäst-inventering

| Namn | Typ | OS | IP | Status | Testlab? |
|------|-----|-----|-----|--------|----------|
| *(fylls i Proxmox UI)* | VM/LXC | | | | |

### OS-rekommendation

| OS | Lämplig för MQTT-testlabb |
|----|---------------------------|
| Ubuntu / Debian | Ja — Docker, enkel drift |
| Slackware | Möjligt — mer manuellt |
| MariaDB (dedikerad) | Nej — håll databas separat |

## D.1 — SSH från fakir (mot testlab-gäst)

Ersätt `<gäst-ip>` och `<användare>` efter D.0-beslut.

```bash
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_hulda -N ""
ssh-copy-id -i ~/.ssh/id_ed25519_hulda.pub <användare>@<gäst-ip>
```

`~/.ssh/config` på fakir (ej i git) — `Host hulda` pekar på **gästen**:

```
Host hulda
  HostName <gäst-ip>
  User <användare>
  IdentityFile ~/.ssh/id_ed25519_hulda
  IdentitiesOnly yes
```

Verifiering:

```bash
ssh hulda true && echo "SSH OK"
```

### Manuell nyckel (konsol på gästen)

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHYWMe5KubRM+IT/SeYk7M5I5Cf+T6raEQq5ur7jpQgH fakir-hulda' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Se även `setup/hulda-ssh-key-oneliner.sh` (kör på **gästen**).

## D.2 — MQTT-broker (Docker Compose på gäst)

Källa: `test/fas2-mqtt/docker-compose.hulda.yml`

### Förutsättningar

- Docker + `docker compose` på gästen
- Repo: `~/Projects/rfid-manager/`

```bash
ssh hulda 'git clone https://github.com/JoaBerra/rfid-manager.git ~/Projects/rfid-manager || true'
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml up -d'
```

### Stoppa / omstart / loggar

```bash
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml down'
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml restart'
ssh hulda 'docker logs rfid-mqtt-hulda --tail 50'
```

### Verifiering från fakir

Använd **gäst-IP**, inte `.100`:

```bash
python3 -c "import socket; s=socket.socket(); s.settimeout(3); print(s.connect_ex(('<gäst-ip>',1883)))"
docker run --rm eclipse-mosquitto:2 mosquitto_pub -h <gäst-ip> -p 1883 -t test/uppdrag003 -m ok
ssh hulda "ss -tlnp | grep 1883"
```

### Valfri systemd (boot på gäst)

```bash
scp test/fas2-mqtt/systemd/rfid-mqtt.service hulda:/tmp/
ssh hulda 'sudo cp /tmp/rfid-mqtt.service /etc/systemd/system/ && sudo systemctl daemon-reload && sudo systemctl enable --now rfid-mqtt'
```

## Python-subscriber (på gäst)

```bash
ssh hulda
cd ~/Projects/rfid-manager/test/fas2-mqtt/mqtt
python3 -m venv .venv && source .venv/bin/activate
pip install paho-mqtt
python test_subscriber_persist.py
```

Kör i `tmux`/`screen` eller systemd. Ansluter till `localhost:1883` på gästen.

## Felsökning

| Symptom | Åtgärd |
|---------|--------|
| SSH till `.100` nekas | Förväntat — SSH ska gå till **gäst-IP** |
| Gäst startar inte | Proxmox UI → starta VM/LXC |
| Port 1883 stängd | `docker compose … up -d` på gästen |
| Telefon når inte broker | Wi-Fi samma LAN; broker-IP = gäst-IP |
| App DISCONNECTED | App pekar på falstaff — Settings eller Uppdrag 004 |

## Status (2026-07-12, iter 2)

| Del | Status |
|-----|--------|
| Proxmox `.100:8006` | Bekräftad |
| Testlab-gäst valt | **Väntar Principal** |
| `docker-compose.hulda.yml` | Klar |
| SSH + broker | Ej klart |

## Relaterat

- [[MQTT-Infrastruktur]] — `mosquitto.conf`
- [[Utvecklingsmiljö-fakir]] — fakir bygger, testlab i garage
- AH: [fas-d-testmiljo-hostar](https://github.com/JoaBerra/andra-hjarna/blob/main/Organisation/Processer/fas-d-testmiljo-hostar.md)
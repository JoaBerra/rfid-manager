---
title: Testmiljö — hulda (Proxmox)
tags: [hulda, proxmox, testmiljo, mqtt, fas-d, infrastruktur]
created: 2026-07-12
updated: 2026-10-03
---

# Testmiljö — hulda (Proxmox)

Permanent RFID/MQTT-testlabb i garaget. Del av **Fas D** ([Uppdrag 003](https://github.com/JoaBerra/andra-hjarna/blob/main/Bearbetning/2026-07-12-fas-d-hulda-testmiljo-uppdrag.md)).

## Arkitektur — två lager

| Lager | IP / åtkomst | Roll |
|-------|--------------|------|
| **Hypervisor** | `192.168.50.100`, Proxmox `:8006` | VM/LXC-drift (root UI) |
| **Testlab-gäst** | **ishtar** — `192.168.50.151/24` | MQTT, subscriber, Docker |

`192.168.50.100` är **inte** MQTT-värden — tjänsterna körs i en vald VM eller LXC.

**Inloggning (sedan 2026-10-03):** brokern på ishtar nekar anonym anslutning (`allow_anonymous false`, `password_file`, `acl_file`); `rfid-app` skriver och `rfid-dashboard` läser `rfidmanager/+/telemetry`. Skapande av lösenord, filägare (uid 1883, mode `0600`), ordning vid byte och återgång: README, avsnittet *Nätverk och säkerhet (MQTT)*. Termer: [[Ordlista]]. Backup av gamla konfigurationen: `~/backup-mqtt-2026-10-03` på ishtar.

**Aktiv broker:** ishtar `.151` (Uppdrag 004 / D.4). Legacy falstaff `.107` dokumentärt avvecklad — [[Fas-D-falstaff-avveckling]] (D.5).

## D.0 — Proxmox och gäst-val ✅

| Fält | Värde |
|------|-------|
| Gäst | **ishtar** (på) |
| OS | Debian |
| IP | `192.168.50.151/24` statisk |
| SSH-användare | `joakim` *(rekommenderat; justera om annat)* |

**SSH-setup:** se [`setup/ishtar-ssh-setup.md`](../setup/ishtar-ssh-setup.md) — port 22 var stängd 2026-07-12.

### OS-rekommendation

| OS | Lämplig för MQTT-testlabb |
|----|---------------------------|
| Ubuntu / Debian | Ja — Docker, enkel drift |
| Slackware | Möjligt — mer manuellt |
| MariaDB (dedikerad) | Nej — håll databas separat |

## D.1 — SSH från fakir → ishtar

### 1a. På ishtar (Proxmox Console) — SSH-server

```bash
sudo apt update && sudo apt install -y openssh-server
sudo systemctl enable --now ssh
```

Eller kör skriptet `setup/ishtar-enable-ssh.sh` i konsolen.

### 1b. Nyckel från fakir

```bash
ssh-copy-id -i ~/.ssh/id_ed25519_hulda.pub joakim@192.168.50.151
```

`~/.ssh/config` på fakir:

```
Host hulda ishtar
  HostName 192.168.50.151
  User joakim
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

- Docker + `docker-compose` på gästen (Debian trixie: paketet `docker-compose`, inte `docker-compose-plugin`)
- Repo: `~/Projects/rfid-manager/`

```bash
ssh hulda 'git clone https://github.com/JoaBerra/rfid-manager.git ~/Projects/rfid-manager || true'
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker-compose -f docker-compose.hulda.yml up -d'
```

### Stoppa / omstart / loggar

```bash
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker-compose -f docker-compose.hulda.yml down'
ssh hulda 'cd ~/Projects/rfid-manager/test/fas2-mqtt && docker-compose -f docker-compose.hulda.yml restart'
ssh hulda 'docker logs rfid-mqtt-hulda --tail 50'
```

### Verifiering från fakir

> **Obs (2026-10-03):** Portkontrollen fungerar som förut, men `mosquitto_pub` utan användare **nekas** nu av brokern (förväntat). Publicera med `-u rfid-app -P …` eller kontrollera med portkontrollen/`docker logs`. Skriv aldrig lösenord i klartext i dokument eller skalhistorik.

Broker-IP = **ishtar** `192.168.50.151`:

```bash
python3 -c "import socket; s=socket.socket(); s.settimeout(3); print(s.connect_ex(('192.168.50.151',1883)))"
docker run --rm eclipse-mosquitto:2 mosquitto_pub -h 192.168.50.151 -p 1883 -t test/uppdrag003 -m ok   # historiskt (anonym broker): nekas sedan 2026-10-03
ssh hulda "ss -tlnp | grep 1883"
```

### Valfri systemd (boot på gäst)

```bash
scp test/fas2-mqtt/systemd/rfid-mqtt.service hulda:/tmp/
ssh hulda 'sudo cp /tmp/rfid-mqtt.service /etc/systemd/system/ && sudo systemctl daemon-reload && sudo systemctl enable --now rfid-mqtt'
```

## MQTT Dashboard (på ishtar, ingen egen broker)

Webb-UI för realtidsflöde. Ansluter till **befintlig** broker `192.168.50.151:1883` — startar **inte** extra Mosquitto. Loggar in som `rfid-dashboard` via `MQTT_USERNAME`/`MQTT_PASSWORD` (i `dashboard/.env`, ignoreras av git — aldrig i compose-filen eller i git); utan `MQTT_USERNAME` loggar den inte in och nekas av brokern.

| Fält | Värde |
|------|-------|
| URL | `http://192.168.50.151:8000` |
| Compose | `dashboard/docker-compose.ishtar.yml` |
| Container | `rfid-mqtt-dashboard` |

### Starta / stoppa

```bash
ssh hulda '~/ishtar-start-dashboard.sh'
# eller
ssh hulda 'cd ~/Projects/rfid-manager/dashboard && docker-compose -f docker-compose.ishtar.yml up -d --build'
```

```bash
ssh hulda 'cd ~/Projects/rfid-manager/dashboard && docker-compose -f docker-compose.ishtar.yml down'
ssh hulda 'docker logs rfid-mqtt-dashboard --tail 30'
```

### Från fakir

Öppna webbläsare: **http://192.168.50.151:8000**

```bash
curl -s http://192.168.50.151:8000/api/stats
```

Loggar ska visa: `Connected to MQTT broker, subscribing to rfidmanager/+/telemetry`.

### MQTT Explorer (fakir, on-demand)

```bash
mqtt-explorer   # ~/.local/bin, v0.3.5 — host 192.168.50.151:1883
```

Kräver användare (`rfid-dashboard`, läsrätt) sedan 2026-10-03 — ännu inte uppsatt i Explorer (teknisk skuld).

Se [[MQTT-Explorer]].

## Python-subscriber (på gäst)

```bash
ssh hulda
cd ~/Projects/rfid-manager/test/fas2-mqtt/mqtt
python3 -m venv .venv && source .venv/bin/activate
pip install paho-mqtt
python test_subscriber_persist.py
```

Kör i `tmux`/`screen` eller systemd. Ansluter till `localhost:1883` på gästen. **Obs (2026-10-03):** skriptet ansluter anonymt och stödjer ännu inte inloggning, så det nekas av brokern (teknisk skuld; behöver en användare med läsrätt).

## Felsökning

| Symptom | Åtgärd |
|---------|--------|
| SSH till `.100` nekas | Förväntat — SSH ska gå till **gäst-IP** |
| Gäst startar inte | Proxmox UI → starta VM/LXC |
| Port 1883 stängd | `docker-compose … up -d` på gästen |
| Telefon når inte broker | Wi-Fi samma LAN; broker-IP = gäst-IP |
| App DISCONNECTED | Kontrollera broker i Settings — ska vara ishtar `.151` (default sedan v1.0.1) |
| App *Misslyckades* / ansluter inte efter 2026-10-03 | Fyll i Användarnamn (`rfid-app`) och Lösenord i Inställningar → MQTT-anslutning och tryck Anslut. Tomt användarnamn = anonym anslutning, som nekas |
| Broker startar inte, `Unable to open pwfile` i `docker logs rfid-mqtt-hulda` | `passwd`/`acl` har fel ägare eller mode — kör chown/chmod-steget i README (uid 1883, `0600`) |
| Dashboard `Ej ansluten` | `MQTT_USERNAME`/`MQTT_PASSWORD` saknas eller är fel i `dashboard/.env` |

## Status (2026-07-12, iter 2)

| Del | Status |
|-----|--------|
| Proxmox `.100:8006` | Bekräftad |
| Testlab-gäst **ishtar** `.151` | Vald |
| `docker-compose.hulda.yml` | Klar |
| SSH `ssh hulda` | ✅ Klar |
| Docker på ishtar | ✅ `docker.io` + `docker-compose` |
| MQTT-broker `rfid-mqtt-hulda` | ✅ `192.168.50.151:1883` |
| Dashboard `rfid-mqtt-dashboard` | ✅ `http://192.168.50.151:8000` |
| E2E app → broker → dashboard | ✅ Principal 2026-07-12 |
| E2E app → broker → MQTT Explorer (fakir) | ✅ Principal 2026-07-12 |
| Verifierat från fakir | ✅ MQTT + dashboard API |
| MQTT-inloggning + ACL | ✅ Aktiv 2026-10-03, verifierad end-to-end (telefon som `rfid-app`, dashboard som `rfid-dashboard`) |

## Relaterat

- [[MQTT-Infrastruktur]] — `mosquitto.conf`
- [[Utvecklingsmiljö-fakir]] — fakir bygger, testlab i garage
- AH: [fas-d-testmiljo-hostar](https://github.com/JoaBerra/andra-hjarna/blob/main/Organisation/Processer/fas-d-testmiljo-hostar.md)
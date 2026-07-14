---
title: Fas D — falstaff-avveckling (D.5)
tags: [fas-d, falstaff, legacy, mqtt, infrastruktur]
created: 2026-07-14
updated: 2026-07-14
status: dokumentärt-avvecklad
---

# Fas D — falstaff-avveckling (D.5)

**Uppdrag 005** — falstaff är **inte längre** aktiv MQTT-värd för RFID Manager. Kanonisk testbroker: **ishtar** `192.168.50.151:1883`.

## Bakgrund

| Host | IP | Roll (historik) | Status rfid-manager |
|------|-----|-----------------|---------------------|
| sixten | `192.168.50.128` | Tidig dev + broker | Ersatt av falstaff → ishtar |
| falstaff | `192.168.50.107` | Broker Fas-500 (2026-06) | **Avvecklad** (D.5) |
| ishtar | `192.168.50.151` | Testlab-gäst på hulda | **Aktiv** (D.2–D.4) |

Historik: [[Fas-500-Miljo-flytt-sixten-till-falstaff]], [[Release-Notes]] v1.0.1.

## Vad agenten gjort (2026-07-14)

- Operativ wiki pekar på ishtar — inte falstaff
- `network_security_config.xml` — cleartext endast för `.151`
- [[Utvecklingsmiljö-fakir]] — falstaff borttagen från aktiv host-tabell
- Verifiering från fakir: `192.168.50.107` ej pingbar (ingen aktiv broker-dependency)

## Principal-checklista (fysisk avveckling)

Kör på **falstaff** när maskinen är tillgänglig. Inget blockerar rfid-manager-utveckling om detta skjuts upp.

### 1. Inventera

```bash
docker ps -a | grep -i mqtt
docker volume ls
ss -tlnp | grep 1883
```

### 2. Stoppa legacy-broker (om den fortfarande körs)

```bash
# Exempel — justera container-namn efter `docker ps`
docker stop <mosquitto-container>
docker rm <mosquitto-container>    # valfritt
```

### 3. Backup (valfritt)

```bash
docker run --rm -v <volume>:/data -v $PWD:/backup alpine tar czf /backup/mosquitto-data.tar.gz /data
```

Spara arkiv på valfri plats (t.ex. NAS/pCloud) innan volym raderas.

### 4. Dokumentera i hemmanatverk

- Uppdatera nätverksdiagram: falstaff = avvecklad / ingen MQTT
- Notera om maskinen får ny roll eller tas ur drift

### 5. Verifiera att inget i hemmet pekar på `.107`

- App på telefon: Settings → broker ska vara `.151` (default sedan v1.0.1)
- MQTT Explorer på fakir: `192.168.50.151`

## Relaterat

- [[Testmiljo-hulda]] — aktiv testmiljö
- [[Utvecklingsmiljö-fakir]] — dev-host utan lokal broker
- Andra Hjärnan: [Uppdrag 005](https://github.com/JoaBerra/andra-hjarna/blob/main/Bearbetning/2026-07-14-rfid-manager-fas-d-renodling-uppdrag.md)
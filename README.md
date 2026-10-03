# RFID Manager

**Industriell Android-app för RFID/eskortminne + MQTT/IoT**

| Fält | Värde |
|------|-------|
| **Projektstatus** | **Pausat** (återöppningsbart) — avslutat 2026-07-14 |
| **Senaste release** | [v1.0.1](https://github.com/JoaBerra/rfid-manager/releases/tag/v1.0.1) |
| **Dev-host** | fakir (Arch/Omarchy) |
| **Testmiljö** | ishtar `192.168.50.151` (MQTT + dashboard på hulda/Proxmox) |
| **Organisation** | [Andra Hjärnan](https://github.com/JoaBerra/andra-hjarna) — projektkunskap och uppdrag |

Projektet är **avslutat i nuvarande fas**: appen fungerar mot testlabbet, Fas D är komplett, och dokumentation är synkad. Ingen aktiv utveckling planerad tills någon **återöppnar** projektet (se nedan).

---

## Vad som levererats

- Android-app (Kotlin, Jetpack Compose) — NFC läs/skriv, persistens, MQTT
- Default broker → **ishtar** `192.168.50.151:1883` (Uppdrag 004, v1.0.1)
- Testmiljö Fas D: Proxmox hulda + gäst ishtar, dashboard `:8000`
- Dev-baseline på **fakir**: Android SDK 36, git/gh, MQTT Explorer, wiki
- Uppdrag 001–005 godkända (onboarding → falstaff-avveckling)
- GitHub Release med debug-APK

---

## Struktur (repo)

| Sökväg | Innehåll |
|--------|----------|
| `RFIDManager/` | Android-projekt (Gradle) — **huvudkällkod** |
| `wiki/` | Projektwiki (Obsidian) — arkitektur, test, miljö |
| `test/fas2-mqtt/` | Mosquitto, subscriber, simulatorer (för ishtar) |
| `dashboard/` | Webb-dashboard (Docker) |
| `mcp-server/` | MCP för AI (parkerad — se AH idé) |
| `setup/` | Installationsskript (fakir, ishtar, SSH, m.m.) |
| `manual/` | Användarmanual |

Historiska referenser till `RFIDManager-android/`, `llm-wiki/` och separat GitHub-repo är **inaktuella** — allt ligger i detta monorepo under `RFIDManager/` och `wiki/`.

---

## Snabbstart (återuppta drift)

### 1. Bygg och installera app (fakir)

```bash
export ANDROID_HOME=~/Android/Sdk
cd RFIDManager
./gradlew assembleDebug
./gradlew installDebug    # Note 10 via USB
```

APK: `RFIDManager/app/build/outputs/apk/debug/app-debug.apk`  
Eller ladda från [GitHub Release v1.0.1](https://github.com/JoaBerra/rfid-manager/releases/tag/v1.0.1).

### 2. Testmiljö (ishtar)

```bash
ssh ishtar
# Se wiki/Testmiljo-hulda.md och setup/ishtar-start-mqtt.sh
```

- MQTT: `192.168.50.151:1883`
- Dashboard: `http://192.168.50.151:8000`
- MQTT Explorer på fakir: `mqtt-explorer`

### 3. Dokumentation

| Ämne | Fil |
|------|-----|
| Dev-miljö fakir | `wiki/Utvecklingsmiljö-fakir.md` |
| Testlabb | `wiki/Testmiljo-hulda.md` |
| Kanban / backlog | `wiki/Kanban.md` |
| Release notes | `wiki/Release-Notes.md` |
| UAT | `wiki/UAT-fakir-smoke-test.md` |

Diagnostik GitHub: `bash setup/github-fakir.sh`

---

## Kvar att göra (backlog vid återöppning)

Prioritera från `wiki/Kanban.md`. Sammanfattning:

### Hög prioritet — produkt

| ID | Beskrivning | Referens |
|----|-------------|----------|
| **Fas-101** | Full MQTT-konfig i appen (TLS, auth, Sparkplug, topics, QoS, testknapp) | `wiki/Fas-101-MQTT-Configuration.md` |
| **UAT NFC** | NFC-scan/write inte körd i senaste smoke — verifiera på Note 10 | `wiki/UAT-fakir-smoke-test.md` |
| **Release v1.0.2** | Ev. ny APK efter `network_security_config` (endast `.151`) | `wiki/Release-Notes.md` |

### Medel prioritet

| ID | Beskrivning |
|----|-------------|
| **Fas-100** | MQTT-fördjupning wiki (topics, QoS, säkerhet, E2E) — punkter 4–11 i Kanban |
| **Write-förbättringar** | Hex-checkbox; fullskärms-editor för write-data |
| **Connectivity UI** | Visa broker-IP på Connectivity-skärmen (planerat under Fas-101) |

### Låg prioritet / parkerat

| ID | Beskrivning |
|----|-------------|
| **MCP-server** | Parkerad idé i AH — `mcp-server/`, dashboard + Explorer räcker idag |
| **Room/KSP** | Riktig Room-databas blockerad av KSP/AGP — in-memory + JSON idag |
| **hemmanatverk** | Uppdatera nätverksdiagram (falstaff avvecklad) |

### Teknisk skuld

- `README` var inaktuell före 2026-07-14 (rättad i samband med avslut)
- README i root pekade på icke-existerande `rfid-manager-android`-repo
- MQTT utan autentisering på ishtar (se avsnittet *Nätverk och säkerhet (MQTT)*), accepterat tills labbet blir riktig drift

---

## Nätverk och säkerhet (MQTT)

Beslut 2026-10-03 (Joakim, tillsammans med Nora Nät som sköter hemmanätet): MQTT-testlabbet på ishtar körs **medvetet utan autentisering**. Det är en accepterad risk, inte ett förbiseende.

- **Broker:** Mosquitto, container `rfid-mqtt-hulda`, `192.168.50.151:1883`, `allow_anonymous true` (se `test/fas2-mqtt/mqtt/mosquitto.conf`).
- **Instrumentpanel:** container `rfid-mqtt-dashboard`, `192.168.50.151:8000`. Ansluter som standard till brokern utan användarnamn eller lösenord. Panelen kan logga in om miljövariablerna `MQTT_USERNAME` och `MQTT_PASSWORD` sätts (se nedan); utan dem är den anonym.
- **Klienter:** appen RFID Manager (Android, klient-id `rfid-android-client`, telefonen på `192.168.50.110`) och panelen på ishtar själv.
- **Varför det är acceptabelt nu:** båda tjänsterna nås bara från hemnätet (192.168.50.0/24). Routern har ingen port vidarebefordrad och ligger bakom CGNAT, så inget går att nå från internet. Det är ett testlab.
- **Brandvägg:** ishtar har varken `ufw` eller `iptables`, och Docker publicerar portarna direkt. Kuku (BTCPay) får inte `ufw` av samma skäl.

**Gör detta samtidigt om något i MQTT-kedjan ändras, eller om labbet blir riktig drift:**

1. Sätt `allow_anonymous false` och en lösenordsfil (`password_file`) i `mosquitto.conf`.
2. Lägg in användarnamn och lösenord i Android-appen: Inställningar → MQTT-anslutning (fälten *Användarnamn* och *Lösenord*, tryck *Anslut*). Lösenordet lagras krypterat (Android Keystore, AES-GCM). Tomt användarnamn = anonym anslutning. Stödet finns i grenen/koden; default är fortfarande anonymt.
3. Sätt `MQTT_USERNAME` och `MQTT_PASSWORD` för panelen. `dashboard/docker-compose.ishtar.yml` läser dem som `${MQTT_USERNAME:-}` och `${MQTT_PASSWORD:-}`; lägg värdena i en `.env` bredvid compose-filen (`dashboard/.env`, ignoreras av git) eller i skalets miljö – aldrig i compose-filen eller i git. Utan `MQTT_USERNAME` loggar panelen inte in.
4. Testa från telefonen och panelen att båda ansluter, och be Nora Nät uppdatera nätverksdokumentationen i repot `hemmanatverk` (avsnitt om ishtar och E3).

Ändra aldrig bara en av delarna: bara lösenord i brokern gör att både appen och panelen tappar anslutningen.

### Brokerfilerna för inloggning (förberedda, ännu inte tillämpade)

Steg 1 ovan är förberett i `test/fas2-mqtt/mqtt/`: `mosquitto.conf` har `allow_anonymous false`, `password_file` och `acl_file`; `acl` ger `rfid-app` skrivrätt och `rfid-dashboard` läsrätt på `rfidmanager/+/telemetry`; `mosquitto.conf.anon` är den tidigare anonyma konfigurationen (återgångsfil). `docker-compose.hulda.yml` monterar `passwd` och `acl` skrivskyddat. Raden om `allow_anonymous true` ovan beskriver alltså det som körs på ishtar nu, tills nedanstående är genomfört.

**Brokern startar inte utan `mqtt/passwd`** (och Docker skapar en tom katalog med det namnet om filen saknas). Tillämpa därför inte ändringen på ishtar förrän lösenordsfilen finns.

Ordning vid införande:

1. Skapa `test/fas2-mqtt/mqtt/passwd` enligt instruktionen i `mqtt/passwd.example` (`mosquitto_passwd`, användarna `rfid-app` och `rfid-dashboard`). Filen ignoreras av git.
2. **Direkt efter att `passwd` skapats** (på körkopian på ishtar, från repots rot) – sätt ägare och rättigheter för både `passwd` och `acl`:
   ```bash
   docker run --rm -v $PWD/test/fas2-mqtt/mqtt:/work eclipse-mosquitto:2 sh -c 'chown 1883:1883 /work/passwd /work/acl && chmod 0600 /work/passwd /work/acl'
   ```
   Se *Filägare och rättigheter* nedan. Kommandot fungerar utan `sudo` för användare i docker-gruppen (joakim).
3. Ställ in lösenorden i appen och för panelen (`MQTT_USERNAME`/`MQTT_PASSWORD`) enligt punkt 2–3 ovan.
4. Hämta ändringarna till ishtar och starta om: `docker compose -f test/fas2-mqtt/docker-compose.hulda.yml up -d --force-recreate`. Kör om chown/chmod-kommandot i steg 2 efter varje `git pull`/`checkout` som kan ha rört `acl`.
5. Testa att appen och panelen ansluter (steg 4 ovan).

#### Filägare och rättigheter (`passwd` och `acl`)

Mosquitto 2.1 kör som uid 1883 i containern och kräver att `passwd` och `acl` är läsbara för den användaren. `mosquitto_passwd` via `docker run` skapar `passwd` som `root:root` med mode `0600`; då startar brokern inte och loggar `Unable to open pwfile`. Lösning: `passwd` **och** `acl` ska ägas av uid 1883 med mode `0600` (kommandot i steg 2).

- Gör detta **bara på körkopian på ishtar**. Efter `chown` blir `acl` ocläsbar för joakim och för git på den kopian, så originalet versionshanteras i repot som vanligt (ägt av joakim) och ändras aldrig med `chown` där.
- En `git pull`/`checkout` kan skriva över ägaren på `acl`; kommandot måste därför köras om efter varje uppdatering av `acl`.
- Felsökning: `Unable to open pwfile` (eller liknande fel för `acl`) i `docker logs rfid-mqtt-hulda` betyder nästan alltid fel ägare/mode på `passwd` respektive `acl`. Kör kommandot i steg 2 och starta om containern.

#### Provkörning (Nora Nät)

Nora Nät har provkört brokern på ishtar med Mosquitto 2.1.2 (syntax och ACL, på port 11883, vid sidan av den ordinarie brokern). Resultat:

- Anonym anslutning nekas.
- Fel lösenord nekas.
- `rfid-app` kan skriva men inte läsa på `rfidmanager/+/telemetry`.
- `rfid-dashboard` kan läsa men inte skriva.

Provkörningen gav även fyndet om filägare ovan (`Unable to open pwfile` tills `passwd` och `acl` ägdes av uid 1883).

Återgång till anonym drift:

```bash
cp test/fas2-mqtt/mqtt/mosquitto.conf.anon test/fas2-mqtt/mqtt/mosquitto.conf
docker compose -f test/fas2-mqtt/docker-compose.hulda.yml up -d --force-recreate
```

Anonym konfig fungerar även med `passwd`/`acl` monterade, men filerna måste fortfarande finnas för att compose ska starta.

---

## Återöppna projektet

1. **Läs** denna README och `wiki/Kanban.md`
2. **Verifiera miljö:** `wiki/Utvecklingsmiljö-fakir.md`, `ssh ishtar`, broker svarar
3. **Andra Hjärnan:** `Kunskapsbas/Projekt/rfid-manager.md` + skapa nytt uppdrag (t.ex. Uppdrag 006) med mål och kriterier
4. **Välj backlog-punkt** — rekommenderat start: Fas-101 eller UAT NFC
5. **Uppdatera status** i README (tabellen överst) och AH från *Pausat* till *Pågående*

---

## Länkar

- **GitHub:** https://github.com/JoaBerra/rfid-manager
- **Andra Hjärnan:** https://github.com/JoaBerra/andra-hjarna
- **Domän (AH):** `Kunskapsbas/Amnen/rfid-adc/`

Utvecklat med AI-assistans (Grok, Qwen) på Arch Linux / Omarchy (fakir).
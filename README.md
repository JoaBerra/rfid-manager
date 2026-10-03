# RFID Manager

**Industriell Android-app för RFID/eskortminne + MQTT/IoT**

| Fält | Värde |
|------|-------|
| **Projektstatus** | **Pausat** (återöppningsbart) — avslutat 2026-07-14. Underhåll 2026-10-03: MQTT-inloggning (på `main`) och Room/SQLite (på `feature/sqlite`) |
| **Pågående gren** | `feature/sqlite` — Room/SQLite som enda lagring, verifierad på telefonen 2026-10-03, **ännu inte mergad till `main`** (se *Room/SQLite (feature/sqlite)* nedan) |
| **Senaste release** | [v1.0.1](https://github.com/JoaBerra/rfid-manager/releases/tag/v1.0.1) |
| **Dev-host** | fakir (Arch/Omarchy) |
| **Testmiljö** | ishtar `192.168.50.151` (MQTT + dashboard på hulda/Proxmox) |
| **Organisation** | [Andra Hjärnan](https://github.com/JoaBerra/andra-hjarna) — projektkunskap och uppdrag |

Projektet är **avslutat i nuvarande fas**: appen fungerar mot testlabbet, Fas D är komplett, och dokumentation är synkad. Ingen aktiv utveckling planerad tills någon **återöppnar** projektet (se nedan).

---

## Vad som levererats

- Android-app (Kotlin, Jetpack Compose) — NFC läs/skriv, persistens (v1.0.1: JSON-fil; `feature/sqlite`: Room/SQLite), MQTT
- Default broker → **ishtar** `192.168.50.151:1883` (Uppdrag 004, v1.0.1)
- **MQTT-inloggning** på ishtar sedan 2026-10-03 (`rfid-app` skriver, `rfid-dashboard` läser, ACL) — på `main` (`f08ff22`)
- **Room/SQLite** som enda lagring på grenen `feature/sqlite` (migrering från `readings.json` verifierad på telefonen 2026-10-03), dynamisk version/byggtid/commit i Inställningar
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

- MQTT: `192.168.50.151:1883` — **kräver inloggning** (anonym anslutning nekas); se *Nätverk och säkerhet (MQTT)* nedan
- Dashboard: `http://192.168.50.151:8000`
- MQTT Explorer på fakir: `mqtt-explorer`

### 3. Dokumentation

| Ämne | Fil |
|------|-----|
| Dev-miljö fakir | `wiki/Utvecklingsmiljö-fakir.md` |
| Testlabb | `wiki/Testmiljo-hulda.md` |
| Kanban / backlog | `wiki/Kanban.md` |
| Release notes | `wiki/Release-Notes.md` |
| Ordlista (Konnotation \| Denotation) | `wiki/Ordlista.md` |
| UAT | `wiki/UAT-fakir-smoke-test.md` |

Diagnostik GitHub: `bash setup/github-fakir.sh`

---

## Kvar att göra (backlog vid återöppning)

Prioritera från `wiki/Kanban.md`. Sammanfattning:

### Hög prioritet — produkt

| ID | Beskrivning | Referens |
|----|-------------|----------|
| **Fas-101** | Full MQTT-konfig i appen. *Autentisering (användarnamn/lösenord, Anslut-knapp med bekräftelse) är klar 2026-10-03*; kvar: TLS, Sparkplug-id, topics, QoS, testknapp | `wiki/Fas-101-MQTT-Configuration.md` |
| **Utkorg (outbox)** | ✅ Implementerad på `feature/outbox` (2026-10-03; ersätter `markAsTransmitted`). **Verifierad på telefon och mot riktiga brokern 2026-10-03** (felvägarna fel lösenord/12 försök/`FAILED`/omstart provade och verifierade på telefon 2026-10-03). Kvar: merga (efter `feature/sqlite`) | `wiki/Outbox.md` |
| **Merge** | Besluta om och merga `feature/sqlite` till `main` (verifierad på telefon, `48e8a89`) | *Room/SQLite (feature/sqlite)* |
| **UAT NFC** | NFC-scan/write inte körd i senaste smoke — verifiera på Note 10 | `wiki/UAT-fakir-smoke-test.md` |
| **Release v1.0.2** | Ev. ny APK efter `network_security_config` (endast `.151`), MQTT-inloggning och Room. `assembleRelease` kräver `~/.android/debug.keystore`, som saknas på fakir | `wiki/Release-Notes.md` |

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
| **Room/SQLite** | **Klar på grenen `feature/sqlite`** (Room 2.8.5 + KSP 2.3.12, AGP 9.2.1) och verifierad på telefonen 2026-10-03. Väntar på merge till `main` — se *Merge* ovan och *Room/SQLite (feature/sqlite)* nedan |
| **hemmanatverk** | Uppdatera nätverksdiagram (falstaff avvecklad) |

### Teknisk skuld

- `README` var inaktuell före 2026-07-14 (rättad i samband med avslut) *(historiskt/löst)*
- README i root pekade på icke-existerande `rfid-manager-android`-repo *(historiskt/löst)*
- ~~MQTT utan autentisering på ishtar~~ — löst: inloggning aktiv sedan 2026-10-03 (se avsnittet *Nätverk och säkerhet (MQTT)*)
- Felmeddelandet vid fel lösenord (*Misslyckades ✗* med felorsak, commit `12012a2`) är **inte visuellt verifierat** på telefonen — provas separat. Att Anslut-knappen ger bekräftelse redan vid första tryck är däremot verifierat (`12012a2`)
- Testskripten i `test/fas2-mqtt/mqtt/` och MQTT Explorer ansluter anonymt och fungerar inte mot brokern förrän de får användare (`rfid-app`/`rfid-dashboard`) — **kvarstående teknisk skuld**
- ~~SQLite istället för `readings.json` (Room/KSP saknas i bygget)~~ — löst på grenen `feature/sqlite` (commits `eab6b6a..48e8a89`), verifierad på telefonen 2026-10-03: 3 poster migrerade, databasen kontrollerad. Väntar på merge till `main`
- `assembleRelease` misslyckas på fakir eftersom `~/.android/debug.keystore` saknas (`signingConfigs.release` pekar dit); `assembleDebug` påverkas inte
- ~~**Bugg:** `ReadingsViewModel.onTransmit` anropade `repository.markAsTransmitted(id)` oavsett om MQTT-publiceringen lyckades~~ — **löst på `feature/outbox`** (2026-10-03): en post blir `SENT` först när brokern bekräftat (QoS 1), se avsnittet *Utkorg (feature/outbox)* och `wiki/Outbox.md`. Kvar i `feature/sqlite` och `main` tills `feature/outbox` mergats
- Backlog: efter lyckad migrering finns `readings.json.migrated` kvar för alltid (raderas aldrig automatiskt) — bestäm när den kan tas bort manuellt

---

## Room/SQLite (feature/sqlite)

**Status 2026-10-03:** Room/SQLite är **enda lagringen** på `feature/sqlite` (commits `eab6b6a..48e8a89`) och är verifierad på telefonen: 3 poster migrerades från `readings.json` och databasen kontrollerades. Grenen är **ännu inte mergad till `main`**; `main` och GitHub Release v1.0.1 använder fortfarande JSON-filen. Termer (Room, KSP, migrering, outbox m.fl.) är definierade i `wiki/Ordlista.md`.

Grenen `feature/sqlite` ersätter JSON-/minneslagringen med Room (SQLite): `AppDatabase` version 1, tabell `persisted_readings` med index på `timestamp` och `transmitted`, schema exporterat i `RFIDManager/app/schemas/`. Bygget använder KSP (`ksp = 2.3.12`) och Room 2.8.5; AGP 9.2.1, Kotlin 2.2.10 och Gradle 9.4.1 är oförändrade. Ingen `fallbackToDestructiveMigration`.

**Migrering vid första start efter uppdatering:** `filesDir/readings.json` läses in i Room i en enda transaktion (id bevaras; id-krockar och dubbletter hanteras utan att data tappas), därefter döps filen om till `readings.json.migrated` (raderas aldrig). Misslyckas migreringen ligger JSON-filen kvar orörd, felet loggas (taggen `JsonToRoomMigration`) och visas i Inställningar → Lagring, och migreringen provas igen vid nästa start. Under tiden sparas nya avläsningar i Room men JSON-innehållet visas inte förrän migreringen lyckats.

Status vid överföring är på `feature/sqlite` alltid `transmitted` (tidigare `transmitted via Sparkplug` i JSON-läget; äldre värden normaliseras vid migreringen). Det är där en markering, inte ett kvitto från brokern — rättat av utkorgen på `feature/outbox` (se nedan).

**Bygginfo i Inställningar (`48e8a89`):** App-info visar nu riktig version (`VERSION_NAME`), byggtid och git-commit (`BuildConfig.BUILD_TIME` / `GIT_COMMIT`, suffix `-dirty` vid ej incheckade ändringar) i stället för den hårdkodade texten *Fas 5 (juni 2026)* *(historiskt/löst)*. Värdena beräknas vid varje Gradle-körning; aktiveras konfigurationscache måste logiken flyttas (kommentar i `app/build.gradle.kts`).

Bygga och testa: `cd RFIDManager && ANDROID_HOME=~/Android/Sdk ./gradlew assembleDebug testDebugUnitTest`.

---

## Utkorg (feature/outbox)

**Status 2026-10-03:** implementerad på grenen `feature/outbox` (utgår från `feature/sqlite`; ej mergad; **verifierad av Joakim på telefon och mot riktiga brokern 2026-10-03**). Full beskrivning: `wiki/Outbox.md`.

- **Spara först, skicka sedan:** varje avläsning skrivs som `PENDING` i Room direkt. Status `PENDING` (Väntar) / `SENT` (Skickad, brokern har bekräftat med QoS 1) / `FAILED` (Misslyckad) + `attempts`, `lastError`, `lastAttemptAt`, `sentAt`. Room-schema 2 med riktig `Migration` 1→2 (`transmitted=1`→`SENT`, `0`→`PENDING`; **äldre poster med `transmitted=0` skickas automatiskt första gången**).
- **Generisk kärna** i paketet `outbox/core` (ren Kotlin, inga Android-beroenden): `OutboxStore`, `OutboxTransport`, `BackoffPolicy`, `OutboxDispatcher`. MQTT (Paho) och Room är adaptrar. Kan återanvändas av FASAD (annat repo, orört) — se `wiki/Outbox.md`.
- **Utskick:** WorkManager (`androidx.work:work-runtime-ktx:2.12.0`, nytt beroende) med unik kö, nätverkskrav och exponentiell backoff; workern skapar egen MQTT-anslutning från sparade inställningar och körs även när appen är stängd. Triggas av ny avläsning, appstart, nätverk och *Skicka nu*.
- **Dubblettskydd:** meddelandet har `id`, `deviceId`, `messageId`; dashboarden ignorerar redan sedda (LRU i minnet, enhetstestat).
- **Verifierat på enhet 2026-10-03 (Joakim, telefon + riktiga brokern på ishtar):** avläsningar blir *Väntar* offline och skickas när nätverk finns, även med appen stängd; de kommer fram en gång var på dashboarden och får status *Skickad*. Room-migreringen 1→2 är körd på telefonens riktiga databas (4 poster blev `SENT`). Dashboarden på ishtar är driftsatt med dubblettskydd (`c16ef43`; backup `~/backup-dashboard-20261003-115215.tar` på ishtar). **Felvägarna provade och verifierade av Joakim på telefon 2026-10-03:** fel lösenord → *Väntar* med röd felorsak och växande *Försök*; efter 12 försök `FAILED` (nästa post går igenom samma process; *Skicka nu* köar om Misslyckade); telefon omstartad med väntande poster → skickas av sig själva inom ett par minuter när nätet slås på. Kvar: merge till `main` (efter `feature/sqlite`). FASAD-172 (utvärdering av utkorgsmönstret) kan starta; Kalle PL Fasad är informerad.
- **Test:** `cd RFIDManager && ANDROID_HOME=~/Android/Sdk ./gradlew assembleDebug testDebugUnitTest` och `cd dashboard && python3 -m unittest`; migrering mot riktig SQLite: `python3 RFIDManager/tools/verify_migration_1_2.py`.

---

## Nätverk och säkerhet (MQTT)

**Inloggning aktiv sedan 2026-10-03.** MQTT-brokern på ishtar kräver användarnamn och lösenord; anonym anslutning nekas. Driftsättningen gjordes 2026-10-03 (09:00–09:11) och verifierades end-to-end: telefonen anslöt som `rfid-app`, instrumentpanelen som `rfid-dashboard`, och en NFC-avläsning kom fram till panelen.

- **Broker:** Mosquitto, container `rfid-mqtt-hulda`, `192.168.50.151:1883`, `allow_anonymous false` med `password_file` och `acl_file` (se `test/fas2-mqtt/mqtt/mosquitto.conf`).
- **Användare och ACL** (`test/fas2-mqtt/mqtt/acl`): `rfid-app` **skriver** och `rfid-dashboard` **läser** på `rfidmanager/+/telemetry`. Appen kan alltså inte läsa och panelen kan inte skriva.
- **Instrumentpanel:** container `rfid-mqtt-dashboard`, `192.168.50.151:8000`. Loggar in som `rfid-dashboard` via miljövariablerna `MQTT_USERNAME` och `MQTT_PASSWORD` (se nedan).
- **Klienter:** appen RFID Manager (Android, klient-id `rfid-android-client`, telefonen på `192.168.50.110`, användare `rfid-app`) och panelen på ishtar själv (användare `rfid-dashboard`).
- **Backup av gamla konfigurationen:** finns på ishtar i `~/backup-mqtt-2026-10-03`.
- **Nätet:** båda tjänsterna nås bara från hemnätet (192.168.50.0/24). Routern har ingen port vidarebefordrad och ligger bakom CGNAT, så inget går att nå från internet. Det är fortfarande ett testlab.
- **Brandvägg:** ishtar har varken `ufw` eller `iptables`, och Docker publicerar portarna direkt. Kuku (BTCPay) får inte `ufw` av samma skäl.

**Gör detta samtidigt om något i MQTT-kedjan ändras (nytt lösenord, ny användare, ny klient):**

1. Brokern: `allow_anonymous false`, `password_file` och `acl_file` i `mosquitto.conf` (redan så).
2. Android-appen: Inställningar → MQTT-anslutning (fälten *Användarnamn* och *Lösenord*, tryck *Anslut*). Lösenordet lagras krypterat (Android Keystore, AES-GCM). Tomt användarnamn = anonym anslutning, vilket brokern nu nekar.
3. Panelen: `MQTT_USERNAME` och `MQTT_PASSWORD`. `dashboard/docker-compose.ishtar.yml` läser dem som `${MQTT_USERNAME:-}` och `${MQTT_PASSWORD:-}`; lägg värdena i en `.env` bredvid compose-filen (`dashboard/.env`, ignoreras av git) eller i skalets miljö – aldrig i compose-filen eller i git. Utan `MQTT_USERNAME` loggar panelen inte in.
4. Testa från telefonen och panelen att båda ansluter, och be Nora Nät uppdatera nätverksdokumentationen i repot `hemmanatverk` (avsnitt om ishtar och E3).

Ändra aldrig bara en av delarna: ändras lösenordet bara i brokern tappar både appen och panelen anslutningen.

### Brokerfilerna för inloggning (tillämpade 2026-10-03)

Filerna i `test/fas2-mqtt/mqtt/`: `mosquitto.conf` har `allow_anonymous false`, `password_file` och `acl_file`; `acl` ger `rfid-app` skrivrätt och `rfid-dashboard` läsrätt på `rfidmanager/+/telemetry`; `mosquitto.conf.anon` är den tidigare anonyma konfigurationen (återgångsfil). `docker-compose.hulda.yml` monterar `passwd` och `acl` skrivskyddat.

**Brokern startar inte utan `mqtt/passwd`** (och Docker skapar en tom katalog med det namnet om filen saknas).

Ordning vid (nyinförande eller) byte av lösenord:

1. Skapa `test/fas2-mqtt/mqtt/passwd` enligt avsnittet *Skapa lösenord (rekommenderat sätt)* nedan (och `mqtt/passwd.example`). Filen ignoreras av git.
2. **Direkt efter att `passwd` skapats** (på körkopian på ishtar, från repots rot) – sätt ägare och rättigheter för både `passwd` och `acl`:
   ```bash
   docker run --rm -v $PWD/test/fas2-mqtt/mqtt:/work eclipse-mosquitto:2 sh -c 'chown 1883:1883 /work/passwd /work/acl && chmod 0600 /work/passwd /work/acl'
   ```
   Se *Filägare och rättigheter* nedan. Kommandot fungerar utan `sudo` för användare i docker-gruppen (joakim).
3. Ställ in lösenorden i appen och för panelen (`MQTT_USERNAME`/`MQTT_PASSWORD`) enligt punkt 2–3 ovan.
4. Hämta ändringarna till ishtar och starta om: `docker compose -f test/fas2-mqtt/docker-compose.hulda.yml up -d --force-recreate`. Kör om chown/chmod-kommandot i steg 2 efter varje `git pull`/`checkout` som kan ha rört `acl`.
5. Testa att appen och panelen ansluter (steg 4 ovan).

#### Skapa lösenord (rekommenderat sätt)

Det interaktiva sättet (`docker run -it ... mosquitto_passwd`) fungerade inte i praktiken – lösenordet godkändes inte. Använd i stället `read -rs` och `-b`. Från repots rot:

```bash
read -rs PW; docker run --rm -v $PWD/test/fas2-mqtt/mqtt:/work eclipse-mosquitto:2 mosquitto_passwd -b -c /work/passwd rfid-app "$PW"
unset PW
```

Kör sedan samma sak för nästa användare, `rfid-dashboard`, **utan `-c`**:

```bash
read -rs PW; docker run --rm -v $PWD/test/fas2-mqtt/mqtt:/work eclipse-mosquitto:2 mosquitto_passwd -b /work/passwd rfid-dashboard "$PW"
unset PW
```

- **`-c` skriver över hela filen.** Använd det bara för den första användaren, annars försvinner tidigare användare.
- `read -rs` läser lösenordet utan att visa det och utan att det hamnar i kommandoraden. **Skriv aldrig lösenordet i klartext på kommandoraden** (`... -b ... rfid-app hemligt`): då hamnar det i shell-historiken (och syns i processlistan).
- `unset PW` direkt efteråt så lösenordet inte ligger kvar i skalets miljö.
- Kör sedan chown/chmod-steget (steg 2 ovan).

#### Filägare och rättigheter (`passwd` och `acl`)

Mosquitto 2.1 kör som uid 1883 i containern och kräver att `passwd` och `acl` är läsbara för den användaren. `mosquitto_passwd` via `docker run` skapar `passwd` som `root:root` med mode `0600`; då startar brokern inte och loggar `Unable to open pwfile`. Lösning: `passwd` **och** `acl` ska ägas av uid 1883 med mode `0600` (kommandot i steg 2).

- Gör detta **bara på körkopian på ishtar**. Efter `chown` blir `acl` ocläsbar för joakim och för git på den kopian, så originalet versionshanteras i repot som vanligt (ägt av joakim) och ändras aldrig med `chown` där.
- En `git pull`/`checkout` kan skriva över ägaren på `acl`; kommandot måste därför köras om efter varje uppdatering av `acl`.
- Felsökning: `Unable to open pwfile` (eller liknande fel för `acl`) i `docker logs rfid-mqtt-hulda` betyder nästan alltid fel ägare/mode på `passwd` respektive `acl`. Kör kommandot i steg 2 och starta om containern.

#### Provkörning (Nora Nät)

Nora Nät provkörde brokern på ishtar före driftsättning med Mosquitto 2.1.2 (syntax och ACL, på port 11883, vid sidan av den ordinarie brokern). Resultat:

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
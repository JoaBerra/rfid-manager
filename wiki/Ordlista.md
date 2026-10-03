---
title: Ordlista — Konnotation | Denotation
tags: [ordlista, terminologi, nomenklatur, room, ksp, mqtt, acl]
created: 2026-10-03
---

# Ordlista — Konnotation | Denotation

> **Syfte:** Varje term som används i dokumentationen ska vara definierad någonstans. Den här sidan är stället för tekniska termer (lagring, bygge, MQTT-säkerhet). Namnsättning för Figma och kod finns i [[Nomenclature-Figma-Android]]; roller och funktionella termer i [[Rollfördelning-och-Arbetsätt]].

**Så läser du tabellen**

| Konnotation | Denotation |
|-------------|------------|
| Vad termen betyder hos oss: definitionen. | Vad vi **inte** menar med termen: avgränsning mot närliggande eller vanliga missförstånd. |

Nya termer läggs till här när de först används i ett dokument. Historiska termer tas inte bort utan markeras *(historiskt)* eller *(löst)*.

---

## Lagring och bygge (Android)

| Term | Konnotation | Denotation |
|------|-------------|------------|
| **Room** | Googles bibliotek (androidx.room, version 2.8.5 hos oss) som ger appen ett typat lager ovanpå SQLite: entiteter, DAO och databasklass. Enda lagringen för avläsningar på `main` (mergad 2026-10-03). | Inte själva databasen (det är SQLite) och inte ett nätverksbibliotek. Inte den tidigare JSON-fallbacken eller minneslistan. |
| **SQLite** | Databasmotorn som lagrar filen `rfid_manager_database` i appens privata lagring på telefonen. | Inte en server, och inte den SQLite-fil som Python-subscribern skriver på testdatorn (`data/rfid_readings.db`) — den är en separat databas för mottagna meddelanden. |
| **DAO** (Data Access Object) | Gränssnitt (`PersistedReadingDao`) med de sökningar och skrivningar som Room genererar kod för. | Inte repository-klassen och inte UI-koden. |
| **KSP** (Kotlin Symbol Processing) | Verktyget i bygget som genererar Rooms kod vid kompilering (`ksp = 2.3.12`). | Inte `kapt` (äldre och borttaget i Kotlin 2.2.x) och inte ett körtidsbibliotek i appen. |
| **AGP** (Android Gradle Plugin) | Byggpluginet för Android (9.2.1 hos oss). Dess inbyggda Kotlin-stöd var det som tidigare gjorde att KSP inte kunde användas. | Inte Gradle själv (9.4.1) och inte Kotlin-kompilatorn (2.2.10). |
| **KSP blockerad** *(historiskt/löst)* | Läget under v1.0/v1.0.1: KSP-versionen då stödde inte AGP 9, så Room kunde inte byggas. Löst på `feature/sqlite` 2026-10-03 med KSP 2.3.12. | Gäller inte längre `main`. Gäller bara de släppta versionerna v1.0 och v1.0.1 på `main`. |
| **JSON-fallback / minnesläge** *(historiskt)* | Den tidigare reservlagringen: `readings.json` i `filesDir` respektive en lista i minnet. Borttagen som lagring (mergat till `main` 2026-10-03). | Inte en tyst reserv idag: misslyckas Room loggas felet och kastas vidare, appen byter inte lagring. |
| **Migrering** (JSON → Room) | Engångsflytt av poster från `readings.json` till Room, gjord i en enda transaktion vid första start efter uppdatering. Id bevaras, id-krockar och dubbletter hanteras utan dataförlust. | Inte en schemamigrering av Room (`Migration`-klass; sådan finns separat för version 1→2, mergad till `main`) och inte en flytt av data mellan telefoner. |
| **Transaktion** | Allt-eller-inget i databasen: lyckas inte hela skrivningen rullas allt tillbaka. | Inte en MQTT-leverans och inte en affärstransaktion. |
| **`readings.json.migrated`** | Den gamla JSON-filen efter lyckad migrering (namnbyte, aldrig radering). Finns kvar tills någon tar bort den manuellt. | Inte en säkerhetskopia av databasen och inte en fil som appen läser igen. |
| **Status `transmitted`** | Äldre kolumner (`transmitted`, `status`) som sedan utkorgen (mergad till `main` 2026-10-03) bara är en spegling av `outboxStatus = SENT`. Före utkorgen (på `feature/sqlite`) sattes den oavsett leverans. | Inte sanningen om leverans — den är `outboxStatus`. |
| **`markAsTransmitted`** *(historiskt/löst)* | Repository-metoden som satte status `transmitted` oavsett om MQTT-publiceringen lyckades. **Borttagen med utkorgen** (`feature/outbox`, mergad till `main` 2026-10-03). | Inte längre en teknisk skuld. |
| **Outbox** (utkorg) | Mönster **implementerat (`feature/outbox`, mergad till `main` 2026-10-03) och verifierat på telefon och mot riktiga brokern 2026-10-03** (felvägarna fel lösenord/12 försök/`FAILED`/omstart provade och verifierade av Joakim på telefon 2026-10-03): varje avläsning sparas först som `PENDING` i Room och skickas sedan av en bakgrundsarbetare i tidsordning, en i taget; posten blir `SENT` först när brokern bekräftat (QoS 1), annars omförsök med backoff och till slut `FAILED`. Se [[Outbox]]. | Inte brokerns egna köer, inte en separat tabell (samma tabell `persisted_readings`, schema v2) och inte garanterad exakt-en-gång-leverans — den är *minst-en-gång*, dashboarden avduplicerar. |
| **Utkorgens status** (`PENDING` / `SENT` / `FAILED`) | Väntar / Skickad (bekräftad av brokern) / Misslyckad (max antal försök nått). | `SENT` betyder bekräftat av brokern, inte att dashboarden hunnit visa det. |
| **Omgång** (omförsöksomgång) | En serie av *försök per omgång* (standard 12) med växande väntetid. Efter varje omgång utom den sista följer en *paus*; efter sista omgången (standard 3) blir posten `FAILED`. Inställbart i Inställningar. Rundnumret lagras inte utan härleds ur `attempts`. Mergad till `main` 2026-10-03 (gren `feature/outbox-rounds`), verifierad av Joakim på telefon 2026-10-03; [[Outbox]]. | Inte en omgång i spel- eller testmening och inte en `Migration`. Inte en ny status: under hela omgångarna är posten `PENDING` (*Väntar*). |
| **Paus** (mellan omgångar) | Väntan efter att en omgång tagit slut (standard 60 min), under vilken inga försök görs och posten står som *Väntar* med kvarstående felorsak och texten om när nästa omgång startar. *Skicka nu* kringgår den; nätverk som kommer tillbaka gör det inte. | Inte backoff (den korta växande väntan mellan försök inom en omgång), inte `FAILED` och inte en avstängd utkorg. |
| **Backoff** | Växande väntetid mellan två försök inom en omgång: 30 s, 60 s, 120 s … tak 30 min (`ExponentialBackoff`); börjar om i varje ny omgång. | Inte pausen mellan omgångar och inte WorkManagers egen omstartsfördröjning (30 s, upp till 5 h) — de två lagren är avsiktligt likartade men separata. |
| **Försök** (`attempts`) | Antal misslyckade sändningsförsök hittills för en post (visas som *Försök* i appen). Allt omgångsbeteende räknas ur detta tal. | Inte antal lyckade skick och inte antal omgångar. |
| **Försök per omgång** | Inställning 1–100, standard 12: hur många försök en omgång innehåller. | Inte totalt antal försök (det är försök × omgångar, standard 36). |
| **`RoundsConfig` / `RetryPolicy`** | Kärnans (`outbox/core/RetryPolicy.kt`) generiska klasser: `RoundsConfig` håller försök per omgång, paus i minuter och antal omgångar; `RetryPolicy` avgör paus, väntetid och när posten ges upp. Ren Kotlin, återanvänds av FASAD-172. | Inte appens inställningsskärm (den heter `OutboxRoundsInput` + `AppSettings`) och inte en Android-klass. |
| **Skicka nu** | Knappen som köar om och försöker direkt: ignorerar backoff **och pausen** (`force = true`, `skipPause = true`); en `FAILED`-post köas om med försök nollställda. | Inte ett garanterat lyckat skick — misslyckas det är posten fortfarande *Väntar* (första försöket i nästa omgång). |
| **Nätverk tillbaka** | Utlösare när telefonen får nät: försöker direkt och hoppar över backoff men **inte** pausen (`force = true`, `skipPause = false`). | Inte samma sak som *Skicka nu*. |
| **WorkManager** | Androids bibliotek för uppskjutet bakgrundsarbete (`androidx.work`, 2.12.0): kör utskicket när nätverk finns, även när appen är stängd, med backoff. | Inte en tjänst som alltid kör och inte en MQTT-klient. |
| **Ack / PUBACK** | Brokerns kvitto på ett QoS 1-meddelande. Paho väntar på det (`waitForCompletion`) innan posten markeras `SENT`. | Inte en bekräftelse från dashboarden. |
| **`messageId`** | Stabil dubblettnyckel i varje meddelande: `<deviceId>:<postens id>`. Samma post har alltid samma `messageId`, även vid omsändning. | Inte MQTT:s interna paket-id och inte `uid` (taggens id). |
| **Dubblettskydd** | Dashboarden ignorerar `messageId`/id den redan sett (LRU, 5000, bara i minnet). | Inte persistent över omstart av dashboarden; meddelanden utan id räknas alltid. |
| **Idempotent** | Samma post kan skickas flera gånger utan skada hos mottagaren (tack vare `messageId`). | Inte samma sak som att den bara skickas en gång. |
| **FASAD** | Annat projekt (eget repo; ägs av Kalle PL Fasad) som kan återanvända utkorgens kärna (`outbox/core`). Repot har inte rörts. | Inte en del av RFID Manager. |
| **BuildConfig** | Klass som Android genererar vid bygge med konstanter. Vi lägger in `BUILD_TIME` (byggtid, Europe/Stockholm) och `GIT_COMMIT` (kort hash, suffix `-dirty` vid ej incheckade ändringar, annars `okänd` om git saknas). | Inte appens inställningar vid körning och inte ett versionsnummer (versionen kommer från `VERSION_NAME`). |
| **Bygginfo / App-info** | Raderna Version, Bygg (`BUILD_TIME · GIT_COMMIT`), Ramverk och MQTT i Inställningar. Värdena är dynamiska sedan `48e8a89`. | Inte den tidigare hårdkodade texten "Fas 5 (juni 2026)" *(historiskt)*. |
| **`debug.keystore`** | Debug-nyckeln som `signingConfigs.release` i `app/build.gradle.kts` pekar på (`~/.android/debug.keystore`). Saknas på fakir idag, så `assembleRelease` misslyckas (teknisk skuld). | Inte en produktionsnyckel; en release-APK från denna konfiguration vore debug-signerad. |

## MQTT och säkerhet

| Term | Konnotation | Denotation |
|------|-------------|------------|
| **MQTT-inloggning** | Brokern kräver användarnamn och lösenord (`allow_anonymous false` + `password_file`). Aktiv på ishtar sedan 2026-10-03. | Inte kryptering: trafiken är fortfarande klartext (`tcp://`, ingen TLS). |
| **Anonym anslutning** | Anslutning utan användarnamn. Nekas av brokern sedan 2026-10-03. Tomt användarnamn i appen betyder anonym anslutning. | Inte det som gäller idag; anonym drift finns bara som återgångsfil (`mosquitto.conf.anon`). |
| **`allow_anonymous`** | Direktiv i `mosquitto.conf`. Idag `false`. | Inte en ACL-regel; det styr bara om anslutning utan inloggning tillåts alls. |
| **ACL** (Access Control List) | Fil (`test/fas2-mqtt/mqtt/acl`) som anger vilka topics varje användare får läsa eller skriva. `rfid-app` får skriva och `rfid-dashboard` får läsa `rfidmanager/+/telemetry`. | Inte lösenordsfilen och inte brandväggsregler i nätet. |
| **`passwd`** | Lösenordsfilen (`mqtt/passwd`) med hashade lösenord, skapad med `mosquitto_passwd`. Ignoreras av git. | Inte klartext och aldrig incheckad (mallen `passwd.example` är det enda som finns i repot). |
| **`mosquitto_passwd -b`** | Verktyget som skapar och ändrar `passwd`; `-b` tar lösenordet från kommandoraden, så vi matar det via `read -rs` (syns inte, hamnar inte i historiken). `-c` skriver över hela filen. | Inte det interaktiva sättet (fungerade inte i praktiken) och inte något man skriver lösenordet i klartext till. |
| **uid 1883** | Användar-id som Mosquitto kör som i containern. `passwd` och `acl` ska ägas av uid 1883 med mode `0600` på körkopian, annars: `Unable to open pwfile`. | Inte `root` och inte användaren joakim. |
| **`rfid-app`** | MQTT-användare som appen loggar in som. Får bara skriva. | Inte en Android-användare eller ett GitHub-konto. |
| **`rfid-dashboard`** | MQTT-användare som webb-dashboarden loggar in som (`MQTT_USERNAME`/`MQTT_PASSWORD`). Får bara läsa. | Inte en inloggning mot dashboardens webbsida. |
| **`eclipse-mosquitto:2`** | Image-taggen produktion kör (ishtar, `docker-compose.hulda.yml`): senaste 2.x-version. | Inte `:latest` — den taggen står bara kvar i äldre exempel *(historiskt)* i [[MQTT-Infrastruktur]]. |

## Miljö och process

| Term | Konnotation | Denotation |
|------|-------------|------------|
| **fakir** | Joakims dev-dator (Arch/Omarchy): bygger appen, kör ADB, MQTT Explorer och wikin. | Inte en broker; ingen MQTT körs lokalt där. |
| **ishtar** | Testlab-gästen `192.168.50.151` (på Proxmox-hypervisorn hulda): broker `rfid-mqtt-hulda` och dashboard `rfid-mqtt-dashboard`. | Inte en publik server och inte hypervisorn `192.168.50.100`. |
| **`feature/sqlite`** | Git-gren med Room/SQLite som enda lagring (commits `eab6b6a..48e8a89`), verifierad på telefonen 2026-10-03 och mergad till `main` 2026-10-03. | Inte en gren som fortfarande väntar på merge — innehållet ligger på `main`. |
| **Teknisk skuld** | Känd brist som medvetet lämnats och är noterad i backlog (README, [[Kanban]]). | Inte en okänd bugg och inte något som blockerar release. |
| **Historiskt / löst** | Märkning av text som beskriver ett tidigare läge. Texten behålls som historik; "löst" anger att problemet är åtgärdat och datum/commit. | Inte "raderat" och inte "gäller fortfarande". |

---
title: Utkorg (outbox) — generisk kärna och MQTT-utskick
tags: [outbox, utkorg, mqtt, workmanager, room, dubblettskydd, fasad, återanvändning]
created: 2026-10-03
---

# Utkorg (outbox)

> **Status:** implementerad på grenen `feature/outbox` (utgår från `feature/sqlite`, ej mergad till `main`). Verifierad med JVM-enhetstester och bygge, och **verifierad av Joakim på telefon och mot riktiga brokern 2026-10-03** (se *Verifiering* nedan). **Felvägarna** (fel lösenord, 12 försök → `FAILED`, omstart med väntande poster) är **provade och verifierade av Joakim på telefon 2026-10-03**. Termer: [[Ordlista]].

> **Ny gren `feature/outbox-rounds` (utgår från `main` `2e3ee25`, ej mergad, ej pushad till `main`):** omförsök i omgångar, se avsnittet *Omförsök i omgångar* nedan. **Status: enhetstestat (JVM) och bygger; INTE provat på telefon.**

## Varför

Tidigare markerades en avläsning som skickad oavsett om MQTT-publiceringen lyckades (`markAsTransmitted`), och inget skickades alls om telefonen var offline. Utkorgen ger: **spara först, skicka sedan**, ack-baserad status, omförsök med backoff, utskick även när appen är stängd och dubblettskydd hos mottagaren.

## Flöde

```
Avläsning ──► Room: PENDING (direkt, ingen väntan på nätverk)
                 │  triggar
                 ▼
        WorkManager (unik kö, nätverk CONNECTED)
                 ▼
        OutboxWorker ── bygger egen MQTT-anslutning från sparade inställningar
                 ▼
        OutboxDispatcher: äldst först, en i taget
                 ▼  QoS 1, väntar på PUBACK
        ack ──► SENT          fel ──► attempts+1, stannar, backoff
                              sista försöket i sista omgången ──► FAILED (manuellt 'Skicka nu')
                              omgångsslut ──► paus (Väntar), sedan nästa omgång
```

## Statusmodell

| Status | Betydelse |
|--------|-----------|
| `PENDING` (Väntar) | Sparad, ska skickas. Gäller även poster som misslyckats men inte nått max försök. |
| `SENT` (Skickad) | Brokern har bekräftat (PUBACK). Slutstatus. `sentAt` sätts. |
| `FAILED` (Misslyckad) | Max antal försök nått. Skickas inte om automatiskt; 'Skicka nu' köar om posten (försök nollställs). |

Fält per post: `attempts` (Int), `lastError` (String?), `lastAttemptAt`, `sentAt`.

**Room-schema v2** (migrering 1→2, riktig `Migration`, ingen destruktiv reserv, schema i `app/schemas/.../2.json`): fem nya kolumner `outboxStatus` (default `PENDING`), `attempts` (default 0), `lastError`, `lastAttemptAt`, `sentAt` + index `(outboxStatus, timestamp)`. Befintliga rader: `transmitted=1` → `SENT`, `transmitted=0` → `PENDING`.

**Val: kolumnen `transmitted` (och `status`) behålls** som äldre spegling. Skäl: `ALTER TABLE … ADD COLUMN` är enklast och säkrast (ingen tabellombyggnad eller dataflytt; `DROP COLUMN` kräver SQLite 3.35/Android 14 men minSdk är 24). `outboxStatus` är sanningen; `transmitted`/`status` skrivs bara av DAO:ns utkorgsfrågor så att de inte kan hamna i otakt.

> **OBS äldre data:** poster som redan stod som `transmitted=1` blir `SENT` — men p.g.a. den gamla buggen kan de aldrig ha nått brokern. Poster med `transmitted=0` blir `PENDING` och **skickas automatiskt** första gången utkorgen körs efter uppdateringen.

## Kärnan (återanvändbar) — paket `com.joakim.rfidmanager.outbox.core`

Ren Kotlin: endast `kotlin.*`, `kotlinx.coroutines.*`, `java.*` (testet `CoreHasNoPlatformImportsTest` fäller bygget om någon importerar Android, Room, Paho eller appens domän). Ingen koppling till MQTT eller `PersistedReading`. Fil för fil:

| Fil | Innehåll |
|-----|----------|
| `OutboxModel.kt` | `OutboxStatus`, `OutboxEntry<T>(id, item, createdAt, attempts, lastAttemptAt, lastError)`, `SendResult` (`Acked` / `Failed(error)`) |
| `OutboxStore.kt` | `OutboxStore<T>`: `nextPending(limit)` (endast PENDING, äldst först), `markSent`, `markAttemptFailed`, `markFailed`, `requeue(id)`, `requeueFailed()`, `countPending()` |
| `OutboxTransport.kt` | `OutboxTransport<T>`: `suspend fun send(entry): SendResult` — `Acked` först när mottagaren bekräftat |
| `BackoffPolicy.kt` | `BackoffPolicy`, `ExponentialBackoff(base=30 s, max=30 min, factor=2)`, `NoBackoff` |
| `RetryPolicy.kt` | *(grenen `feature/outbox-rounds`)* `RoundsConfig`, `RetryPolicy` — omgångar och paus ovanpå backoff |
| `OutboxDispatcher.kt` | `OutboxDispatcher<T>(store, transport, backoff, maxAttempts=12, clock).drain(force)` (på `feature/outbox-rounds` även `OutboxDispatcher(store, transport, RetryPolicy, clock).drain(force, skipPause)`) → `DrainResult.Drained(sent)` / `Blocked(sent, retryAfterMillis, error, paused)` |
| `InMemoryOutboxStore.kt` | Referensimplementation i minnet (tester, exempel) |

### Dispatcherns regler

1. Äldsta `PENDING` först, **en i taget**; `markSent` först efter `Acked`.
2. **Stannar vid första felet** (`Blocked`): posten får `attempts+1` + `lastError`; senare poster skickas aldrig förbi.
3. Efter sista omgångens sista försök (standard 12 × 3 = 36 på grenen `feature/outbox-rounds`; tidigare `maxAttempts` = 12) → `FAILED` (lämnar kön, körningen stannar ändå). Ny körning går vidare till nästa post. `requeue`/`requeueFailed` ger nytt försök.
4. Väntetid: utan `force` skickas inte en post förrän `lastAttemptAt + väntetid(attempts)` passerat (backoff, eller paus vid omgångsslut); `force = true` ('Skicka nu', nätverk tillbaka) ignorerar backoff, och `skipPause` ('Skicka nu') ignorerar även pausen.
5. **At-least-once / idempotent:** kraschar processen efter ack men före `markSent` skickas posten igen. Transporten ska därför ge mottagaren en stabil nyckel (postens id) att avduplicera på.
6. `CancellationException` kastas vidare orört (posten förblir `PENDING`); övriga undantag från transporten räknas som misslyckat försök.

## Adaptrar i appen

| Paket | Klass | Roll |
|-------|-------|------|
| `outbox.room` | `ReadingOutboxStore` | `OutboxStore<PersistedReading>` ovanpå `PersistedReadingDao` (Room) |
| `outbox.mqtt` | `MqttOutboxTransport<T>` + `MqttMessageEncoder<T>` + `MqttLink` | Generisk MQTT-transport: encodern gör post → topic + bytes, länken publicerar och väntar på ack |
| `outbox.mqtt` | `PahoMqttLink` | Paho `MqttClient`, QoS 1, `MemoryPersistence`, `getTopic().publish()` + `waitForCompletion(15 s)` (PUBACK); ansluter lazy, återanslutning vid fel |
| `outbox.mqtt` | `ReadingMqttEncoder` | Avläsning → samma JSON som förut + `id`, `deviceId`, `messageId` |
| `outbox.work` | `OutboxWorker` (`CoroutineWorker`) | Bygger databas + `AppSettings` (värd, port, användarnamn, krypterat lösenord) + egen MQTT-anslutning; kopplar ner efter körning |
| `outbox.work` | `OutboxScheduler`, `OutboxNetworkTrigger` | Köar arbete (se nedan); nätverkscallback medan appen lever |

### WorkManager

- Beroende: `androidx.work:work-runtime-ktx:2.12.0` (godkänt av Joakim 2026-10-03).
- **En unik kö** `rfid-outbox-send`, `OneTimeWork`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` (30 s).
- Triggers: ny avläsning och appstart → `APPEND_OR_REPLACE` (läggs sist, ingen avläsning missas medan en körning avslutas); 'Skicka nu' och nätverk tillgängligt → `REPLACE` med `force` (försöker direkt). Körs även när appen är stängd (kön ligger i WorkManagers databas).
- Workern använder eget MQTT-klient-id `rfid-outbox-<deviceId>-<slump>` så att den aldrig kastar ut UI:ts anslutning (`rfid-android-client`) hos brokern. Lösenordet loggas aldrig.
- Tom kö → ingen anslutning alls. Misslyckad körning → `Result.retry()`; omkörningar använder aldrig `force`.
- Två lager backoff: postnivå (dispatcher, `lastAttemptAt`) och körningsnivå (WorkManager). De är avsiktligt likartade (30 s start); WorkManager tar max 5 h, dispatcherns tak är 30 min.

## Omförsök i omgångar (gren `feature/outbox-rounds`)

> **Status (ärlig):** byggt och **enhetstestat på JVM** (28 nya tester; alla 108 gröna) samt `assembleDebug` OK. **Inte provat på telefon**, inte installerat, inte mergat. Hur WorkManager beter sig i verkligheten (schemaläggning efter pausen, överlevnad vid omstart, UI-text) är alltså **otestat på enhet**.

**Önskemål (Joakim 2026-10-03):** för arbete i fält utan täckning ska en post inte ge upp efter 12 direkta försök, utan försöka i *omgångar*: 12 försök, en timmes paus, 12 försök, en timmes paus, 12 försök, och först därefter `FAILED`.

### Mönstret

| Inställning | Standard | Gräns i appen |
|-------------|----------|---------------|
| Försök per omgång | 12 | 1–100 |
| Paus mellan omgångar (minuter) | 60 | 1–1440 |
| Antal omgångar | 3 | 1–20 |

Högst `försök × omgångar` försök (standard 36) innan `FAILED`. **Med 1 omgång är beteendet identiskt med det gamla** (backoff mellan försöken, `FAILED` efter sista försöket, ingen paus) — det bevisas av `RetryPolicyTest` och `OutboxDispatcherRoundsTest`.

Inom en omgång gäller samma exponentiella backoff som förut (30 s, 60 s … tak 30 min), men den **börjar om** i varje ny omgång. Efter sista försöket i en omgång (utom den sista) väntar posten i en **paus**.

### Designval

- **Ingen Room-migrering.** Rundnumret *härleds* ur `attempts`: försök n ligger i omgång `(n−1) / försök_per_omgång + 1`, och en omgång är slut när `attempts % försök_per_omgång == 0`. Schemat är kvar på v2 (ingen `3.json`, ingen `Migration` 2→3). Följd: ändrar man inställningarna medan poster väntar, räknas de om med de nya värdena (t.ex. sänks antal omgångar till under redan gjorda försök blir nästa misslyckade försök det sista).
- **Logiken ligger i kärnan** (`outbox/core`, ren Kotlin, ingen Android): `RoundsConfig(attemptsPerRound, pauseMinutes, rounds)` (konfiguration; kräver bara ≥ 1 — appens gränser 1–100/1–1440/1–20 hör till appen, `OutboxRoundsInput`) och `RetryPolicy(config, backoff)` med `isPause`, `isGiveUp`, `delayMillis`, `currentRound`, `nextRoundStartsAt`. `OutboxDispatcher` tar en `RetryPolicy`; den gamla konstruktorn `(store, transport, backoff, maxAttempts, clock)` finns kvar och betyder en omgång.
- **`DrainResult.Blocked` har nytt fält `paused`** (standard `false`) så att anroparen vet om väntan är en paus.
- **Skicka nu kringgår pausen** (`drain(force = true)`; `skipPause` är som standard lika med `force`). Det köar om och försöker direkt; en `FAILED`-post köas om med försök nollställda, som idag. Misslyckas ett Skicka nu under paus räknas det som första försöket i nästa omgång.
- **Nätverk som kommer tillbaka** (`onNetworkAvailable`) skickar `force = true, skipPause = false`: hoppar över backoff inom en omgång (som idag) men **inte** pausen.
- **Under paus:** posten är `PENDING` (*Väntar*), felorsaken står kvar (röd), och en rad visar *"Paus: omgång 1 av 3 klar. Nästa omgång startar kl 14:35 (om 59 min)"* (räknas om var 15:e sekund). Är pausen slut men inget nätverk finns står *"Paus slut – nästa omgång startar så snart nätverk finns"*. Med fler än en omgång visas även *"omgång 2 av 3"* på statusraden.
- **WorkManager:** när första posten står i paus planerar `OutboxWorker` nästa körning med `OutboxScheduler.scheduleAfter(resterande paus)` (`OneTimeWork` med `initialDelay`, samma unika kö, `REPLACE`, nätverkskrav `CONNECTED`) och returnerar `success` i stället för `retry`. WorkManager lagrar jobbet i sin databas, så det ska överleva omstart av appen och telefonen. Ändras inställningarna planeras körningen om (`onSettingsChanged`). Workern läser konfigurationen ur `AppSettings` vid varje körning.
- **Inställningar:** nytt kort *Utkorg – omförsök i omgångar* i Inställningar med tre numeriska fält, standardvärdet synligt under varje fält, felmeddelande på svenska vid ogiltigt värde (inget sparas då), knapparna *Spara* och *Standardvärden*. Sparas i samma `SharedPreferences` (`rfid_settings`) som övrigt: `outbox_attempts_per_round`, `outbox_pause_minutes`, `outbox_rounds`. Ogiltigt sparat värde läses som standardvärdet.

### Begränsningar (kända, ej åtgärdade)

- **Första posten blockerar köerna** (som förut): poster bakom en post i paus skickas inte förrän den är skickad eller `FAILED`. Med standardvärden kan det vara drygt två timmar.
- **Inget nätverk = inga försök räknas.** WorkManager kör bara med nätverk (`CONNECTED`). I fält helt utan täckning pausar därför hela kön av sig själv; omgångarna räknas när telefonen har nät men brokern inte svarar (t.ex. mobildata utan fungerande väg ut). Hur mycket detta spelar roll i Joakims användning är inte utrett.
- Pausens sluttid räknas från `lastAttemptAt` + paus; ändras systemklockan kan visningen bli fel.
- *Anslutning*-vyn (`MqttStatusScreen`) visar försök och felorsak men ännu ingen pausrad.
- Ej provat på telefon: UI-texten, kortet i Inställningar, `scheduleAfter` med verklig `initialDelay`, omstart under paus.

### Kalle (FASAD-172): så återanvänder du omgångarna

Kopiera även `RetryPolicy.kt` (där `RoundsConfig` också ligger) tillsammans med de övriga filerna i `outbox/core/`. Skapa dispatchern med `OutboxDispatcher(store, transport, RetryPolicy(RoundsConfig(attemptsPerRound = 12, pauseMinutes = 60, rounds = 3), ExponentialBackoff()))` — värdena är parametrar, inget är hårdkodat i Android. `drain()` returnerar `Blocked(retryAfterMillis, paused = true)` när en paus börjar: planera nästa körning om `retryAfterMillis` (hos oss WorkManager med `initialDelay`). Anropa `drain(force = true)` för ett manuellt "skicka nu" som kringgår pausen, och `drain(force = true, skipPause = false)` när du vill hoppa över backoff men vänta ut pausen. Ingen extra kolumn behövs i ditt lager: rundnumret härleds ur `attempts`. Utan `RetryPolicy` (gamla konstruktorn med `maxAttempts`) får du det gamla beteendet, en omgång. Testerna att kopiera: `RetryPolicyTest` och `OutboxDispatcherRoundsTest`.


## Dubblettskydd

Meddelandet innehåller nu:

| Fält | Exempel | Betydelse |
|------|---------|-----------|
| `id` | `1759480000123` | Postens lokala id |
| `deviceId` | `a1b2c3d4` | Slumpat id för installationen (`AppSettings.deviceId`) |
| `messageId` | `a1b2c3d4:1759480000123` | Stabil, unik nyckel — samma post har alltid samma `messageId` |

Dashboarden (`dashboard/app/dedup.py`, används i `mqtt_client.on_message`) ignorerar nycklar den redan sett: `messageId` → annars `deviceId:id` → annars `uid:id` → **meddelanden utan id räknas alltid** (bakåtkompatibelt). Minnet är en LRU på 5000 nycklar (`DEDUP_MAX_IDS`); **bara i minnet** — en omstart av dashboarden glömmer nycklarna (medveten avvägning för att hålla det litet). `/api/stats` har nytt fält `duplicates`.

## Så återanvänds kärnan i FASAD

(Beskrivning — FASAD-repot har inte rörts; Kalle PL Fasad äger det.)

1. **Kopiera** katalogen `outbox/core/` (6 filer, ~300 rader inkl. kommentarer) till FASAD:s Kotlin-kod, eller bryt ut den till en gemensam JVM-modul/artefakt senare. Byt paketnamn vid behov. Inga beroenden utöver `kotlinx-coroutines-core`.
2. **Definiera ett id** (`Long`) per post i FASAD:s domän och skriv en **`OutboxStore<T>`**: en tabell/fil med status (`PENDING`/`SENT`/`FAILED`), `attempts`, `lastError`, `lastAttemptAt` och tidsordning. Se `InMemoryOutboxStore` som facit för semantiken, och `ReadingOutboxStore` + DAO-frågorna för Room.
3. **Välj transport:**
   - MQTT: återanvänd `MqttOutboxTransport<T>` + `PahoMqttLink` och skriv en `MqttMessageEncoder<T>` (topic + payload; ta med en stabil `messageId`).
   - Annat (HTTP, kö …): implementera `OutboxTransport<T>`; returnera `Acked` först när mottagaren bekräftat.
4. **Kör**: `OutboxDispatcher(store, transport).drain()` från valfri utlösare (WorkManager, tjänst, cron). Tolka `DrainResult.Blocked` som "försök igen om `retryAfterMillis`".
5. **Mottagaren** måste avduplicera på den stabila nyckeln (se Dubblettskydd).
6. **Tester:** kopiera `OutboxDispatcherTest` och `CoreHasNoPlatformImportsTest` — de använder bara `InMemoryOutboxStore`.

Begränsningar att känna till: id är `Long`; en post i taget (ingen batch); `drain` är inte trådsäker mot parallella `drain` på samma lager (serialisera i anroparen — i appen gör WorkManagers unika kö det).

## Verifiering

| Vad | Hur | Status |
|-----|-----|--------|
| Dispatcher: ordning, backoff, stopp vid fel, max försök, requeue, ingen dubbelsändning, avbrott, krasch före `markSent` | JVM-enhetstest (`OutboxDispatcherTest`) | ✅ |
| Kärnan utan plattformsimporter | `CoreHasNoPlatformImportsTest` | ✅ |
| Room-adapter (mappning, ordning, status, housekeeping skonar PENDING) | JVM-test mot fejk-DAO (`ReadingOutboxStoreTest`) — **testar inte själva SQL-frågorna** | ✅ (begränsat) |
| MQTT-transport, payload, messageId, kedja offline→online | JVM-test mot fejk-länk (`MqttOutboxTransportTest`) — **inte mot riktig Paho/broker** | ✅ (begränsat) |
| Migrering 1→2: SQL konsistent med `1.json`/`2.json` | `Migration1To2SqlTest` (JVM, kör inte SQL) | ✅ |
| Migrering 1→2: SQL körd mot riktig SQLite, rätt status på befintliga rader | `RFIDManager/tools/verify_migration_1_2.py` (Python `sqlite3`) | ✅ — men **ingen** `MigrationTestHelper` (kräver instrumented test på enhet/emulator) |
| Dashboard-dubblettskydd | `python3 -m unittest` i `dashboard/` | ✅ |
| Offline → *Väntar* → skickas när nätverk finns (även med appen stängd); WorkManager, Paho mot riktig broker, nätverkstrigger | Manuellt på telefonen mot riktiga brokern på ishtar, Joakim 2026-10-03 | ✅ verifierad på enhet |
| Alla avläsningar kommer fram **en gång var** på dashboarden, status *Skickad* | Manuellt, Joakim 2026-10-03 | ✅ verifierad på enhet |
| Room-migrering 1→2 på telefonens riktiga databas | Körd på telefonen, Joakim 2026-10-03: 4 poster blev `SENT` | ✅ verifierad på enhet (fortfarande ingen `MigrationTestHelper`) |
| Dashboard med dubblettskydd driftsatt på ishtar | `c16ef43`; backup `~/backup-dashboard-20261003-115215.tar` på ishtar | ✅ driftsatt 2026-10-03 |
| Fel lösenord → posten förblir *Väntar* med felorsak och växande *Försök* | Manuellt på telefonen mot riktiga brokern, Joakim 2026-10-03 (röd felorsak "Not Authorised to connect") | ✅ verifierad på enhet |
| 12 misslyckade försök → status `FAILED`, *Skicka nu* köar om | Manuellt, Joakim 2026-10-03: posten blev *Misslyckad* (röd), nästa post gick igenom samma process; efter rätt lösenord och *Skicka nu* blev båda Misslyckade posterna *Skickade* | ✅ verifierad på enhet |
| Telefon omstartad (flygplansläge på) medan poster väntade → skickas när nätet slås på, utan att appen öppnas | Manuellt, Joakim 2026-10-03: alla väntande poster skickades av sig själva inom ett par minuter | ✅ verifierad på enhet |
| Omgångar: gränser, paus, `FAILED` först efter sista omgången, Skicka nu, nätverk tillbaka, standardvärden, en omgång = gammalt beteende; validering 1–100/1–1440/1–20 | JVM-enhetstest (`RetryPolicyTest`, `OutboxDispatcherRoundsTest`, `OutboxRoundsInputTest`, 28 nya; totalt 108 gröna) på `feature/outbox-rounds` | ✅ enhetstestat |
| Omgångar: WorkManager `initialDelay` efter pausen, överlevnad vid omstart, pausraden och inställningskortet i UI | — | ❌ **ej provat på telefon** |

## Manuell test på telefonen

*Steg 1–5 är genomförda av Joakim 2026-10-03 med lyckat resultat (se Verifiering). Steg 6 (felvägar) är också provat och verifierat av Joakim på telefon 2026-10-03 (inkl. omstart med väntande poster).*

1. Installera bygget (Joakim). Starta appen, öppna Inställningar → anteckna *Väntande i utkorgen* (äldre poster med `transmitted=0` skickas nu automatiskt).
2. Stäng av wifi/mobildata. Skanna tre taggar och spara dem. De visas som **Väntar**; *Väntande i utkorgen* ökar med 3.
3. (Valfritt) Stäng appen helt.
4. Slå på nätet igen. Inom några sekunder (eller tryck **Skicka nu**) ska posterna bli **Skickad**.
5. På dashboarden ska alla tre dyka upp **en gång var**. Skicka nu igen på en redan skickad post är avstängt; omsändning ger inga dubbletter (`/api/stats` → `duplicates`).
6. Fel vid skick (t.ex. fel lösenord i Inställningar) → posten förblir **Väntar** med felorsak och växande *Försök*; efter 12 försök **Misslyckad** → **Skicka nu** köar om den.

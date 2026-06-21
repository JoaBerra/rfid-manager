---
title: Kundrelationer och Acceptans
tags: [kund, uat, acceptans, sign-off, godkännande, projektstyrning, governance, roller]
created: 2026-06-07
---

# Kundrelationer och Acceptans – RFID Manager

> **Syfte**: Skapa en tydlig, tidsstämplad och spårbar dokumentation av relationen till Kund, utförda UAT-tester i kundrollen samt formella godkännanden (sign-off) per fas eller leverans.  
> Detta är **Single Source of Truth** för vad som har accepterats av Kund och under vilka förutsättningar.

Denna sida kompletterar [[Rollfördelning-och-Arbetsätt]] där rollerna **Kund / Product Owner** och **UAT-testare & Slutanvändare** är explicit definierade och ägda av dig.

## Roller gentemot Kund

| Roll                        | Ägare     | Ansvar i relation till Kund                          |
|-----------------------------|-----------|-------------------------------------------------------|
| **Kund / Product Owner**    | Du (Joa)  | Krav, acceptanskriterier, prioritering, UAT           |
| **UAT-testare & Slutanvändare** | Du (Joa) | Manuell acceptanstest på riktig hårdvara i verklig miljö |
| **Projektledare**           | Du (Joa)  | Scope, faser, Go/No-go-beslut, sign-off               |
| **Domänexpert**             | Du (Joa)  | Industriell kontext (eskortminnen, processer, säkerhet) |
| **Programmerare m.fl.**     | Grok      | Tekniskt genomförande + dev-test (stödjer UAT)        |

Se full RACI och Working Agreement i [[Rollfördelning-och-Arbetsätt]].

**Viktigt signalord**: När du säger "Jag agerar kund nu" eller "Detta var ett UAT-test" så är det en explicit rollväxling som dokumenteras här.

## Arbetsätt för UAT och Formella Godkännanden

1. **Förberedelse** — Grok (i rollerna Testare/DevOps/Programmerare) bygger, testar internt, fixar blockerare och dokumenterar i wiki + buggrapporter.
2. **UAT-utförande** — Du kliver in i Kund-rollen och testar på fysisk enhet (Samsung Note 10 etc.). Du noterar vad som fungerar, vad som inte gör det, och ger affärsmässig feedback.
3. **Dokumentation** — Resultat, loggar, referenser till buggrapporter och kod läggs in här eller i länkade sidor.
4. **Sign-off** — Du (som Kund/PL) ger ett formellt godkännande eller "godkänd med öppna punkter". Detta tidsstämplas och signeras i sektionen nedan.
5. **Fas-gate** — Godkännandet blir underlag för beslut om nästa fas eller iteration.

All dokumentation är **append-only** eller versionshanterad via log.md för maximal spårbarhet (viktigt vid second opinion, framtida teammedlemmar eller revision).

## Sign-off Logg

### [2026-06-07] UAT | Fas 2 godkänd av Kund

**Datum:** 2026-06-07  
**Tid / Kontext:** Slutligt UAT-test utfört ~09:33 (Logcat-tid). Godkännande efter full verifiering av end-to-end-flöde samma dag.

**Utfört av:** Kund (Joa) i egenskap av **UAT-testare & Slutanvändare** + **Projektledare**

**Fas / Leverans:**  
**Fas 2** – Lokal persistens av RFID-läsningar + MQTT/Sparkplug-kommunikation (telemetri till extern mottagare)

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS, internationell Exynos)
- App: RFID Manager (debug build byggd i Android Studio, targetSdk 36)
- Nätverk: Lokal Wi-Fi, broker på 192.168.50.107:1883 (Docker eclipse-mosquitto)
- Valideringssida: Python subscriber (`test_subscriber_persist.py`) som lyssnar på `rfidmanager/+/telemetry` och persisterar till SQLite

**Omfattning – Vad testades i UAT (Kund-perspektiv):**
- "Persist after write" på NFC Write (läsningar sparas lokalt i appen)
- PersistedReadings-lista med status, metadata och Transmit ↑-knapp
- Transmit-flöde: App → MqttSender → rå TCP MQTT → broker
- Meddelandeformat (Sparkplug-liknande): `type`, `uid`, `timestamp`, `source`, `sparkplug: true`, `data` med `memoryBank`, `address`, `length`, `payload` (hex)
- Topic: `rfidmanager/<uid>/telemetry`
- End-to-end validering: Subscriber tar emot meddelandet korrekt och sparar det i testdatabasen
- Hantering av tidigare blockerare (EPERM på Samsung debug builds)

**Kritiskt validerat testfall (09:33 samma dag):**
- UID: `047B05CA885884`
- memoryBank: 3, address: 4, length: 4, payload: `74655400`
- Logcat (MqttSender):
  - "Attempting MQTT connect/publish..."
  - "Connected to MQTT broker"
  - "Published to rfidmanager/047B05CA885884/telemetry : { ... }"
  - "=== SEND COMPLETE for uid=... ==="
- Subscriber-sida: "Received: ReadEscortMemory from topic ... Persisted to SQLite (Sparkplug-aware)."

**Resultat:**
UAT godkänt. Alla kritiska flöden för Fas 2-plattformen fungerade som förväntat på riktig hårdvara efter nödvändiga korrigeringar (manifest + network security config).

Se full historik, alla tidigare försök, Samsung-inställningar och Logcat i:
[[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] (inkl. genererad PDF för second opinion)

**Öppna punkter / Kvar i Fas 2 vid godkännande:**
- EAN-streckkodsläsning via kamera (enligt ursprunglig Fas 2-mål)
- ViewModel-refaktor för bättre state-hantering (istället för direkt i Screen)
- Reaktivera riktig Room-databas (KSP) istället för in-memory fallback
- Ytterligare polish, felhantering och UX-förbättringar
- Kryptering: **Arkitekturbeslut** – Produktion skall använda krypterad kommunikation (MQTT over TLS). Under utveckling är okrypterad (tcp://) godkänd.

**Godkännande:**

**UAT godkänd av Kund för Fas 2.**

Fas 2-leveransen (lokal persistens + MQTT/Sparkplug-meddelanden) anses komplett och accepterad i sin kärna. Projektet kan fortsätta med återstående arbetsuppgifter i Fas 2 samt planering av Fas 3.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-07

**Referenser:**
- Buggrapport (kanonisk källa + PDF): `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md`
- Rollfördelning: [[Rollfördelning-och-Arbetsätt]]
- Arkitekturbeslut (kryptering): [[App-Architecture]]
- Teknisk översikt: [[Fas2-Implementation-Overview]]
- Kronologisk logg: [[log]]

---

## Mall för framtida UAT / Sign-off

Kopiera och fyll i nedanstående struktur vid varje formellt godkännande.

```markdown
### [YYYY-MM-DD] UAT | <Fas / Leverans> – <Godkänd / Godkänd med anmärkning / Ej godkänd>

**Datum:** ...  
**Tid / Kontext:** ...

**Utfört av:** Kund (...) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** ...

**Testmiljö:** ...

**Omfattning:**  
- ...

**Resultat:** ...

**Öppna punkter vid godkännande:**  
- ...

**Godkännande:**  
UAT godkänd / ... av Kund för ...

**Signerat av Kund:** ___________________________   **Datum:** YYYY-MM-DD

**Referenser:** ...
```

## Rekommendationer för framtida arbete

- Varje ny Fas eller större leverans börjar med kick-off: Lås acceptanskriterier här + i [[Produkt-Roadmap]].
- UI-andrum: Explicit "breathing room check" innan kod (dedikerade vyer, spacing-regler, ingen trängsel). Vi använder befintliga Figma-mocks + textspecar (nomenclature + markdown) för att spara bildkrediter. Nya bilder bara för kritiska valideringar.
- Efter varje UAT/sign-off: Kort fas-retro (5–10 min) – vad fungerade i samarbetet? Vad ändra till nästa fas?
- Använd alltid explicit roll ("Som Kund godkänner jag...").
- Länka alltid till teknisk dokumentation + eventuella buggrapporter.
- Använd standardiserad path-struktur `~/projects/rfid/rfid-manager/` för test, setup, releases och artifacts.
- Uppdatera denna sida + `log.md` + [[Produkt-Roadmap]] + relevant översiktssida vid varje sign-off.
- Fas-specifika taggar (t.ex. `fas-3-ui-polish-...`) + artifacts under `~/projects/rfid/rfid-manager/releases/`.

## Fas 3 Kick-off Check (driven by Lead, 2026-06-07)

**Syfte:** Formell start av Fas 3 enligt våra arbetssätt. Lås scope, metoder, arkitektur, specifikationer och acceptanskriterier innan kodning börjar. Detta är en "phase kick-off" som komplement till den slutliga UAT-sign-offen.

**Datum:** 2026-06-07  
**Deltagare:** Kund (Projektledare + Domänexpert) + Lead (Arkitekt / Technical Lead / Programmerare)

### 1. Scope & Roadmap-bekräftelse
- Fas 3 fokus (från [[Produkt-Roadmap]]): UI-reallokering (dedikerade vyer + bottom nav), breathing room / spacing, ViewModel-refaktor, reaktivera riktig Room (KSP), grundläggande polish.
- Övrigt i Fas 3 (låg risk): Förbättrad loggning i test-subscriber + dokumentation av MQTT Explorer.
- EAN och avancerad PC-GUI: Låst till Fas 4+.
- Bekräftat: Inget scope creep. Huvudmålet är att appen ska kännas luftig och professionell.

**Status:** Godkänt av Kund.

### 2. Arbetssätt för Fas 3 (bekräftelse av metoder)
Följande arbetssätt gäller explicit under Fas 3 (formaliserat i [[Rollfördelning-och-Arbetsätt]] och denna sida):

- Levande Roadmap som nav – kick-off + löpande uppdatering.
- UI-andrum som explicit regel – "breathing room check" innan kod (textspecar + befintliga mocks för att spara bildkrediter; nya bilder endast vid kritisk validering).
- Kort fas-retro efter UAT/sign-off.
- Fas-specifika taggar (t.ex. `fas-3-ui-polish-...`) + artifacts under `~/projects/rfid/rfid-manager/releases/`.
- Standardiserad path-struktur: Allt stödmaterial (test, setup, releases, artifacts) under `~/projects/rfid/rfid-manager/`. Alla referenser pekar dit.

**Status:** Godkänt av Kund. Lead driver efterlevnad.

### 3. Arkitekturbeslut (låsta)
- Navigation: `androidx.navigation.compose` + bottom nav med dedikerade destinations.
- State management: ViewModel per vy + lätt AppViewModel för delad state.
- Se detaljer i [[App-Architecture#arkitekturbeslut-för-fas-3-låsta-2026-06-07]] och [[Fas3-Navigation-Spacing-Design]].

**Status:** Låsta och godkända av Kund.

### 4. Specifikationer (utvecklade först)
- Huvudspec för navigation + spacing: [[Fas3-Navigation-Spacing-Design]] (textbaserad, använder de tre befintliga referensbilderna + nomenclature).
- Innehåller: Bottom nav-struktur (4 items), destinations, hur spacing-regler implementeras i Compose (16 dp padding, 12 dp spacing etc.), filändrings-skiss och kodexempel.
- Specifikationerna är textdrivna för att respektera bildkredit-begränsning. Vi itererar med markdown-tabeller, kodexempel och referens till befintliga mocks.

**Status:** Specifikationer klara och granskade av Lead. Kund har tillgång till dem.

### 5. Fas 3-acceptanskriterier (uppdaterade)
Se detaljerat utkast nedan (baserat på specifikationerna). Kriterierna är mätbara och kopplade till UAT på enhet.

**Status:** Uppdaterade. Kund granskar och godkänner.

### 6. Risker & öppen punkter
- KSP/Room-processor: Dual-mode behålls som fallback under utveckling.
- Scope creep: Lead flaggar omedelbart vid förslag utanför UI-reallokering + polish.
- PC-stöd: Hålls lättviktigt (logg + MQTT Explorer-dokumentation).

**Status:** Dokumenterade. Inga blockerare för kick-off.

### 7. Kick-off godkännande
- Scope, metoder, arkitektur, specifikationer och acceptanskriterier godkända för Fas 3.
- Lead får mandat att driva spec → kriterier → kodning → UAT.
- Nästa: Efter detta kick-off börjar kodning (när kriterierna är låsta).

**Godkännande från Kund (2026-06-07):**
"I egenskap av Kund godkänner jag kick-offen. Jag godkänner också att acceptanskriterierna för Fas 3 kan användas vid UAT av mig (Kund)."

**Signerat av Kund:** Kund (Joa)   **Datum:** 2026-06-07

**Signerat av Lead:** Grok (Arkitekt / Technical Lead / Programmerare)   **Datum:** 2026-06-07

**Beslut dokumenterat:** Kick-offen är godkänd. Fas 3-acceptanskriterierna är officiellt godkända för användning i UAT av Kund. Lead driver nu implementeringen framåt enligt låst scope och design.

**Uppdaterat:** 2026-06-07 – Kundens explicita godkännande infört. Nästa steg: Implementering påbörjas (se log.md och [[Fas3-Implementation-Plan]] för detaljer).

---

## Utkast: Fas 3-acceptanskriterier (2026-06-07)

### [2026-06-XX] UAT | Fas 3 – UI-reallokering, andrum och teknisk grundpolish

**Datum:** ...  
**Tid / Kontext:** ...

**Utfört av:** Kund (...) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 3 – UI-reallokering + andrum + ViewModel + riktig Room (baserat på öppna punkter från Fas 2-godkännande)

**Testmiljö:** 
- Samsung Galaxy Note 10 (SM-N970F) eller motsvarande NFC-enhet.
- Samma test-MQTT-miljö som i Fas 2 (`~/projects/rfid/rfid-manager/test/fas2-mqtt/` + MQTT Explorer för inspektion).

**Omfattning (baserat på spec i [[Fas3-Navigation-Spacing-Design]]):**
- Reallokering av funktionalitet från de tre trånga gränssnitten (huvudskärm med tabs, Persisted list, MQTT status) – se referensbilder i `~/projects/rfid/rfid-manager/artifacts/` (fas2-main-rfid-screen.jpg, fas2-persisted-readings-list.jpg, fas2-mqtt-sparkplug-status.jpg) och [[Figma-Design-Spec-Fas2]].

  **Navigation (konkreta acceptanskriterier):**
  - Bottom navigation bar med exakt 4 items: "Scan" (huvudskärm), "Readings", "Connectivity", "Settings".
  - Från huvudskärmen (live scanning + Radar + StatCards) navigerar "Readings" till en **dedikerad fullskärmsvy** PersistedReadingsScreen (ingen TabRow som blandar READ/WRITE/PERSISTED).
  - "Connectivity" navigerar till en **dedikerad MqttStatusScreen** (status, logg, test publish, connection details) – antingen via bottom nav eller knapp i header.
  - Huvudskärmen har max en enkel action-rad eller knapp för "Readings" / "Connectivity" – ingen trång TabRow.
  - Back-navigation och state bevaras korrekt mellan vyer (använd ViewModel för navigation state).
  - Inga modala sheets som blockerar huvudflödet onödigt; använd dem bara för detaljer (t.ex. full JSON eller Transmit-bekräftelse).

  **Spacing / Breathing room (konkreta acceptanskriterier, "breathing room check" – se detaljerad implementation i design note):**
  - Minst 16 dp padding inuti alla Cards och PersistedListItem.
  - Minst 12 dp vertikalt mellanrum mellan PersistedListItem-rader i listor.
  - RadarView på huvudskärmen tar max 35–40 % av skärmhöjden (inte dominant).
  - StatCards i 2x2 grid med minst 8–12 dp mellanrum.
  - Text har line-height ≥ 1.4 för läsbarhet.
  - Filter i Persisted-listan som chips (inte tighta tabs) med minst 8 dp mellan chips.
  - Empty states har generös padding (minst 32 dp runt ikon + text).
  - Inga överlappande element, tillräckliga touch targets (minst 48 dp höjd/bredd).
  - Cards har tydlig separation (elevation/shadow eller border) så listor inte känns "ihopklämda".
  - På huvudskärmen: Radar + StatCards + action-rader har tydliga sektioner med minst 16 dp mellan sektioner.

- Tekniskt:
  - ViewModel-refaktor för bättre state-hantering (inkl. navigation state).
  - Reaktivera riktig Room (KSP) istället för in-memory fallback.
- Grundläggande polish: Empty states, loading indicators, error handling, accessibility, konsekvent nomenclature från Fas 2.
- PC-stöd (lättviktigt): Förbättrad loggning i test-subscriber + dokumenterad rekommendation av gratisverktyget MQTT Explorer för transaktionsinspektion.
- EAN och avancerad PC-GUI skjuts till Fas 4+ (enligt överenskommelse).

**Resultat:**
- Appen känns luftig och professionell på enheten.
- Användaren kan navigera mellan live scanning, historik och anslutningsstatus utan att uppleva trängsel.
- Kodbasen är renare och lättare att underhålla.

**Öppna punkter vid godkännande:**
- (Fylls i efter UAT)

**Godkännande:**  
UAT godkänd / Godkänd med anmärkning / Ej godkänd av Kund för Fas 3.

**Signerat av Kund:** ___________________________   **Datum:** YYYY-MM-DD

**Referenser:**
- [[Produkt-Roadmap#fas-3-plan-ui-förbättringar--grundläggande-polish]]
- [[Fas3-Navigation-Spacing-Design]] (full design note med låsta beslut, navigation-struktur och kodskiss)
- [[Fas2-Implementation-Overview]] (öppna punkter från Fas 2)
- Figma-mocks i `~/projects/rfid/rfid-manager/artifacts/` och releases (fas2-main-rfid-screen.jpg, fas2-persisted-readings-list.jpg, fas2-mqtt-sparkplug-status.jpg)
- [[Nomenclature-Figma-Android]] och [[Figma-Design-Spec-Fas2]]
- [[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] (Resolved)
- Ny path-struktur: `~/projects/rfid/rfid-manager/`

---

## Fas 3 UAT-test — 2026-06-10

**Testare:** Kund (Joa)  
**Lead:** AI-assistenten  
**Miljö:** Samsung Galaxy Note 10 (SM-N970F), Android, lokal MQTT-broker

### Testresultat

| Nr | Testfall | Status | Anmärkning |
|---|---|---|---|
| 1 | Navigation – 4 vyer | ✅ | Alla fungerar |
| 2 | Scanning – loading bar + tag-detektion | ✅ | Fungerar |
| 3 | Connectivity – status (grön) | ✅ | SPARKPLUG CONNECTED visas |
| 4 | Transmit-knapp i Connectivity | ❌ | Fanns som "Test Publish Reading" – otydligt |
| 5 | Transaktioner i Connectivity | ❌ | Visade bara demo-data, inte riktiga readings |
| 6 | Readings – kort visas | ✅ | Fungerar |
| 7 | Settings – info visas | ✅ | Snyggt |
| 8 | Persistens över omstart | ✅ | Fungerar |
| 9 | Rotation | ✅ | Layouten roterar |
| 10 | Radar sweep | ❌ | Statisk, sveper inte |
| 11 | "GO TO READINGS"-knapp i Scan | ❌ | Redundant (bottom nav finns) |
| 12 | JSON-indikator i Readings | 🟡 | Bör bara visas i Settings |
| 13 | Sortering Readings | 🟡 | Senaste ska vara överst |
| 14 | Connectivity-kort visa bara UID | 🟡 | Bör visa mer data |

### Godkända ändringskrav (UAT-punkter, införs omedelbart)

Nedanstående punkter är godkända av Kund (Joa) att införas innan Fas 3 sign-off:

| # | Var | Krav | Beslut |
|---|---|---|---|---|
| UAT-1 | Scan – Radar | Animera sweep-linjen när scanning är aktiv | ✅ Införd |
| UAT-2 | Scan – "GO TO READINGS" | Ta bort knappen (redundant med bottom nav) | ✅ Införd |
| UAT-3 | Scan – "Detected" | Byt till "SCANNED TAGS" eller gör tydligare | ✅ Införd |
| UAT-4 | Readings – JSON-indikator | Ta bort — räcker i Settings | ✅ Införd |
| UAT-5 | Readings – Sortering | Senast skannad överst (timestamp descending) | ✅ Införd |
| UAT-6 | Connectivity – Transmit-knapp | Byt namn till "Transmit" | ✅ Införd |
| UAT-7 | Connectivity – Transaktionskort | Visa alla readings med status (Pending/Transmitted) + tidsstämpel, typ, dataPreview | ✅ Införd |
| UAT-8 | Connectivity – "LAST TRANSMITTED" | Ta bort sektionen (demo-JSON) | ✅ Införd |
| UAT-9 | Connectivity – "Transmit"-knapp | Ta bort (demo-knapp, Transmit sker från Readings) | ✅ Införd |
| UAT-10 | Connectivity – "RECENT MQTT MESSAGES" | Ta bort sektionen + demo-data i ViewModel | ✅ Införd |

**Alla 10 UAT-punkter införda.** ✅

---

## Fas 3 Sign-off — 2026-06-10

**Kundens beslut:** Fas 3 godkänns i sin helhet.

| Kriterium | Status |
|---|---|
| Navigation (4 vyer, bottom nav) | ✅ |
| ViewModels per vy | ✅ |
| Spacing / breathing room (Dimens) | ✅ |
| Room/JSON-fallback persistens | ✅ |
| Polish (empty states, loading, error) | ✅ |
| PC-stöd (subscriber + MQTT Explorer-dok) | ✅ |
| Alla 10 UAT-punkter införda | ✅ |

**Signerat av Kund:** Joa (muntligt godkännande i chatt 2026-06-10)

**Nästa steg:** Synka release + push till GitHub. Planera Fas 4.

---

## [2026-06-11] UAT | Fas 4, Punkt 4.2 (Textstorlek / Font size-slider) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.2 — Font size-slider för transaktionsdata

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build `installDebug` från Android Studio)
- Installationsmetod: USB-anslutning via adb

**Omfattning:**
- Font size-slider i Settings (intervall 1.0×–1.8×)
- Slidern påverkar textstorlek i Connectivity (MqttStatusScreen)
- Slidern påverkar textstorlek i Readings (PersistedListItem i MainScreenHost)
- Slidern påverkar textstorlek i Scan (detected tag-kort)
- Inställningen sparas över app-omstart

**Uppmätta steg (9 diskreta positioner):** 100%, 110%, 120%, 130%, 140%, 150%, 160%, 170%, 180%

**Resultat:**
- Slidern fungerar i alla tre vyer (Scan ✅, Readings ✅, Connectivity ✅)
- 0.7–0.9 borttagna enligt feedback (för små)
- Samtliga 9 steg i intervallet 1.0–1.8 användbara

**Godkännande:**
Punkt 4.2 (Textstorlek / Font size-slider) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#42-textstorlek-font-size-slider]]
- `AppSettings.kt` — SharedPreferences + StateFlow för font size
- `SettingsScreen.kt` — slider-UI
- `MqttStatusScreen.kt` — font scale applicerad på reading-kort
- `PersistedListItem.kt` — font scale applicerad på alla text-element
- `ScanScreen.kt` — font scale applicerad på tag-kort
- `MainScreenHost.kt` — `fontSizeScale` vidarebefordrad till alla vyer

---

## [2026-06-11] UAT | Fas 4, Punkt 4.3 (Sök/filtrering i Readings) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.3 — Sök/filtrering i Readings

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)
- Förberedda testdata: flera persisterade avläsningar med olika UID-koder

**Omfattning:**
- Sökfält i Readings-menyn (mellan rubrik och filter-chips)
- Realtidsfiltrering på UID/kod när användaren skriver
- Stöd för wildcards: `*` (godtycklig text) och `?` (ett tecken)
- Icke-wildcard-tecken escapes och tolkas som literala
- Filter via type-chips (All/RFID/EAN) i kombination med sök

**Testresultat:**
- Sökfält syns och är användbart ✅
- Plain text-sökning fungerar ✅
- Wildcard `*` fungerar (t.ex. `04*84`) ✅
- Wildcard `?` fungerar (t.ex. `?BC`) ✅
- Kombination med type-filter fungerar ✅
- Inga krascher vid ogiltiga mönster ✅

**Godkännande:**
Punkt 4.3 (Sök/filtrering i Readings) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#43-sökfiltrering-i-readings-förslag]]
- `ReadingsViewModel.kt` — `searchQuery` StateFlow + `combine` för realtidsfiltrering
- `MainScreenHost.kt` — `OutlinedTextField` med sökikon

---

## [2026-06-11] UAT | Fas 4, Punkt 4.1 (Lokaliseringssystem / i18n) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.1 — Lokaliseringssystem (i18n)

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)

**Omfattning:**
- JSON-filer per språk (`strings_sv.json`, `strings_en.json`) i assets
- `LocalizationManager` som laddar JSON och exponerar `StateFlow<Map<String, String>>`
- `CompositionLocal` för åtkomst från alla composables
- `str(key)` helper-funktion i hela UI:t
- Språkväljare i Settings (FilterChips: Svenska / English)
- Byte sker omedelbart utan omstart
- Alla skärmar migrerade: Scan, Readings, Connectivity, Settings, PersistedListItem

**Testresultat:**
- Svenska visas som standard ✅
- Växla till English → alla texter uppdateras direkt ✅
- Växla tillbaka till Svenska ✅
- Settings behåller språkväljare ✅
- Inga trunkerade eller ofullständiga strängar ✅
- Inställningen sparas över omstart ✅

**Godkännande:**
Punkt 4.1 (Lokaliseringssystem / i18n) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#41-lokaliseringssystem-i18n]]
- `assets/strings_sv.json` — Svenska lexikon (70+ nycklar)
- `assets/strings_en.json` — Engelska lexikon (70+ nycklar)
- `data/localization/LocalizationManager.kt` — JSON-laddning + StateFlow
- `ui/Localized.kt` — `LocalLocalization` + `str()` helper
- `AppContainer.kt` — `localizationManager` initieras
- Alla screens migrerade till `str()`

---

## [2026-06-11] UAT | Fas 4, Punkt 4.4 (Export CSV/JSON) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.4 — Export CSV/JSON

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)
- Förberedda testdata: flera persisterade avläsningar

**Omfattning:**
- Fil på cache via FileProvider + Android Share Sheet
- CSV-export med kolumner: UID, Type, Timestamp, Source, DataPreview, Transmitted, MemoryBank, Address, Length, Payload
- JSON-export med samma fält som objekt-array
- Knappar i Settings (Export CSV / Export JSON)
- Knapparna disabled om inga readings finns

**Testresultat:**
- CSV-export → Share Sheet → sparad ✅
- JSON-export → Share Sheet → sparad ✅
- Båda formaten innehåller korrekta datafält ✅
- Knapparna disabled när listan är tom ✅

**Godkännande:**
Punkt 4.4 (Export CSV/JSON) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#44-export-csvjson-förslag]]
- `data/export/ReadingExporter.kt` — CSV/JSON-generering + FileProvider-sharing
- `res/xml/file_paths.xml` — cache-path för FileProvider
- `AndroidManifest.xml` — `<provider>`-deklaration
- `SettingsScreen.kt` — export-knappar med repository-data

---

## [2026-06-11] UAT | Fas 4, Punkt 4.5 (Haptic + ljud vid scan) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.5 — Haptic + ljud vid scan

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)

**Omfattning:**
- Kort vibration (80 ms) när tagg detekteras
- Pip-ljud (880 Hz, 100 ms) via SoundPool när tagg detekteras
- Toggles i Settings: Vibration on scan / Sound on scan (På/Av)
- Inställningarna sparas i SharedPreferences
- Haptic var redan implementerat men nu kopplat till inställning
- Ljudfil: `res/raw/beep.wav` (genererad med Python)

**Testresultat:**
- Vibration vid scan ✅
- Pip-ljud vid scan ✅
- Båda toggles fungerar (På → feedback, Av → tyst) ✅
- Inställningarna sparas över omstart ✅

**Godkännande:**
Punkt 4.5 (Haptic + ljud vid scan) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#45-haptic--ljud-vid-scan-förslag]]
- `res/raw/beep.wav` — 880 Hz pip, 100 ms
- `data/settings/AppSettings.kt` — `hapticEnabled` + `soundEnabled` StateFlows
- `nfc/AndroidNfcManager.kt` — `playBeep()` + villkorlig `vibrate()`
- `ui/screens/SettingsScreen.kt` — Switch-komponenter för haptik + ljud

---

## [2026-06-11] UAT | Fas 4, Punkt 4.6 (Riktig MQTT-anslutning) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment + logcat-verifiering

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.6 — Riktig MQTT-anslutning

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)
- Broker: 192.168.50.107:1883 (Docker eclipse-mosquitto)
- MQTT-klient: Paho 1.2.5

**Omfattning:**
- `MqttConnectionManager` — persistent MQTT-anslutning som startas vid app-start
- StateFlows: `connectionStatus` ("CONNECTED" / "DISCONNECTED") och `lastHeartbeat` (tidsstämpel)
- `MqttSender` använder delad anslutning från MqttConnectionManager (istället för egen connect)
- `ConnectivityViewModel` — demo-data borttagen, hämtar status + heartbeat från MqttConnectionManager
- `MqttStatusScreen` — visar riktig anslutningsstatus med grön/röd badge
- Automatisk återanslutning var 35:e sekund om broker är otillgänglig
- Keep-alive-kontroll var 30:e sekund

**Testresultat (logcat):**
```
I MqttSender: MqttSender initialized with shared MqttConnectionManager
I MqttConnectionManager: Connected to tcp://192.168.50.107:1883
D MqttConnectionManager: Keep-alive OK
```

**Godkännande:**
Punkt 4.6 (Riktig MQTT-anslutning) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#46-riktig-mqtt-anslutning-förslag]]
- `data/mqtt/MqttConnectionManager.kt` — persistent connection + StateFlows + reconnection
- `data/mqtt/MqttSender.kt` — använder delad MqttConnectionManager
- `di/AppContainer.kt` — `mqttManager` initieras
- `ui/viewmodel/ConnectivityViewModel.kt` — demo-data bort, läser från MqttConnectionManager
- `ui/screens/MqttStatusScreen.kt` — tar emot ConnectivityViewModel som parameter
- `ui/MainScreenHost.kt` — skapar ConnectivityViewModel med mqttManager
- `MainActivity.kt` — skickar `appContainer.mqttManager` till MainScreenHost

---

## [2026-06-11] UAT | Fas 4, Punkt 4.7 (Dark mode-toggle) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.7 — Dark mode-toggle

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)

**Omfattning:**
- `AppSettings.kt` — `ThemeMode` enum (LIGHT/DARK) + StateFlow + persistens
- `SettingsScreen.kt` — Switch-komponent (På = mörkt, Av = ljust)
- `RFIDManagerTheme` — tar `themeMode` istället för `darkTheme: Boolean`, styr statusbar-ikonfärg
- `MainActivity.kt` — skickar `settings.themeMode` till `RFIDManagerTheme`
- **Light/dark-migrering:** Alla skärmar (MainScreenHost, MqttStatusScreen, ScanScreen, SettingsScreen) migrerade från hårdkodade `Color.kt`-värden till `MaterialTheme.colorScheme.*` för att respektera ljust/mörkt tema
- `LightColorScheme` i Theme.kt uppdaterad med riktiga ljusa färger (vit bakgrund, mörk text)

**Testresultat:**
- Mörkt läge: identiskt utseende som innan ✅
- Ljust läge: vit bakgrund, mörk text, alla skärmar fullt läsbara ✅
- Switch: På = mörkt, Av = ljust, byte sker omedelbart ✅
- Inställningen sparas över omstart ✅

**Godkännande:**
Punkt 4.7 (Dark mode-toggle) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#47-dark-mode-toggle-förslag]]
- `data/settings/AppSettings.kt` — `ThemeMode` enum + `themeMode` StateFlow
- `ui/theme/Theme.kt` — `RFIDManagerTheme` tar `themeMode`, `LightColorScheme` med ljusa färger
- `ui/screens/SettingsScreen.kt` — Switch för mörkt/ljust
- `MainActivity.kt` — skickar `themeMode` till `RFIDManagerTheme`
- Samtliga skärmfiler migrerade till `MaterialTheme.colorScheme.*`

---

## [2026-06-11] UAT | Fas 4, Punkt 4.8 (Paginering i Readings) – Godkänd

**Datum:** 2026-06-11  
**Tid / Kontext:** UAT-test på fysisk Samsung Galaxy Note 10 via USB-deployment

**Utfört av:** Kund (Joa) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** Fas 4, acceptanskriterium 4.8 — Paginering i Readings

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS)
- App: RFID Manager (debug build via `adb install`)
- Förberedda testdata: flera persisterade avläsningar

**Omfattning:**
- `ReadingsViewModel` — `_displayLimit`, `_hasMore`, `loadMore()` styr hur många readings som visas
- `MainScreenHost` — "Ladda fler"-knapp längst ner i LazyColumn när `hasMore` är true
- `AppSettings` — `pageSize` StateFlow (10–50, steg om 10) sparas i SharedPreferences
- `SettingsScreen` — slider för sidstorlek som styr antal readings per "load more"
- Filter/sök-ändring återställer `_displayLimit` till aktuell `pageSize`
- Knapptexten visar dynamiskt aktuell sidstorlek, t.ex. "Ladda fler (20)"

**Testresultat:**
- Paginering fungerar: "Ladda fler" lägger till fler readings ✅
- Sidstorleks-reglaget i Settings fungerar (10, 20, 30, 40, 50) ✅
- Knappen visar dynamiskt antal ✅
- Filter/sök-ändring återställer till ny sidstorlek ✅
- Inställningen sparas över omstart ✅

**Godkännande:**
Punkt 4.8 (Paginering i Readings) godkänd av Kund för Fas 4.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-11

**Referenser:**
- [[Produkt-Roadmap#48-paginering-i-readings-förslag]]
- `data/settings/AppSettings.kt` — `pageSize` StateFlow + `setPageSize()`
- `ui/viewmodel/ReadingsViewModel.kt` — `_displayLimit`, `_hasMore`, `loadMore()`, `currentPageSize()`
- `ui/MainScreenHost.kt` — "Ladda fler"-knapp med dynamisk `pageSize`
- `ui/screens/SettingsScreen.kt` — slider för sidstorlek
- `assets/strings_sv.json` / `strings_en.json` — `screen.settings.page_size` + `screen.readings.load_more`

---

**Skapad:** 2026-06-07  
**Ägare:** Kund (du har sista ordet)  
**Uppdateras:** Vid varje UAT eller formellt godkännande

**Fas 3 Kick-off:** Genomförd 2026-06-07. Dokumentation ovan. Lead (Grok) driver processen framåt enligt låsta beslut.

Se även:
- [[Rollfördelning-och-Arbetsätt]] (roller och working agreement)
- [[log]] (append-only kronologi)
- [[bugs/README]] (struktur för formella felrapporter)

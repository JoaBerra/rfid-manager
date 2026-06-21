---
title: Fas 3 Implementation Plan
tags: [fas3, implementation, plan, backlog, coding]
created: 2026-06-07
updated: 2026-06-10
---

# Fas 3 Implementation Plan

**Status:** Kick-off godkänd av Kund 2026-06-07.  
**Lead:** AI-assistenten (Arkitekt / Technical Lead / Programmerare)  
**Kund:** Joa (Projektledare + UAT-testare)

## Övergripande mål (från godkända acceptanskriterier)
- Reallokera UI från trånga tabs till dedikerade vyer med bottom navigation.
- Implementera explicit breathing room (spacing-regler från spec).
- Införa ViewModel per vy + navigation state.
- Reaktivera riktig Room (KSP) med dual-mode fallback.
- Grundläggande polish (empty states, loading, error handling, accessibility).
- Lättviktigt PC-stöd: förbättrad loggning + dokumentation av MQTT Explorer.

**Huvudreferens:** [[Fas3-Navigation-Spacing-Design]] (den textbaserade specen som utvecklades först).

## Arbetssätt under implementering
- Använd standardiserad path-struktur: `~/projects/rfid/rfid-manager/`
- Fas-specifik tagg: `fas-3-ui-polish-2026-06`
- Löpande uppdatering av [[log.md]] och denna plan.
- Alla ändringar synkas till release-snapshot under `~/projects/rfid/rfid-manager/releases/2026-06-Fas2/`
- Breathing room check: Alla UI-ändringar granskas mot spacing-tabellen i design-noten.
- Kod följer befintlig nomenclature och rich comments-princip.

## Prioriterad implementationsbacklog

### Fas 3.1 – Grundläggande navigation (högsta prioritet)
1. Lägg till dependencies i `app/build.gradle.kts`:
   - `androidx.navigation:navigation-compose`
   - `androidx.lifecycle:lifecycle-viewmodel-compose`

2. Skapa `ui/navigation/Screen.kt`:
   - `sealed class Screen(val route: String)`
   - Objekt: Scan, Readings, Connectivity, Settings

3. Skapa `ui/MainScreenHost.kt`:
   - `Scaffold` med `BottomNavigation` (4 items, icons från Material).
   - `NavHost` med destinations som pekar till de fyra vyerna.
   - Hantera `navController`.

4. Refaktorera `MainActivity.kt`:
   - Ta bort tung state hoisting (flytta till ViewModels).
   - Behåll NFC-livscykel och AppContainer.
   - Sätt upp `MainScreenHost` som root i setContent.

5. Uppdatera befintliga skärmar:
   - `RFIDManagerScreen.kt` → dela upp / byt namn till `ScanScreen.kt` (fokusera på radar + live scanning).
   - Se till att "Readings" och "Connectivity" knappar navigerar korrekt.

**Acceptanskriterier som täcks:** Bottom nav med 4 items, dedikerade vyer, ingen trång TabRow, korrekt back-navigation.

### Fas 3.2 – ViewModels + state
6. Skapa ViewModels (i `ui/viewmodel/` eller per screen):
   - `ScanViewModel`
   - `ReadingsViewModel` (observerar `persistedReadingRepository`)
   - `ConnectivityViewModel`
   - Eventuell `AppViewModel` för delad state (t.ex. MQTT connection status).

7. Koppla ViewModels till skärmar med `viewModel()`.

8. Flytta state (t.ex. current readings filter, navigation state) från MainActivity till ViewModels.

**Acceptanskriterier som täcks:** ViewModel för navigation state, renare kodbas.

### Fas 3.3 – Spacing / Breathing room (parallellt med 3.1–3.2)
9. Skapa `ui/theme/Dimens.kt` (om inte finns):
   ```kotlin
   object Dimens {
       val cardPadding = 16.dp
       val listItemSpacing = 12.dp
       val sectionSpacing = 16.dp
       val radarMaxHeight = 280.dp   // anpassas
   }
   ```

10. Applicera spacing-regler i:
    - `PersistedListItem.kt`
    - `PersistedReadingsScreen.kt` (LazyColumn + chips)
    - `ScanScreen.kt` (StatCards grid, RadarView höjd)
    - `MqttStatusScreen.kt`

11. Validera mot tabellen i [[Fas3-Navigation-Spacing-Design]] (16 dp padding, 12 dp mellanrum, etc.).

**Acceptanskriterier som täcks:** Alla mätbara spacing-regler, inga överlapp, touch targets ≥48 dp.

### Fas 3.4 – Room + teknisk grund: ✅ KLAR (JSON-fallback)
12. I `AppContainer.kt`:
    - Försök initiera riktig `AppDatabase` och DAO med try/catch.
    - Vid misslyckande (saknad `_Impl`-klass) → null dao → JSON-fallback.

13. Testa att data överlever app-omstart — **bekräftat** via JSON-fil i `context.filesDir/readings.json`.

14. `DatabaseProvider.kt`, `PersistedReadingRepository.kt` — uppdaterade för dual-mode.

**Acceptanskriterier som täcks:** Data överlever app-omstart (JSON-fallback). Reaktivera riktig Room flyttas till roadmap — se [[Produkt-Roadmap#villkor-för-riktig-room-databas]].

### Fas 3.5 – Polish + PC-stöd (lägre prioritet)
15. Lägg till empty states, loading indicators, error handling i de nya vyerna. ✅
    - ScanScreen: ikon + färgkodad text + loading bar vid scanning
    - PersistedReadings (MainScreenHost): ikon + spinner + tomt tillstånd med instruktion
    - MqttStatusScreen: tomt tillstånd för meddelanden + dynamisk statusfärg + Wi-Fi/CloudOff-ikon
    - SettingsScreen: ny egen fil (storage mode, app info, version)
16. Förbättra Python subscriber i `~/projects/rfid/rfid-manager/test/fas2-mqtt/mqtt/` med bättre strukturerad loggning. ✅
    - Färgkodad output (ANSI) per meddelandetyp
    - Filtrering på uid (--uid-flagga)
    - Reconnect-hantering via on_disconnect
    - Ctrl+C → statistik (received/persisted/errors)
17. Uppdatera dokumentation: Rekommendera MQTT Explorer som gratis verktyg för transaktionsinspektion. ✅
    - Ny wiki-sida: [[MQTT-Explorer]]
    - Referens i subscriber-scriptets docstring

**Acceptanskriterier som täcks:** Grundläggande polish, lättviktigt PC-stöd.

## Synk & Release
- Alla ändringar görs först i dev-miljön (`~/projects/rfid/rfid-manager-android`).
- Synkas till `~/projects/rfid/rfid-manager/releases/2026-06-Fas2/RFIDManager/`.
- Ny tagg när delmål är klart: `fas-3-ui-polish-...`
- Uppdatera `PUSH-TO-GITHUB.md` och `README.md` i release-mappen vid behov.

## Risker & Mitigering (uppdaterad efter kick-off)
- KSP-problem: Dual-mode behålls som fallback.
- Scope creep: Lead flaggar omedelbart.
- UAT på enhet: Använd samma testmiljö + MQTT Explorer.

## Rekommenderad låst sekvens (Lead-förslag efter kick-off)

**Varför låsa ordningen först?** (Nyttor)
- **Minskar risk för omarbete**: Navigation är fundamentet. Om vi bygger spacing/ViewModels innan nav-strukturen är stabil, riskerar vi att behöva flytta kod senare.
- **Möjliggör inkrementell leverans & tidig feedback**: Varje fas ger ett testbart inkrement (t.ex. efter 3.1 har vi fungerande bottom nav med placeholders – kan visas för dig som Kund direkt).
- **Bättre andrum & disciplin**: Spacing-regler kan appliceras medan vi bygger vyerna (parallellt), inte som en stor "städning" i slutet.
- **Tydligare scope-kontroll**: Gör det lättare att säga nej till scope creep (t.ex. "detta hör till 3.5").
- **Enklare spårbarhet & rollback**: Fas-specifika commits/taggar blir logiska.
- **Effektivare samarbete**: Du (Kund) får tydliga milstolpar att ge feedback på istället för en stor "big bang".
- **Teknisk beroendehantering**: Navigation-compose måste vara på plats innan vi kan använda NavHost/Screen i kod.

**Låst rekommenderad sekvens** (Lead-rekommendation – godkänd att följa om du inte har annan preferens):

**Fas 3.1 – Navigation Foundation (sekventiellt, högsta prioritet)**
- 3.1a: Dependency (navigation-compose) – KLAR i snapshot, applicera i dev.
- 3.1b: Skapa `ui/navigation/Screen.kt` + grundläggande `MainScreenHost.kt` med BottomNavigation + NavHost (4 destinations, placeholders för vyerna).
- 3.1c: Refaktorera `MainActivity.kt` + `RFIDManagerScreen.kt` så att den gamla TabRow tas bort och vi använder den nya hosten. Huvudskärm blir "Scan".

**Fas 3.2 – ViewModels + State (kan starta efter 3.1b)**
- Skapa ViewModels för de nya vyerna.
- Flytta state från MainActivity.
- Koppla till Repository.

**Fas 3.3 – Spacing / Breathing Room (kan köras parallellt med 3.1c–3.2)**
- Applicera de konkreta spacing-reglerna från design-noten i de nya vyerna och komponenter (16dp, 12dp, etc.).
- Validera mot "breathing room check".

**Fas 3.4 – Room Enablement (vertikal slice, kan göras när 3.1 är stabil)**
- Slå på riktig DAO i AppContainer.
- Testa persistens över app-omstart.

**Fas 3.5 – Polish + PC-stöd (sist)**
- Empty states, loading, error handling, accessibility.
- Förbättra Python subscriber + dokumentera MQTT Explorer.

Denna sekvens är nu låst i planen. Vi följer den om du inte säger annat.

**Aktuell status (uppdaterad 2026-06-10):**
- **Fas 3.1 – Navigation Foundation: ✅ KLAR**
  - 3.1a: Dependencies (navigation-compose, viewmodel-compose) i dev-miljön.
  - 3.1b: `Screen.kt` (sealed), `MainScreenHost.kt` (Scaffold + 4-item NavigationBar + NavHost) implementerade.
  - 3.1c: `MainActivity.kt` refaktorerad — anropar `MainScreenHost` som root. Gamla TabRow borttagen. `ScanScreen.kt` (radar + stat cards + detected tags), `PersistedReadingsScreen.kt` och `MqttStatusScreen.kt` fullt anpassade för NavHost. Settings placeholder finns.
- **Fas 3.2 – ViewModels: ✅ KLAR**
  - `ReadingsViewModel` finns och används av PersistedReadingsScreen.
  - `ScanViewModel` skapad — hanterar `selectedTagUid`. Skapas i `MainScreenHost`.
  - `ConnectivityViewModel` skapad — hanterar MQTT-status, heartbeat, meddelandelogg, test publish.
  - `MqttStatusScreen` omarbetad — använder ViewModel istället för lokal state. `onClose` borttagen (NavHost-navigation via bottom bar).
  - Placeholders (`DedicatedReadingsPlaceholder`, `DedicatedConnectivityPlaceholder`) borttagna.
- **Fas 3.3 – Spacing/Breathing Room: ✅ KLAR**
  - `Dimens.kt` skapad med `cardPadding`, `sectionSpacing`, `smallGap`, `listItemSpacing`, `screenHorizontalPadding`.
  - Alla skärmar och komponenter använder Dimens-konstanter för konsekvent spacing.
  - Minst 16 dp padding i cards, 12 dp mellan listrader, touch targets ≥48 dp.
- **Fas 3.4 – Room Enablement: ✅ KLAR (JSON-fallback)**
  - KSP (2.2.10-2.0.2) inkompatibelt med AGP 9.x built-in Kotlin. Även KSP 2.2.21-2.0.5 saknar stöd.
  - kapt borttaget i Kotlin 2.2.x.
  - Opt-out (builtInKotlin=false) orsakar "already on classpath"-konflikter.
  - **Slutsats:** Riktig Room-databas blockerad tills KSP får AGP 9-stöd. JSON-fallback aktiv.
  - Se [[Produkt-Roadmap#villkor-för-riktig-room-databas]] för återaktiveringsplan.
- **Fas 3.5 – Polish + PC-stöd: ✅ KLAR**
  - Empty states + loading + error i ScanScreen, PersistedReadings, MqttStatusScreen, SettingsScreen.
  - Python subscriber: färgkodad, filtrerbar, statistik vid Ctrl+C.
  - MQTT Explorer dokumenterad i wiki.
- **UAT-punkter (post-implementation, 2026-06-10): ✅ INFÖRDA**
  - Radar sweep animerad.
  - "GO TO READINGS" borttagen.
  - JSON-indikator borttagen från Readings.
  - Readings sorterade på timestamp descending.
  - Connectivity visar alla readings med statusetikett (Pending/Transmitted), tidsstämpel, typ, dataPreview.
  - Se [[Kundrelationer-och-Acceptans#fas-3-uat-test-2026-06-10]] för full dokumentation.
- **Buggar lösta under Fas 3 (utanför planen):**
  - Andra taggens data visade första taggens data i scanning — **fixad** (remove `remember` from `currentTags` SnapshotStateList key).
  - Persist-knappens state återställdes vid navigation — **fixad** (session-scoped `mutableStateOf` i MainScreenHost).

Uppdaterad: 2026-06-10

## Fas 3 — Sign-off ✅

**Godkänd av Kund** (Joa) 2026-06-10. Se [[Kundrelationer-och-Acceptans#fas-3-sign-off-2026-06-10]].

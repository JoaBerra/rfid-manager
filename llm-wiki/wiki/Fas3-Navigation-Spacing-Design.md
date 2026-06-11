---
title: Fas 3 – Navigation & Spacing Design
tags: [fas3, design, navigation, ui, spacing, compose]
created: 2026-06-07
---

# Fas 3 – Navigation & Spacing Design

**Syfte**: Konkret design för att uppfylla acceptanskriterierna för navigation + breathing room i Fas 3.  
**Baserat på**:  
- [[Produkt-Roadmap#fas-3-plan-ui-förbättringar--grundläggande-polish]]  
- [[Kundrelationer-och-Acceptans#utkast-fas-3-acceptanskriterier-2026-06-07]] (de konkreta mätbara kriterierna)  
- De tre referensbilderna från Fas 2 (fas2-main-rfid-screen.jpg, fas2-persisted-readings-list.jpg, fas2-mqtt-sparkplug-status.jpg)  
- Befintlig nomenclature

## Låsta arkitekturbeslut (2026-06-07)

1. **Navigation**: Vi inför `androidx.navigation.compose` för bottom navigation + dedikerade destinations.
2. **State Management**: ViewModel per vy + lätt AppViewModel för delad state (t.ex. MQTT-status, global persisted list).

Dessa beslut är dokumenterade i [[App-Architecture#arkitekturbeslut-för-fas-3-låsta-2026-06-07]].

## Övergripande Navigation Structure

**Bottom Navigation** (4 fasta items, Material 3):

- **Scan** → Huvudskärm (live scanning + Radar + snabba StatCards + "Start Scan")
- **Readings** → Dedikerad `PersistedReadingsScreen` (full vy med filter, sök, lista)
- **Connectivity** → Dedikerad `MqttStatusScreen` (status, historik, test publish, connection details)
- **Settings** → Framtida (minimal i Fas 3)

**Destinations** (Compose Navigation):

```kotlin
sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object Readings : Screen("readings")
    object Connectivity : Screen("connectivity")
    object Settings : Screen("settings")
}
```

- Använd `NavHost` + `BottomNavigation` i en ny eller refaktorerad `MainScreenHost`.
- `RFIDManagerScreen` blir troligen omdöpt/refaktorerad till `ScanScreen` (huvudflödet).
- Navigation via `navController.navigate(Screen.Readings.route)` etc.
- State bevaras per destination (Compose Navigation hanterar detta bra).

**Varför detta löser Fas 2-problemet**:
- Inga trånga TabRows som blandar READ/WRITE/PERSISTED.
- Varje vy får sin egen "andning" och kan växa utan att tränga ihop sig.
- Uppfyller explicit: "Dedikerade vyer istället för allt-i-ett-tab" och "Huvudskärm fokuserar på live scanning + Radar + översikt".

## Spacing / Breathing Room Implementation

Vi implementerar de mätbara reglerna från acceptanskriterierna direkt i Compose (använder befintliga Figma-mocks som visuell referens, ingen ny bildgenerering behövs för iterationer).

**Konkreta regler som kodas in**:

| Regel | Compose-implementation | Exempel |
|-------|------------------------|---------|
| Minst 16 dp padding inuti Cards | `Modifier.padding(16.dp)` på Card content | `PersistedListItem`, StatCards |
| Minst 12 dp mellan listrader | `Arrangement.spacedBy(12.dp)` i LazyColumn | `PersistedReadingsList` |
| RadarView max 35–40 % av höjden | Begränsad höjd + aspect ratio | `RadarView(Modifier.heightIn(max = 280.dp))` (anpassas till skärm) |
| 8–12 dp mellan StatCards | `Arrangement.spacedBy(8.dp)` i Grid | 2x2 grid på huvudskärm |
| Generösa empty states | `Modifier.padding(32.dp)` + stor ikon + text | "No persisted readings yet" |
| Touch targets ≥ 48 dp | Button/Clickable med minSize | Alla Transmit/Details-knappar |
| Cards med tydlig separation | `Card(elevation = CardDefaults.cardElevation(4.dp))` eller border | PersistedListItem |

**Exempel på spacing i en lista** (Kotlin/Compose):

```kotlin
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(16.dp)
) {
    items(readings) { reading ->
        PersistedListItem(
            reading = reading,
            modifier = Modifier.padding(horizontal = 0.dp) // inre padding hanteras i komponenten
        )
    }
}
```

**I PersistedListItem** (enligt nomenclature):

- Yttre Card: `Modifier.padding(horizontal = 16.dp)` (om listan inte har det)
- Inre innehåll: `Modifier.padding(16.dp)`
- Mellan metadata-rader: `Spacer(Modifier.height(8.dp))`

Samma principer appliceras på:
- Huvudskärmens StatCards (2x2 grid)
- MqttStatusScreen (kort + logg-lista)
- Modaler/sheets

## Skiss på filändringar & kodstruktur (Fas 3)

### Nya / ändrade filer (ungefärlig ordning)

1. **Ny dependency** (i `app/build.gradle.kts`):
   ```kotlin
   implementation("androidx.navigation:navigation-compose:2.8.0") // eller senaste stabila
   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
   ```

2. **AppContainer.kt** (utökas):
   - Lägg till ViewModel-providers om vi vill hålla det enkelt utan Hilt.
   - Fortfarande stöd för både in-memory och riktig DAO.

3. **MainActivity.kt** (förenklas kraftigt):
   - Ta bort de flesta hoisted states.
   - Skapa `AppContainer` som tidigare.
   - Sätt upp `NavHost` + `BottomNavigation` (eller flytta till en `MainScreenHost`).
   - Hantera bara NFC-livscykel + globala saker (t.ex. pendingWrite för armed writes).

4. **Ny fil: ui/navigation/Screen.kt** (eller i ui package):
   - `sealed class Screen` med routes.
   - Eventuellt `NavGraph` setup.

5. **Ny fil: ui/MainScreenHost.kt** (rekommenderas):
   - `Scaffold` med `BottomNavigation`.
   - `NavHost` som hostar de fyra vyerna.
   - Hanterar `navController`.

6. **Ändringar i befintliga skärmar**:
   - `RFIDManagerScreen.kt` → delas upp eller byter namn till `ScanScreen.kt`.
   - `PersistedReadingsScreen.kt` → görs till full dedikerad destination (flytta filter, sök etc. hit).
   - `MqttStatusScreen.kt` → görs till dedikerad destination.
   - Alla tre får egna ViewModels: `ScanViewModel`, `ReadingsViewModel`, `ConnectivityViewModel`.

7. **Nya ViewModels** (i `ui/viewmodel/` eller per screen):
   - `ReadingsViewModel` observerar `persistedReadingRepository.getAllReadings()`.
   - Hanterar filter-state, onTransmit etc.
   - `ConnectivityViewModel` för MQTT-status (kan senare kopplas till en shared MQTT-observer).

8. **Spacing & Theme-uppdateringar**:
   - Lägg till spacing constants i `ui/theme/Dimens.kt` (om inte finns):
     ```kotlin
     object Dimens {
         val cardPadding = 16.dp
         val listItemSpacing = 12.dp
         val sectionSpacing = 16.dp
     }
     ```
   - Använd konsekvent i alla nya/ändrade komponenter.

9. **AppDatabase / Repository**:
   - I Fas 3: Försök slå på riktig DAO i `AppContainer` (se dual-mode i [[App-Architecture]]).
   - Om KSP-problem kvarstår → behåll in-memory som fallback (transparent för UI).

### Rekommenderad implementationsordning (efter denna design)

1. Lägg till navigation-compose dependency + grundläggande NavHost + Bottom Nav (utan att bryta befintlig TabRow först).
2. Skapa `MainScreenHost` + flytta en vy i taget (börja med Readings).
3. Inför `ReadingsViewModel` + koppla till befintlig Repository.
4. Applicera spacing-regler på Readings-skärmen (lättast att validera).
5. Upprepa för Connectivity.
6. Refaktorera huvudskärmen till Scan + ta bort gammal TabRow.
7. Slå på riktig Room + testa dataöverlevnad.
8. UAT mot acceptanskriterierna.

## Koppling till acceptanskriterier

Alla punkter ovan är direkt härledda från de konkreta kriterierna i [[Kundrelationer-och-Acceptans]]:

- Bottom nav med 4 items → dedikerade vyer.
- Mätbara spacing-värden (16 dp, 12 dp, max % för Radar etc.).
- Touch targets, empty states, separation.

## Nästa steg (Lead)

- Skapa ViewModel-skelett + navigation setup i kod (kan göras i nästa iteration).
- Uppdatera Figma-Design-Spec-Fas2.md eller skapa Fas 3-spec om det behövs (textbaserat).
- Börja med kodändringar i en branch (t.ex. `feature/fas3-navigation`).

**Status**: Design note klar. Arkitekturbeslut låsta. Kodskiss levererad.

Uppdaterad: 2026-06-07

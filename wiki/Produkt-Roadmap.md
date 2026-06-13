---
title: Produkt Roadmap och Features
tags: [roadmap, features, backlog, fas, planering, projektstyrning]
created: 2026-06-07
---

# Produkt Roadmap och Features – RFID Manager

> **Syfte**: Single source of truth för vad som är gjort, pågående och planerat.  
> Uppdateras vid varje fas-slut (efter UAT/sign-off i [[Kundrelationer-och-Acceptans]]).  
> Baseras på Figma-specs, App-Architecture, logg och UAT-feedback.

## Fas 2 – Slutfört (UAT godkänd 2026-06-07)

**Mål som uppnåtts:**
- Lokal persistens av läsningar (in-memory fallback + Room-entiteter/DAO/Repository redo).
- MQTT-kommunikation (Sparkplug-liknande JSON på `rfidmanager/<uid>/telemetry`).
- UI för persisterade läsningar + Transmit ↑ + grundläggande MQTT-status.
- End-to-end validering på fysisk Samsung Galaxy Note 10 (read/write → persist → transmit → subscriber + SQLite).
- EPERM-problem på debug builds löst (manifest + network_security_config + Samsung-inställningar).
- Formell UAT av Kund + sign-off (se [[Kundrelationer-och-Acceptans#2026-06-07-uat-fas-2-godknd-av-kund]]).

**Referenser:**
- [[Fas2-Implementation-Overview]] (detaljerad status + öppna punkter vid godkännande).
- [[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] (Resolved + PDF).
- Tag i GitHub: `fas-2-uat-godkand-2026-06-07` (i release-snapshot).

**Öppna punkter från Fas 2-godkännande (prioriteras i Fas 3):**
- ViewModel-refaktor (bättre state-hantering).
- Reaktivera riktig Room (KSP istället för in-memory).
- UI-polish och andrum (se nedan).
- Kryptering: Produktion skall vara krypterad (dev okrypterat godkänt under utveckling).

## Fas 3 – Plan (UI-förbättringar + grundläggande polish)

**Fokus:** Gör appen användbar och luftig. Allokera om funktionalitet så att inget trängs ihop.

**De tre gränssnitten från Figma (referensbilder i `~/rfid-manager/releases/2026-06-Fas2/Fas2-Figma-UI-Mocks/` och artifacts):**
1. **Huvudskärm / Main RFID** (fas2-main-rfid-screen.jpg) – RadarView + StatCards + TabRow (READ | WRITE | PERSISTED) + systemstatus.
2. **Persisted Readings List** (fas2-persisted-readings-list.jpg) – Lista med filter (All/RFID/EAN), PersistedListItem (rik metadata: UID, timestamp, Source/Location, dataPreview, status badges, Transmit/Details).
3. **MQTT / Sparkplug Status** (fas2-mqtt-sparkplug-status.jpg) – Status, recent messages, test publish, heartbeat, version.

**Fas 2-erfarenhet:** Allt (särskilt Persisted + MQTT) trängdes in i tabs på huvudskärmen → kändes tight.

**Fas 3-förslag (reallokering med fler UI-yta):**
- **Dedicated screens istället för allt-i-ett-tab:**
  - Huvudskärm: Fokusera på live scanning + Radar + snabba StatCards + "Start Scan" + navigation till andra vyer (bottom nav eller drawer).
  - Separat **Readings / Persisted** screen (full vy med filter, sök, sortering, bulk actions, rich PersistedListItem).
  - Separat **Connectivity / MQTT** screen eller sheet (status, logg, last transmitted JSON, manual test publish, connection details).
- Använd Material 3 patterns: 
  - Bottom navigation (Scan | Readings | Connectivity | Settings).
  - Eller top-level tabs + "More" för avancerat.
  - Modal/sheet för detaljer (t.ex. full Sparkplug JSON eller Transmit confirmation).
- Andrum: Större cards, bättre spacing, collapsible sections, filter chips istället för trånga listor.
- Behåll nomenclature 1:1 (PersistedListItem, MqttStatusBadge, etc.) från [[Nomenclature-Figma-Android]] och Figma-specs.
- Lägg till ViewModel + riktig Room.
- Grundläggande polish: Empty states, loading, error handling, accessibility.

**Övrigt i Fas 3 (låg risk):**
- Förbättra Python subscriber i testmiljön med bättre loggning (se PC-sektion nedan).
- Fortsatt användning av explicit roller + UAT/sign-off per delmål.

**Mål för Fas 3 UAT:**
- Appen känns "luftig" och professionell på telefonen.
- Användaren kan enkelt navigera mellan live scanning, historik och anslutningsstatus utan att känna trängsel.
- Kodbas renare (ViewModel + Room).

**Konkreta acceptanskriterier för navigation + spacing** (detaljerat utkast + design finns i [[Kundrelationer-och-Acceptans#utkast-fas-3-acceptanskriterier-2026-06-07]] och [[Fas3-Navigation-Spacing-Design]]):
- Bottom navigation med 4 items (Scan | Readings | Connectivity | Settings).
- Dedikerade fullskärmsvyer istället för trånga tabs.
- Mätbara spacing-regler: minst 16 dp padding i cards, 12 dp mellan listrader, RadarView max 35–40 % av höjden, etc.
- Inga överlapp, tillräckliga touch targets (≥48 dp), generösa empty states.

Se full design note: [[Fas3-Navigation-Spacing-Design]] (innehåller låsta beslut, exakt NavHost-struktur, spacing-implementation i Compose och filändrings-skiss).

## Senare faser (Fas 4+)

### Villkor för riktig Room-databas

**Status:** Room-källkod (entiteter, DAO, databas, repository) finns i trädet och kompilerar. JSON-fallback aktiv i produktion — data överlever app-omstart. Riktig Room kräver en annotationsprocessor som idag inte är kompatibel med AGP 9.2.1 + Kotlin 2.2.10.

| Barriär | Förklaring | Lösning när |
|---|---|---|
| KSP 2.2.10-2.0.2 kräver `KotlinCompilerPluginSupportPlugin` | Klassen finns inte i AGP 9 built-in Kotlin — KSP kraschar vid configuration | KSP släpper version som stödjer AGP 9 (kolla PR [#2674](https://github.com/google/ksp/pull/2674), merged Oct 2025) |
| kapt borttaget i Kotlin 2.2.x | Kapt-pluginet är inkompatibelt med built-in Kotlin, och Kotlin 2.2 har tagit bort kapt-konfigurationen | Använd KSP istället (nedan) |
| Opt-out (builtInKotlin=false) krockar med AGP | AGP läser ändå in KGP på classpath → "already on classpath" vid plugin-applicering | Kräver AGP-fix eller annan plugin-hantering |

**Så här återaktiverar du Room när KSP är kompatibelt:**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "<kompatibel-version>" // t.ex. 2.2.21-2.0.5 när fix är bekräftad
}

dependencies {
    ksp(libs.androidx.room.compiler)      // ← lägg till denna rad
}
```

Ta sedan bort try/catch i `AppContainer.kt` och låt `DatabaseProvider.getDatabase()` initiera direkt.

**Övervakning:** Kontrollera KSP-releases på https://github.com/google/ksp/releases efter en version som nämner "AGP 9", "built-in Kotlin" eller "KotlinCompilerPluginSupportPlugin".

---

- **EAN / Barcode scanning** (kamera via CameraX + ML Kit eller ZXing).  
  Domän: `BarcodeReading`. UI: Ny vy/knapp + integrering i Readings-listan.  
  **Placeras i Fas 4 eller senare** (per användarens önskemål – vi håller fokus på RFID + UI i Fas 3).

- **PC-sida: Tydligare transaktionslogg + grafiskt gränssnitt för MQTT** (se bedömning nedan).  
  Python-subscribern har färgkodad utdata och UID-filtrering. MQTT Explorer dokumenterat som gratis GUI-alternativ — se [[MQTT-Explorer]].

- **Produktionshärdning:**
  - Riktig kryptering (MQTT over TLS / wss).
  - Riktig Room-persistens + migreringar (se villkor ovan).
  - Bättre felhantering, retry, offline-kö.
  - Release builds, Play Store (valfritt).

- **Avancerat:**
  - Full Sparkplug B (NBIRTH, NDATA, metrics etc.).
  - Multi-device / multi-tenant stöd.
  - Dashboard på PC med historik, grafer, export.

---

## Fas 4 — Lokalisering, inställningar och användbarhet

**Mål:** Gör appen redo för kundanpassning (språk, textstorlek, sök, export, dark mode).

### Acceptanskriterier

#### 4.1 Lokaliseringssystem (i18n)
- Alla användarsynliga texter (etiketter, rubriker, knappar, statusmeddelanden, empty states) är flyttade till en JSON-fil per språk.
- En tabell/lexikon skapas i wikin (t.ex. i [[Fas4-Implementation-Plan]]) med kolumner: **Identifierare** | **Svenska** | **Default (engelska)**.
- `AppContainer` laddar rätt JSON baserat på valt språk.
- Settings-vyn har en språkväljare där användaren kan byta språk.
- Byte av språk sker omedelbart (ingen omstart krävs) — UI rekompenserar via `StateFlow`.
- Minst två språk levereras: svenska (primär) och engelska (fallback).

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-41-lokaliseringssystem-i18n--godkänd]].

#### 4.2 Textstorlek (font size-slider)
- Settings-vyn har en slider för "Font size — transaktionsdata".
- Intervallet är 1.0× – 1.8× av default-storleken (clampas i AppSettings till samma intervall).
- Inställningen sparas i SharedPreferences och återställs vid nästa start.
- Slidern påverkar alla listor med readings (Connectivity, Readings, Scan).
- Defaultvärde sätts till 1.0× (normal).

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-42-textstorlek-font-size-slider-godknd]].

#### 4.3 Sök/filtrering i Readings
- Ett sökfält ovanför Readings-listan.
- Filtrerar i realtid på UID eller kod (case-insensitive) med stöd för wildcards (`*`, `?`).
- Sökfältet syns även när listan är tom (förberedande).
- Fungerar i kombination med type-filter (All/RFID/EAN).

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-43-sökfiltrering-i-readings--godkänd]].

#### 4.4 Export (CSV/JSON)
- I Settings: knappar "Export CSV" och "Export JSON".
- Anropar Android Share Sheet → användaren kan skicka filen via mejl, spara nedladdningar, etc.
- Exporterade data innehåller alla metadata-fält.
- Knapparna disabled om inga readings finns i listan.

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-44-export-csvjson--godkänd]].

#### 4.5 Haptic + ljud vid scan
- Kort vibration när en tagg detekteras.
- Kort ljudsignal (pip) när en tagg detekteras.
- Inställning i Settings: på/av för haptik och ljud separat.

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-45-haptic--ljud-vid-scan--godkänd]].

#### 4.6 Riktig MQTT-anslutning
- Ersätt demo-data i ConnectivityViewModel med riktig MQTT-uppkoppling.
- Anslutningsstatus, heartbeat och meddelanden kommer från en verklig broker.
- Automatisk återanslutning om broker är otillgänglig.

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-46-riktig-mqtt-anslutning--godkänd]].

#### 4.7 Dark mode-toggle
- Settings: Switch mellan mörkt och ljust läge.
- Tema appliceras omedelbart utan omstart.
- Alla skärmar migrerade från hårdkodade färger till `MaterialTheme.colorScheme.*`.

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-47-dark-mode-toggle--godkänd]].

#### 4.8 Paginering i Readings (förslag)
- Ladda och visa readings med "Load more"-knapp längst ner.
- Antal per "load more" konfigurerbart via slider i Settings (10–50).

> ✅ **Status:** Godkänd och signerad av Kund 2026-06-11. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-48-paginering-i-readings--godkänd]].

## Fas 5 — Dokumentation, kvalitet och radar-estetik

**Mål:** Gör projektet redo för överlämning och arkivering.

### Acceptanskriterier

#### 5.1 Användarmanual
- PDF-manual med skärmbilder som beskriver varje vy och flöde.
- Språk: Svenska.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13. Se [[Kundrelationer-och-Acceptans#2026-06-13-uat--fas-5-punkt-51-användarmanual--godkänd]].

#### 5.2 Arkitektur-diagram
- Uppdatera [[App-Architecture]] med Fas 3 och Fas 4-strukturen samt Fas 5-tillägg.
- Diagram över komponenter, dataflöden och navigation.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13. Se [[Kundrelationer-och-Acceptans#2026-06-13-uat--fas-5-punkt-52-arkitektur-diagram--godkänd]].

#### 5.3 Release notes
- Dokument per fas med vad som ingår, buggar fixade, kända begränsningar.
- GitHub Releases uppdateras.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13. Se [[Release-Notes]].

#### 5.4 Testplan
- Lista över vad som ska testas inför varje release.
- Manuella testfall för varje vy och funktion.
> ✅ **Status:** 22 testfall, alla Godkänt av Kund 2026-06-13. Se [[Testplan]].

#### 5.5 Kodgenomgång
- Ta bort sista rester av Fas 1–2 mönster (om några).
- Säkerställ konsekvent kodstil.
> ✅ **Status:** Godkänd 2026-06-13.

#### 5.6 Dynamisk layout för fontstorlek
- Postytan (reading-kort) skall dynamiskt ändra storlek när fontstorleken förändras.
- Datafältens placering och layout skall ses över så att ytan används mer effektivt, särskilt vid förstorad text.
> ✅ **Status:** Godkänd 2026-06-13.

#### 5.7 Radar sweep med trail/efterglöd (flyttad från Fas 6)
- Radarns svepande linje lämnar ett avtagande spår (trail/efterglöd) efter sig, istället för att bara vara en tunn linje.
- Trailen bleknar gradvis (opacity eller fade-out) för att efterlikna en riktig radarskärm.
- Animationen skall vara mjuk och prestandavänlig (ingen onödig recomposition).
- Trail-effekten syns endast när scanning är aktiv.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13. Se [[Kundrelationer-och-Acceptans#2026-06-13-uat--fas-5-punkt-57-radar-sweep-trail--godkänd]].

### Estetisk Feature (framtida förbättring)
- En detekterad tagg föds på sveplinjen (dess vinkel sätts av svepets position vid detektionstillfället).
- Taggen är endast synlig så länge den befinner sig inom efterglödsfältet (72° bakom sveplinjen).

## Fas 6 — 1.0 Release

**Mål:** Förbered och leverera version 1.0 av RFIDManager.

### Acceptanskriterier

#### 6.1 MQTT-broker-konfiguration i UI
- Settings-vy fält för broker-URL (tcp://...) och port.
- Sparas i AppSettings/SharedPreferences.
- MqttConnectionManager läser från settings istället för hårdkodad URL.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13.

#### 6.2 App-ikon
- Anpassad launcher-ikon (ej standard Android-ikon).
- Minst adaptive icon för API 26+.
> ✅ **Status:** Godkänd och signerad av Kund 2026-06-13.

#### 6.3 Release build-setup
- Signeringskonfiguration i build.gradle.kts.
- ProGuard-regler för minifiering.
- APK/AAB för distribution.

#### 6.4 Borttagning av stub-kod
- `StubNfcManager.kt` tas bort eller flyttas till test.
- Sista demo-referenser i kommentarer städas.

#### 6.5 End-to-end test
- Fullständig manuell testkörning enligt testplan (5.4).
- Verifiera: NFC-läs, persistens, MQTT, export, dark mode, fontskalning, paginering.

#### 6.6 Version 1.0
- Bump till versionName = "1.0", versionCode = 2.
- Release notes sammanställda.
- GitHub Release skapad med tag `v1.0`.

## Hur vi håller roadmapen levande

- Uppdateras efter varje UAT/sign-off i [[Kundrelationer-och-Acceptans]].
- Fas-planering börjar alltid med: "Vad är acceptanskriterier för den här fasen?"
- Länkar till:
  - [[Fas2-Implementation-Overview]] (historik).
  - Figma-specs ([[Figma-Design-Spec-Fas2]], [[Figma-Prototype-Fas2-Proof]]).
  - [[App-Architecture]] (tekniska beslut).
  - [[Kundrelationer-och-Acceptans]] (formella godkännanden).

---

**Senast uppdaterad:** 2026-06-13 (Fas 5 samtliga punkter godkända; Estetisk Feature tillagd).

**Fas 3 UAT-test:** Genomförd 2026-06-10. 10 ändringskrav införda och installerade. **Sign-off av Kund 2026-06-10.**
Se [[Kundrelationer-och-Acceptans#fas-3-uat-test-2026-06-10]] och [[Kundrelationer-och-Acceptans#fas-3-sign-off-2026-06-10]] för detaljer.

**Fas 3 Kick-off Check:** Genomförd och godkänd av Kund 2026-06-07 (se [[Kundrelationer-och-Acceptans#fas-3-kick-off-check-driven-by-lead-2026-06-07]]). 
- Kick-off godkänd av Kund.
- Fas 3-acceptanskriterierna officiellt godkända för UAT av Kund.
- Lead har mandat att driva implementering.
- Nästa steg: Börja kodning baserat på [[Fas3-Navigation-Spacing-Design]].

Se även uppdaterad sökvägsstruktur i [[rfid-manager/README]] (~/rfid-manager/ samlar test, setup, releases och artifacts).

Se även [[index]] för översikt och [[Rollfördelning-och-Arbetsätt]] för hur vi samarbetar runt roadmapen.
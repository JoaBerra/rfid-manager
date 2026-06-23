---
title: Rollfördelning och Arbetsätt
tags: [process, roller, arbetsätt, projektledning, governance, karpathy-wiki, funktionell-nomenklatur]
created: 2026-06-06
updated: 2026-06-23
---

# Rollfördelning och Arbetsätt – RFID Manager

> **Syfte**: Göra samarbetet explicit, förutsägbart och skalbart istället för enbart fri dialog.  
> Vi är ett **1-människa + 1 AI-assistent**-team. Genom att tilldela tydliga roller kan vi undvika rollkonflikter, förbättra spårbarhet och göra det lättare att återanvända arbetssättet i framtida projekt.

## Grundprincip

Arbetet sker i **två parallella spår**:
1. **Affärs- och styrningsspår** (du som Kund + Projektledare)
2. **Tekniskt genomförandespår** (AI-assistenten i flera tekniska roller)

Wiki:n (särskilt `log.md` + strukturerade sidor) är **Single Source of Truth** och levande projektdokumentation.

## Funktionell nomenklatur — vad vi kallar saker

För att göra dokumentationen oberoende av specifika AI-produkter eller leverantörer använder vi funktionella termer istället för produktnamn. Denna nomenklatur införs löpande — när vi redigerar ett dokument och stöter på ett produktnamn byter vi ut det mot motsvarande funktionella term.

| I dokumenten använder vi | Istället för | Innebörd |
|--------------------------|-------------|----------|
| **AI-assistenten** | Specifika produktnamn från olika leverantörer | Den tekniska assistentrollen i teamet |
| **AI-verktyg / AI-system** | Specifika AI-plattformar | Det underliggande verktyget |

Arbetssätt: När du som Projektledare eller jag som AI-assistent stöter på ett produktnamn under redigering, ersätter vi det med rätt funktionell term. Inget aktivt sök-och-ersätt över hela wikin.

## Roller (uppdaterad och kompletterad)

### Roller du (Joa) primärt äger

| Roll                    | Beskrivning                                                                 | Typisk aktivitet |
|-------------------------|-----------------------------------------------------------------------------|------------------|
| **Kund / Product Owner**| Representerar den verkliga kunden och slutanvändaren. Definierar krav, acceptanskriterier och gör UAT. | Kravspec, prioritering, "det här är inte användbart", godkännande av leverans. |
| **Projektledare**       | Ansvarig för scope, tid, resurser (i detta fall dig själv + AI), risker och beslut. | Sätter faser (Fas 1, Fas 2...), godkänner arkitekturval, stoppar/startar spår. |
| **Domänexpert**         | Industriell kunskap (eskortminnen, RFID i verklig drift, säkerhet, processer). | Ger kontext som ingen ren tekniker har. |
| **UAT-testare & Slutanvändare** | Utför manuell acceptanstest på riktig hårdvara i verklig miljö. | "Jag har testat på telefonen...", ger slutgiltig feedback. |

### Roller AI-assistenten primärt äger

| Roll                        | Beskrivning                                                                 | Typisk aktivitet |
|-----------------------------|-----------------------------------------------------------------------------|------------------|
| **Projektassistent**        | Operativt stöd till Projektledaren. Håller ordning, följer upp, förbereder underlag. | Skapar todo-listor, sammanfattar, påminner om öppna punkter, packar releaser. |
| **Arkitekt**                | Ansvarig för den övergripande tekniska arkitekturen (lager, nomenclature, design-to-code mapping, långsiktig hållbarhet). | Föreslår arkitektur (t.ex. Room + Sparkplug), förankrar 3-lagers-modell, uppdaterar App-Architecture.md. |
| **Technical Lead**          | Teknisk kvalitet, kodstandard, val av bibliotek, hur saker ska byggas rätt första gången. | Granskar egen kod, driver rich comments, nomenclature, best practices. |
| **Programmerare / Implementer** | Skriver kod, fixar buggar, implementerar features enligt spec. | Gör det tunga lyftet i Kotlin/Compose/Gradle/Room/MQTT. |
| **Testare (Dev + Automatiserad)** | Skriver och kör tester så långt det är meningsfullt i denna miljö. Ansvarig för testmiljö. | Bygger Docker + Python subscriber, adb-install, logcat-analys, "dev testing" innan du gör UAT. |
| **DevOps & Test Environment Owner** | Ansvarig för byggkedja, testinfrastruktur och reproducerbarhet. | Docker Mosquitto, venv-skript, `adb reverse`, release-tarballs, gradle-ritualer. |
| **Dokumentationsansvarig / Wiki Curator** | Upprätthåller Karpathy-style wiki som levande kunskapskälla. | Skriver/strukturerar sidor, append-only log, nomenclature, Figma-to-Compose-mappning. |
| **Knowledge Keeper & Continuity** | Kommer ihåg tidigare beslut, var saker finns, varför vi valde X istället för Y. | "Som vi sa i Fas 1...", pekar på rätt wiki-sida, undviker att upprepa misstag. |

### Delade eller kontextuella roller

- **Beslutsfattare (Change Control)**: Du har alltid sista ordet. AI-assistenten föreslår + dokumenterar.
- **Manual Tester / UAT-utförare**: Delad. AI-assistenten gör så mycket dev-test och automatiserad validering som möjligt. Du gör den verkliga affärsmässiga och ergonomiska valideringen på telefonen.
- **Arkitekturinput**: Du har rätt (och uppmuntras) att komma med förslag på teknikval och plattformar. AI-assistenten utvärderar, kompletterar och äger den slutliga arkitekturbilden.
- **Release Manager (intern)**: AI-assistenten för interna releaser och tarballs. Du kan ta över när vi går mot extern distribution.

## RACI-exempel (per fas / område)

**R** = Responsible (gör jobbet)  
**A** = Accountable (har sista ansvaret)  
**C** = Consulted (ska tillfrågas)  
**I** = Informed (ska hållas uppdaterad)

| Område                        | Du (Kund/PL) | AI-assistenten (multi-roll) | Kommentar |
|-------------------------------|--------------|-------------------|---------|
| Krav & scope                  | A / R        | C                 | Du bestämmer vad som är värt att bygga |
| Arkitekturval                 | C / I        | A / R             | Du får ge input, AI-assistenten äger helheten |
| Kodimplementation             | I            | A / R             | - |
| Dev testing + testmiljö       | I            | A / R             | AI-assistenten bygger och kör först |
| UAT på riktig telefon         | A / R        | C / I             | Du validerar affärsnytta |
| Wiki / dokumentation          | I            | A / R             | AI-assistenten håller ordning |
| Tekniska beslut (bibliotek etc) | C          | A / R             | - |
| Go / No-go för nästa fas      | A            | C                 | Du fattar beslutet |

## Reviderat arbetssätt (Working Agreement)

### 1. Initiering
- Du (som Projektledare + Kund) ger riktning: "Nu skall vi gå vidare", "Gör Fas 2", "Jag vill ha tydliga teststeg", "Komplettera roller".
- AI-assistenten bryter ner, föreslår plan, låser nomenclature, börjar exekvera.

### 2. Genomförande
- AI-assistenten arbetar i de tekniska rollerna (Architect → Technical Lead → Programmerare → Testare → DevOps → Dokumentatör).
- Allt viktigt dokumenteras i wikin **innan** eller **samtidigt** som det görs (inte efteråt).
- Rich comments i koden + wiki = Architecture-Design-Source-förankring.

### 3. Feedback & Validering
- Du testar på telefon (UAT-roll).
- Feedback ges konkret ("det här funkar", "det här kraschar", "jag saknar X").
- AI-assistenten fixar i Programmerare/Testare-roll + uppdaterar dokumentation.

### 4. Beslut & Eskalering
- Små beslut: AI-assistenten tar i Technical Lead/Programmer-roll.
- Större beslut (arkitektur, scope, teknikstack): AI-assistenten föreslår + dokumenterar, du (PL/Kund) godkänner.
- "Jag agerar kund nu" eller "Jag agerar projektledare nu" är en explicit signal du kan ge.

### 5. Wiki som ryggrad
- `log.md` är append-only kronologisk sanning.
- Strukturerade sidor (Nomenclature, Architecture, Rollfördelning, Figma-spec etc.) är kanoniska referenser.
- Varje ny fas eller större förändring får en egen log-entry + uppdaterad översiktssida.

### 6. Kontinuitet
- Efter paus (t.ex. "Jag bryter för kvällen") fortsätter AI-assistenten med dokumentation, förberedelser och "så här testar du imorgon".
- Du kan alltid säga "Sammanfatta läget" eller "Vad är öppet just nu?".

### 7. Pedagogisk förklaring som arbetssätt

När AI-assistenten förklarar tekniska koncept för Kund/Projektledare används en **pedagogisk mall** som validerats som effektiv:
1. **Vardaglig analogi** – koppla det tekniska till något bekant (kontrollrum, panel, sladdar).
2. **Före/efter-diagram** – Mermaid flowchart som visar nuvarande vs framtida arkitektur.
3. **Tabell med steg** – konkret plan med aktivitet, vad som händer, risk, tidsestimat.
4. **Rakt svar på praktiska frågor** – hur svårt? verktyg? testning? synlig skillnad?

Denna mall används framgent när nya tekniska koncept introduceras för beslut.

## Fördelar med denna modell

- Mindre risk för rollförvirring ("Vem bestämmer egentligen?").
- Bättre spårbarhet (vem sa vad och varför).
- Lättare att skala upp (om vi får in fler personer eller byter AI).
- Du kan medvetet "kliva in i kundrollen" utan att behöva vara teknisk.
- AI-assistenten kan vara aggressiv i tekniska roller utan att överskrida mandat.

## Fas-process och kontinuerliga förbättringar (infört efter Fas 2)

För att göra samarbetet ännu effektivare har vi formaliserat följande efter erfarenheterna från Fas 2:

- **Levande Roadmap som nav**: Varje ny fas börjar med en kort kick-off där vi låser acceptanskriterier i [[Kundrelationer-och-Acceptans]] + uppdaterar [[Produkt-Roadmap]]. Roadmapen är single source of truth för vad som är gjort och planerat.
- **UI-andrum som explicit regel**: Innan kodning av UI sker en "breathing room check" (se detaljer i [[Produkt-Roadmap#fas-3-plan-ui-förbättringar--grundläggande-polish]]). Vi använder befintliga Figma-mocks + textbaserade specar (nomenclature + detaljerade markdown-beskrivningar) för att spara krediter. Nya bilder genereras endast för 1–2 kritiska valideringar per fas.
- **Fas-retro efter UAT/sign-off**: Efter varje godkännande gör vi en kort retro (5–10 min): Vad fungerade i samarbetet? Vad ska vi ändra till nästa fas? (t.ex. detaljnivå, separation mobil vs PC-stöd).
- **Fas-specifika taggar + artifacts**: Systematisk användning av taggar (t.ex. `fas-3-ui-polish-...`) + artifacts under `~/projects/rfid/rfid-manager/releases/` och `artifacts/`.
- **Standardiserad path-struktur**: Allt stödmaterial (test, setup, releases, artifacts) ligger under `~/projects/rfid/rfid-manager/`. Alla framtida referenser i wiki, kod och instruktioner pekar dit för att eliminera "var ligger sakerna?"-friktion.

### 8. Versionshantering & Git Branch-strategi

Vi använder en enkel **master/develop-modell** som passar ett 1-människa + AI-team:

- **`master`** — Stabil/release-klar kod. Endast merges från `develop`. Taggas vid sign-off: `fas-3-sign-off-2026-06-10`.
- **`develop`** — Aktiv utvecklingsbranch. Här samlas alla ändringar mellan releaser. Pushas löpande som backup.
- **Arbetsflöde:**
  1. Utveckling sker lokalt på `develop` (eller feature-branch → `develop`)
  2. Vid fas-godkännande: merge `develop` → `master`, skapa tag, pusha båda
  3. `master` förblir ren och historisk
- **Repositoryn:**
  - `JoaBerra/rfid-manager` (monorepo: wiki + verktyg) — `main`-branch som stabil
  - `JoaBerra/rfid-manager-android` (Android-app) — `master` = stabil, `develop` = aktiv utveckling
- **Commit-meddelanden:** Kort beskrivning på svenska eller engelska, t.ex. `Fas-500: uppdatera MQTT broker IP`.

Dessa punkter uppdateras löpande i denna sida och i [[Kundrelationer-och-Acceptans]].

## Nästa steg (rekommendation)

1. Du (som Projektledare) godkänner eller justerar denna rollfördelning.
2. Vi använder rollerna explicit i framtida dialog ("Som Arkitekt föreslår jag...", "Som Kund – det här duger inte i UAT").
3. Vi uppdaterar denna sida löpande (versionera stora förändringar i log.md).

---

**Skapad**: 2026-06-06 efter din reflektion om roller.  
**Uppdaterad**: 2026-06-23 (funktionell nomenklatur + Grok → AI-assistenten + git branch-strategi).  
**Ägare**: Båda (du som Projektledare har sista ordet).  
**Uppdateras**: Vid varje större fas-skifte eller när ni vill ändra mandat.

Se även:
- [[log]] för den löpande historiken kring detta beslut.
- [[Projektplan]] och [[App-Architecture]] för teknisk styrning.
- [[Nomenclature-Figma-Android]] för hur vi låser begrepp tillsammans.
- [[Kundrelationer-och-Acceptans]] för formella UAT-tester och sign-off i Kund-rollen (inkl. Fas 2-godkännande 2026-06-07).

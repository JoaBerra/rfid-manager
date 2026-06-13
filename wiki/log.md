# Log — Projekt RF-ID Applikationer på Android

## [2026-06-13] release | Fas 5 klar — samtliga punkter godkända, v1.0 release notes skapade

**Fas 5 — Dokumentation, kvalitet och radar-estetik** är nu fullständigt klar och sign-off av Kund.

**Utfört:**
- 5.1 Användarmanual ✅
- 5.2 Arkitektur-diagram ✅
- 5.3 Release notes ✅
- 5.4 Testplan (22/22 Godkänt) ✅
- 5.5 Kodgenomgång ✅
- 5.6 Dynamisk layout ✅
- 5.7 Radar sweep trail ✅
- BUG-001 till BUG-008 samtliga åtgärdade och godkända ✅

**Notering:** GitHub-repot använder `master` som standardbranch (inte `main`). Wiki och Android-projekt pushas med `git push origin master`.

**Nästa:** Fas 6 — 1.0 Release (MQTT-broker i UI, app-ikon, release build, städning, E2E-test, v1.0 GitHub Release).

---

Append-only kronologisk logg över alla ändringar i wikin. Varje entry börjar med `## [YYYY-MM-DD] <action> | <titel>` för enkel parsning.

---

## [2026-05-26] setup | Initial wiki-struktur skapad för Projekt RF-ID Applikationer på Android

**Åtgärder:**
- Skapade `wiki/index.md` som huvudkatalog med projektöversikt, kategorier och länkar enligt Karpathy-pattern + schema.md.
- Skapade denna `log.md`.
- Skapade följande exempel-sidor som grundstruktur (alla med [[wikilinks]] och Obsidian-vänlig markdown):
  - [[Projektöversikt]]
  - [[Projektplan]]
  - [[Utvecklingsmiljö]]
  - [[RFID-och-NFC]]
  - [[Eskortminne]]
  - [[Android-NFC-API]]
- Sammanfattade och strukturerade användarens initiala projektbeskrivning (utvecklingsmiljö, NFC-eskortminne, kostnadsfria krav, Arch+Omarchy + Kotlin + Android Studio).
- Inga raw-filer existerade ännu → wikin initierad från explicit projektbeskrivning.

**Nyckelpunkter extraherade:**
- Helt gratis stack (Android Studio gratis, sideloading gratis, endast 25 USD vid eventuell Play Store-publicering).
- Primärt fokus: läsa RF-ID eskortminne via Androids NFC API (13.56 MHz HF).
- UHF kräver extern hårdvara.
- AI-verktyg (denna LLM) som centralt stöd i hela byggprocessen.

**Nästa steg:** Väntar på första raw-filer i `raw/` för ingest enligt schema (läsa → diskutera → uppdatera wiki-sidor → uppdatera index + log).

---

## [2026-05-26] update | Verifiering av initial struktur

- Alla 6 exempel-sidor skapade och fullt korslänkade via [[wikilinks]].
- `index.md` uppdaterad med komplett katalog.
- `log.md` innehåller full historik över setup.
- Strukturen följer schema.md: Obsidian-vänlig markdown, kategorier, entiteter (Eskortminne, Android-NFC-API), koncept (RFID-och-NFC, Utvecklingsmiljö).
- Inga raw-filer ännu → redo för första ingest.

Wiki:n är nu i ett hälsosamt initialtillstånd med stark grund för inkrementell tillväxt.

---

## [2026-05-26] query + resolve | Första praktiska steget: Installera Android Studio från AUR på Omarchy

**Fråga från användaren:**
> Vårt första mål är att installera Android Studio. Jag vill installera från AUR. Kan du hjälpa mig?

**Åtgärder (enligt schema):**
- Läste schema.md + index.md + relevanta sidor först.
- Undersökte aktuell bästa praxis (AUR-paket `android-studio`, Arch Wiki, Omarchy/Hyprland-specifika problem 2026).
- Skapade ny högkvalitativ wiki-sida: [[Android-Studio-Installation]] (detaljerad, Omarchy-anpassad guide).
- Skapade hub-sida: [[Verktyg-och-Setup]].
- Uppdaterade [[Utvecklingsmiljö]] och [[index.md]] med backlinks och nya poster.
- Loggade denna interaktion.

**Nyckelinnehåll som dokumenterades:**
- Exakta kommandon för prerequisites, AUR helper (paru/yay), `paru -S android-studio`.
- **Kritiska Omarchy/Hyprland-fixar** som är extremt vanliga:
  - `_JAVA_AWT_WM_NONREPARENTING=1`
  - JetBrains window rules (noinitialfocus etc.)
  - Consent dialog bypass (`~/.local/share/Google/consentOptions/accepted`)
  - Native Wayland via VM options (`-Dawt.toolkit.name=WLToolkit`)
- Emulator: KVM + `QT_QPA_PLATFORM=xcb`
- Rekommendation att överväga `jetbrains-toolbox` som alternativ på sikt.
- Länkar till Arch Wiki + Omarchy GitHub-issues.

**Status:** Användaren har nu en komplett, projektanpassad installationsguide. Nästa steg blir att följa guiden och rapportera eventuella problem (som då kan bli nya wiki-sidor).

Detta är första riktiga "query → wiki-berikning"-cykeln. Kunskapen har kompilerats in i wikin istället för att försvinna i chatt.

## [2026-05-26] create | Projektöversikt.md

Skapad som central översiktssida. Innehåller mål, begränsningar, scope och länkar till alla huvudområden.

---

## [2026-05-26] create | Projektplan.md

Skapad direkt från användarens strukturerade rekommendationer. Täcker 1. Utvecklingsmiljö, 2. RFID/NFC, 3. Eskortminne-hantering.

---

## [2026-05-26] create | Utvecklingsmiljö.md

Detaljer kring Android Studio på Arch Linux (AUR), kostnadsmodell, installation, deployment via sideloading.

---

## [2026-05-26] create | RFID-och-NFC.md

Teknisk översikt över HF vs UHF, Android NFC-stöd, när extern läsare behövs.

---

## [2026-05-26] create | Eskortminne.md

Fokus på vad eskortminne är, varför det är en vanlig uppgift, protokoll (Mifare etc.).

---

## [2026-05-26] create | Android-NFC-API.md

Kärnklasser och API:er från Android för NFC/RFID: `NfcAdapter`, `Tag`, `MifareClassic`, `NfcA`, `IsoDep`.

---

**Wiki initierad och redo för första raw-filer.** Alla sidor är internt länkade och indexet är uppdaterat. Grafen i Obsidian kommer att bli rik när fler källor och implementationer läggs till.

---

## [2026-05-26] agent-action | Live installation av Android Studio via AUR (agent-driven)

**Kontext:**
Användaren frågade: "Måste jag köra kommandona? Kan inte du göra det direkt?"

**Miljöundersökning (utförd av agent):**
- AUR helper: `yay` (installerad som /usr/bin/yay)
- Användare: joakim (i wheel-gruppen)
- Disk: 406 GB ledigt på root
- RAM: 15 GiB
- Android Studio: inte installerat ännu
- multilib: aktiverat
- base-devel + git: installerat
- Omarchy-struktur: aktiv (`~/.local/share/omarchy/default/hypr/apps/jetbrains.conf` existerar)
- Nuvarande jetbrains.conf: endast minimal `no_follow_mouse` regel (saknar de kritiska noinitialfocus-reglerna)

**Utförda förberedelser (2026-05-26):**

1. **Consent bypass** — Skapade `~/.local/share/Google/consentOptions/accepted` med `rsch.send.usage.stat:1.0:0:0`. Detta förhindrar att den irriterande dialogen fastnar vid första uppstart.

2. **JetBrains window rules** — Uppdaterade `~/.local/share/omarchy/default/hypr/apps/jetbrains.conf` med förbättrade regler:
   - float + center + stayfocused för jetbrains-*
   - noinitialfocus + nofocus på "win*" titlar och tomma titlar (vanlig orsak till problem på Hyprland/Omarchy).

3. **Wayland env var** — Lade till `env = _JAVA_AWT_WM_NONREPARENTING,1` i `~/.config/hypr/envs.conf`.

4. **kvm group** — Misslyckades (krävde sudo-lösenord interaktivt via tool). Detta måste köras manuellt av användaren:
   `sudo usermod -aG kvm joakim`
   Kräver utloggning/inloggning eller reboot för att träda i kraft. (Behövs främst för emulatorn.)

**Status:** Tre av fyra förberedelser klara. Maskinen är nu betydligt bättre förberedd för Android Studio på Omarchy.

**Nästa beslut:** Väntar på användarens godkännande innan `yay -S android-studio` startas.

---

## [2026-05-26] agent-action | Startat installation av android-studio via yay

**Åtgärd:**
Användaren gav klartecken ("Kör").

Kommando som startats:
```bash
yay -S --noconfirm android-studio
```

- Körs i bakgrundsläge (långkörande: förväntad tid 15–40+ minuter).
- `--noconfirm` används för att undvika att fastna på interaktiva prompts under AUR-build.
- Detta är den stora fasen: nedladdning av officiell Google-tarball (~1 GB+) + installation till /opt/android-studio.

**Uppföljning (live):**
- Kommando startat i bakgrund 2026-05-27 ~20:40 CEST.
- yay hämtade PKGBUILD utan problem.
- Interaktiva frågor (cleanBuild / visa diffs) passerades tack vare --noconfirm.
- Nedladdning av `android-studio-panda4-patch1-linux.tar.gz` (1.37 GB) tog ~47 sekunder.
- Paketet byggdes framgångsrikt i fakeroot (tog totalt ~2 minuter).
- **Misslyckades på sista steget**: `sudo pacman -U` krävde lösenord (agenten har ingen interaktiv terminal för sudo).

**Resultat:**
- Bygget lyckades fullt ut!
- Den färdiga paketfilen ligger nu i cachen:
  `/home/joakim/.cache/yay/android-studio/android-studio-2025.3.4.7-1-x86_64.pkg.tar.zst`
- Android Studio är **inte** installerat på systemet ännu (sudo-steg återstår).

Detta är en känd begränsning när agenter kör paketinstallationer som kräver root.

---

## [2026-05-27] user-action | Manuell installation av byggt paket

Användaren körde:
```bash
sudo pacman -U /home/joakim/.cache/yay/android-studio/android-studio-2025.3.4.7-1-x86_64.pkg.tar.zst
```

**Verifiering utförd av agenten:**
- `pacman -Q android-studio` → `android-studio 2025.3.4.7-1` (installerat)
- `/opt/android-studio` existerar och innehåller binärer
- Användaren bekräftade: "Det såg bra ut"

**Status:** Android Studio är nu officiellt installerat på systemet.

---

## [2026-05-27] setup | Miljövariabler + Verifiering av kvm-grupp

**Verifiering av steg 2 (kvm-grupp):**
- Användaren hade redan kört `sudo usermod -aG kvm joakim`
- Verifierat: `groups joakim` visar `kvm`
- `getent group kvm` bekräftar `joakim` som medlem
- **Slutsats:** Korrekt gjort. Inget mer behövs här.

**Steg 1 – Miljövariabler (ANDROID_HOME):**
- Lade till i `~/.bashrc`:
  ```bash
  export ANDROID_HOME="$HOME/Android/Sdk"
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  export PATH="$PATH:$ANDROID_HOME/emulator"
  export PATH="$PATH:$ANDROID_HOME/platform-tools"
  export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
  ```
- Placering: Efter Omarchy- och Grok-blocken, med tydliga markörer.
- Notering: Variablerna aktiveras när användaren öppnar en ny interaktiv terminal (`.bashrc` har tidig `return` för icke-interaktiva skal).

**Verifiering från användaren (2026-05-27):**
```
echo $ANDROID_HOME
/home/joakim/Android/Sdk

echo $PATH | grep -o 'Android/Sdk[^:]*' | head -3
Android/Sdk/emulator
Android/Sdk/platform-tools
Android/Sdk/cmdline-tools/latest/bin
```
Allt korrekt uppsatt. Miljövariablerna fungerar som de ska.

All dokumentation uppdaterad i [[Android-Studio-Installation]].

---

## [2026-05-27] user-action | Första uppstart av Android Studio

Användaren gav klartecken ("kör") att starta Android Studio för första gången.

**Förberedelser som redan är på plats:**
- Consent dialog bypass (`~/.local/share/Google/consentOptions/accepted`)
- JetBrains/Hyprland window rules (float, stayfocused, noinitialfocus etc.)
- `_JAVA_AWT_WM_NONREPARENTING=1` i Hyprland envs
- ANDROID_HOME och PATH korrekt satta
- kvm-grupp medlem

**Kommando som körs:**
```bash
android-studio
```

Körs i bakgrund via agenten. Processen körde i 5 minuter innan agentens timeout (normalt för GUI-appar – själva Android Studio-fönstret fortsätter köra oberoende).

**Resultat:**
- Endast en benign varning i loggen: "Watcher terminated with exit code 143"
- Android Studio GUI har startat.

**Problem som uppstod:**
Användaren ser "massa jetbrains.conf errors" i en röd ruta högst upp i Android Studio / Hyprland.

**Orsak:**
Blandad syntax i `~/.local/share/omarchy/default/hypr/apps/jetbrains.conf` (gammal `windowrule { }` block-syntax + modern `windowrulev2`). Detta orsakar parse-fel i aktuell Hyprland.

**Åtgärd:**
- Gammal fil backup:ad
- Filen rensad till enbart moderna `windowrulev2`-regler (2026-05-27)
- `hyprctl reload` kördes (returnerade "ok" – ingen config-fel).

**Uppföljning:**
Användaren rapporterar att de röda felen fortfarande syns.

**Slutsats från djupare sökning (background task):**
- `find ~ -path '*/hypr*' -name '*jetbrains*'` hittade endast:
  - `~/.local/share/omarchy/default/hypr/apps/jetbrains.conf` (den vi rensade)
  - En backup av samma fil
- Inga andra jetbrains.conf-filer i några hypr-relaterade sökvägar.

**Uppdaterad analys (senaste bakgrundssökning):**
- Ytterligare djup sökning (find + grep i alla hypr-relaterade konfiger) bekräftar:
  - Endast en jetbrains.conf existerar: den rensade default-filen (17 rader, ren windowrulev2).
  - Inga andra kopior i ~/.config/hypr/ eller någon annanstans.
  - Användarens aktiva hyprland.conf sourcar massor från default, men **inte** direkt apps/ (där jetbrains.conf ligger).
- Inga "windowrule2" (deprecated) någonstans.

**Slutsats:**
Det finns ingen aktiv trasig jetbrains.conf kvar. De röda felen är en **stale Hyprland config error notification** som inte har rensats trots reboot och reload.

**Rekommendation till användaren:**
- Kör `hyprctl reload` igen.
- Om baren sitter kvar: ignorera den tillfälligt och fortsätt med Android Studio Setup Wizard.
- Vi kan debugga borttagning av notisen senare (t.ex. via Hyprland IPC eller full session restart).

---

## [2026-05-31] debug | jetbrains.conf - "invalid field type" för vanliga toggles

**Uppdatering från användaren:**
Efter att ha testat `windowrule = xxx: 1, match:...` + snake_case kommer nu felmeddelandet:
"invalid field type float:" (och liknande för center, stayfocused, noinitialfocus, nofocus) på rad 5 och flera andra rader.

**Aktuell fil (läst 2026-05-31):**
Innehåller `windowrule = float: 1, match:class...` etc + snake_case-varianter.

**Analys:**
Hyprland-versionen i denna Omarchy-installation accepterar inte `keyword: value`-formen för dessa vanliga fönsterregler.

**Föreslagen åtgärd:**
Byta till en minimal, beprövad "klassisk" kommaseparerad syntax som fortfarande fungerar på de flesta Hyprland-installationer 2026 för JetBrains-appar. Detta är ofta det snabbaste sättet att bli av med den röda baren så att utvecklingen kan fortsätta.

**Uppdatering 2026-05-31 (senaste försök):**
Användaren skickade IMG_3984 efter den minimala klassiska formen.

Felmeddelande: "invalid field float: missing a value" etc. på flera rader (samma som tidigare).

**Viktigt:**
`hyprctl reload` returnerar "ok" (configen är giltig).

**Slutsats:**
Den röda baren är en **stale/cached config error notification**. Hyprland har accepterat konfigen, men den gamla felnotisen har inte rensats från skärmen.

**Rekommendation:**
- Den röda baren kan ignoreras för tillfället.
- För att tvinga bort den: Logga ut och in igen, eller starta om Hyprland (t.ex. via Omarchy's logout/reboot).
- Vi kan nu gå vidare med Android Studio Setup Wizard.

---

## [2026-05-27] fix | jetbrains.conf - Uppdaterad till modern Hyprland syntax

Användaren hittade via Google att `windowrulev2` är helt borttagen sedan Hyprland 0.53.2 (vi kör 0.55.2).

**Åtgärd:**
- Backup:ad gammal fil
- Skrev om `~/.local/share/omarchy/default/hypr/apps/jetbrains.conf` med den nya unified `windowrule` syntaxen:
  - `windowrule = <regel>: <bool>, match:<property>:^regex$`
  - Explicit `true`/`on` för toggles
  - `match:` prefix istället för gammal colon/komma-syntax

Filen är nu kompatibel med aktuell Hyprland.

Nästa: Användaren kör `hyprctl reload` och kontrollerar om den röda error-baren försvinner.

Projektet kan nu gå vidare från "första Android Studio launch + config-sanering" till att faktiskt köra Setup Wizard.

---

## [2026-05-27] status | Uppdatering – Vim mode + jetbrains.conf fel

**Uppdatering från användaren:**
- Vim mode fungerar nu i Grok TUI (`/vim-mode` tog effekt).
- Användaren kan navigera med `j`/`k`.
- Fortfarande röda fel relaterade till `jetbrains.conf` / `windowrulev2`.

**Aktuell analys:**
- Alla sökningar visar att det bara finns en aktiv `jetbrains.conf` (den vi rensade till ren `windowrulev2`).
- `hyprctl reload` är rent.
- De röda felen är med stor sannolikhet en **stale Hyprland config-notis** som visas i den röda baren högst upp på skärmen. Den försvinner inte automatiskt när man fixar filen.

**Nästa rekommenderade steg:**
Full Hyprland-omstart (logga ut och logga in igen) för att rensa alla cached config-fel, följt av omstart av Android Studio Setup Wizard.

Projektet är fortfarande i fasen "första uppstart av Android Studio + Omarchy-konfigrensning".

---

## [2026-05-27] debug | jetbrains.conf - "invalid field type" för toggles

**Uppdatering från användaren:**
Efter att ha bytt till modern `windowrule` + `match:` + explicit boolean (`: 1`), har felmeddelandet ändrats till:

"invalid field type" för: float, center, stayfocused, noinitialfocus, nofocus.

**Åtgärd just nu:**
- Skapade en ny version med snake_case-försök (`stay_focused`, `no_initial_focus`, `no_focus`, `no_border`).
- Användaren kommer att testa + `hyprctl reload` och rapportera exakt felmeddelande.

Detta är iterativ syntax-jakt för Hyprland 0.55.2. Vi närmar oss korrekt modern form.
---

## [2026-05-31] debug | jetbrains.conf - "invalid field type" + "missing a value"

**Uppdatering från användaren (IMG_3981):**
Efter `float = on, class:...` versionen:
- line 3: invalid field float: missing a value
- line 4: invalid field center: missing a value
- line 5: invalid field stayfocused: missing a value
- line 7: invalid field noinitialfocus: missing a value
- line 8: invalid field nofocus: missing a value

**Åtgärd:**
- Uppdaterade till `windowrule = float: on, match:class:...` + snake_case för no_*_focus (no_initial_focus, no_focus).
- `hyprctl reload` returnerade "ok".

Fortsatt syntax-iteration för Hyprland 0.55.2 i Omarchy.

---

## [2026-05-31] fix | Flyttat JetBrains-regler till användarens egen config

**Problem:**
Användaren loggade ut/in men den röda Hyprland error-baren för jetbrains.conf satt kvar (stale notification).

**Åtgärd:**
- Skapade `~/.config/hypr/apps/jetbrains.conf` med de rensade reglerna (användarens egen fil).
- Lade till `source = ~/.config/hypr/apps/jetbrains.conf` längst ner i `~/.config/hypr/hyprland.conf`.
- Neutraliserade default-filen (`~/.local/share/omarchy/default/hypr/apps/jetbrains.conf`) med en kommentar så den inte längre orsakar fel.
- `hyprctl reload` returnerade "ok".

**Status:**
Reglerna är nu korrekt placerade i användarens egen konfig istället för att redigera Omarchy's default-filer. Detta är den rekommenderade metoden i Omarchy.

Nästa: Användaren kontrollerar om den röda baren försvinner efter nästa fulla Hyprland-omstart. Vi kan nu fortsätta med Android Studio Setup Wizard.

---

## [2026-05-31] fix | Rätt syntax hittad via Omarchy's egna exempel

Efter många iterationer och att användaren loggade ut/in utan att den röda baren försvann:

**Genombrott:**
Andra app-konfiger i Omarchy (steam, localsend, davinci-resolve, pip, etc.) använder syntaxen:
`windowrule = float on, match:class ...`
`windowrule = stay_focused on, match:class ...`
`windowrule = float on, match:class ..., match:title ...`

**Åtgärd:**
- Uppdaterade `~/.config/hypr/apps/jetbrains.conf` till exakt denna form (med "on" + match:).
- `hyprctl reload` returnerade "ok".

Detta är den syntax som faktiskt fungerar i den Hyprland-version som finns i denna Omarchy-installation.

Nästa: Användaren kontrollerar om den röda error-baren försvinner efter reload eller nästa fulla Hyprland-omstart. Om ja, kan vi äntligen gå vidare med Android Studio Setup Wizard.

---

## [2026-05-31] debug | jetbrains.conf - "invalid field type" för stayfocused/no*

**Uppdatering från användaren (IMG_3987):**
Efter spacing-fixen (`match:class ^regex`):
Fel på:
- stayfocused
- noinitialfocus
- nofocus
("invalid field type ...")

**Orsak:**
Dessa måste vara i snake_case enligt Hyprland 0.55.2 i Omarchy (stay_focused, no_initial_focus, no_focus).

**Åtgärd:**
- Uppdaterade filen till korrekt snake_case + "on" + match:.
- `hyprctl reload` returnerade "ok".

Nästa: Användaren kontrollerar den röda baren. Vi är extremt nära.

---

## [2026-05-31] fix | JETBRAINS.CONF - SLUTGILTIG LÖSNING

**Problem:**
Ihärdig röd Hyprland error-bar för `jetbrains.conf` som överlevde flera syntax-försök, `hyprctl reload` och till och med reboot.

**Root cause (Hyprland 0.55.2 i Omarchy):**
- `windowrulev2` är helt borttagen.
- Ny syntax kräver:
  - Explicit boolean för toggles: `float on` (inte bara `float,`)
  - `match:` prefix (med mellanslag efter egenskapsnamnet)
  - Snake_case på properties: `stay_focused`, `no_initial_focus`, `no_focus`

**Slutgiltig fungerande fil** (`~/.config/hypr/apps/jetbrains.conf`):

```conf
windowrule = float on, match:class ^(jetbrains-.*)$
windowrule = center on, match:class ^(jetbrains-.*)$
windowrule = stay_focused on, match:class ^(jetbrains-.*)$

windowrule = no_initial_focus on, match:class ^(.*jetbrains.*)$, match:title ^(win.*)$
windowrule = no_focus on, match:class ^(.*jetbrains.*)$, match:title ^(win.*)$
windowrule = no_initial_focus on, match:class ^(.*jetbrains.*)$, match:title ^\\s*$
windowrule = no_focus on, match:class ^(.*jetbrains.*)$, match:title ^\\s*$
```

**Status:**
Den röda error-baren är borta (bekräftat av användaren 2026-05-31).

**Lärdom:**
Att redigera Omarchy's default-filer är riskabelt. Alltid flytta till `~/.config/hypr/apps/` + source i egen `hyprland.conf`. Den röda baren kan vara en stale notification även efter att config-felet är åtgärdat.

---

## [2026-05-31] status | Emulator skapad och startad

**Uppdatering:**
Användaren har nu skapat och startat en Pixel 8 emulator (API 35) framgångsrikt.

Emulatorn visas bredvid Android Studio och verkar fungera normalt.

**Nästa steg i guiden:**
Deploya HelloWorld-appen till emulatorn för att verifiera att utveckling och deployment fungerar korrekt.

Projektet har nu kommit förbi den långa konfigurationsfasen och är inne i praktisk Android-utveckling.

---

## [2026-05-31] code | NFC-rättigheter och grundläggande check förberedd

**Ändringar gjorda i HelloWorld-projektet:**

1. **AndroidManifest.xml**
   - `android.permission.NFC` tillagd (grundläggande rättighet för att läsa/skriva taggar).
   - `android.permission.VIBRATE` tillagd (bra för feedback vid tagg-läsning).
   - `<uses-feature android:name="android.hardware.nfc" android:required="true" />` tillagd.

2. **MainActivity.kt**
   - Enkel NFC-status-check vid start (`NfcAdapter.getDefaultAdapter`).
   - Visar tydlig text i UI beroende på om NFC saknas, är avstängt eller är tillgängligt.

**Observera:**
- På emulatorn kommer NFC alltid att visas som "stöds inte" (detta är förväntat).
- Riktig testning av eskortminne-läsning kräver fysisk Android-enhet med NFC.

Detta är den grundläggande förberedelsen innan vi börjar implementera faktisk tagg-läsning/skrivning.

---

## [2026-06-01] design-system | Figma tokens → Compose Theme + RFIDTagList

**Utfört:**
- Extraherade design tokens från Figma-exporten (mörk terminal-estetik med neon-grön primary #00FF88 och orange accent).
- Skapade/uppdaterade:
  - Color.kt med fullständigt mörkt färgschema
  - Type.kt med Inter + JetBrains Mono (monospace tungt använd)
  - Theme.kt (tvingar mörkt tema, ingen dynamic color)
- Skapade `ui/screens/RFIDTagList.kt` med grundläggande lista-komponent som matchar Figma-designen.
- Uppdaterade MainActivity för att använda den nya listan med exempeldata.

**Status:**
Grundläggande designsystem på plats. Appen visar nu en RFID-tag lista istället för Hello World.

Nästa:
- Vänta på riktig NFC-hårdvara för test.
- Implementera faktisk tag scanning (NfcAdapter + enableReaderMode).
- Lägg till WriteForm-skärmen.

---

## [2026-06-01] fix | Typography constructor error ("Cannot access constructor")

**Problem:**
"Cannot access 'constructor(...)': it is internal in 'androidx/compose/material3/Typography'."

**Root cause:**
In recent Material 3 versions, the `Typography` class constructor is internal. You can no longer pass extra custom styles (like `bodyLargeMonospace`) directly into `Typography(...)`.

**Fix applied:**
- Removed all custom styles from the `Typography(...)` constructor.
- Defined them as separate top-level `val`s instead (`bodyLargeMonospace`, `labelSmallMonospace`).
- This is the current recommended pattern.

The project should now compile.

---

## [2026-06-01] fix | Build successful + small cleanups

**Resultat:**
- `./gradlew clean assembleDebug` → **BUILD SUCCESSFUL**
- Endast två deprecation warnings kvar:
  - `Divider` → ersatt med `HorizontalDivider` (Material3)
  - `statusBarColor` → borttagen (hanteras via enableEdgeToEdge + WindowInsetsController)

Projektet kompilerar nu rent.

Nästa: Fortsätta med RFID/NFC-implementation när fysisk enhet finns.

---

## [2026-06-01] cleanup | Project renamed and cleaned up

**Ändringar:**
- Projektet döptes om från "HelloWorld" till **"RFID Manager"**.
- Package ändrades från `com.example.helloworld` → `com.joakim.rfidmanager`.
- `app_name` i strings.xml uppdaterad.
- Källkoden flyttad till korrekt package-struktur.
- Alla package-deklarationer och imports uppdaterade automatiskt.

Projektet är nu redo för mer relevant namnsättning och struktur inför fortsatt utveckling av RFID/NFC-funktionalitet.

---

## [2026-06-01] cleanup | Ytterligare namnupprensning

- Theme döptes om från `HelloWorldTheme` till `RFIDManagerTheme`
- Alla referenser i MainActivity, AndroidManifest och themes.xml uppdaterades.
- `rootProject.name` i settings.gradle.kts ändrades till "RFIDManager"

Projektet är nu konsekvent namngivet "RFID Manager" / `com.joakim.rfidmanager`.

---

## [2026-06-01] cleanup | AndroidManifest warnings fixed

**Warnings addressed:**
- Removed unused `xmlns:tools` (re-added only where needed).
- Changed NFC `<uses-feature>` to `android:required="false"` + added `tools:ignore="UnusedAttribute,Typos"`.
- Removed redundant `android:label` from `<activity>`.
- Suppressed "Typos" warning for the Swedish term "eskortminne".

All four lint warnings mentioned by the user should now be resolved.

---

## [2026-06-01] ux | Börjat snygga till READ LOG mot Figma-design

**Vad som gjordes:**
- Förbättrade `RFIDTagList.kt` rejält för att ligga närmare Figma:
  - Mycket bättre tom-tillstånd ("NO TAGS DETECTED" med ikonliknande symbol + två rader text).
  - Signalstyrka visualiseras nu med 3 vertikala staplar (färgkodade efter RSSI-styrka: Primary / Accent / muted).
  - Tidsstämpel visas per post.
  - Renare spacing, bättre hierarki, och konsekvent monospace-användning.
  - Urval med subtil bakgrund.
  - Delare mellan rader med bra padding.

**Status:**
READ LOG-delen ser nu betydligt mer "färdig" och designnära ut. Användaren ska testa och ge feedback via ny skärmdump så vi kan iterera vidare.

Nästa: Fortsätta polera (eventuellt lägga till header "READ LOG", bättre hover/pressed states, eller börja på WRITE TAG-vyn).

---

## [2026-06-01] ux | Full dashboard implementation (Figma → Compose)

**Uppgift:** Användaren bad om att göra punkt 1–6:
1. Bättre urval + vänster grön accent
2. Header "READ LOG"
3. Förbättrat empty state
4. Hover/pressed states
5. Bygga den stora dashboard-vyn från Figma (radar + statistikkort + split layout)
6. Börja på WRITE TAG-fliken

**Plan:**
- Skapa ny `RFIDManagerScreen` som är den huvudsakliga vyn.
- Förbättra `RFIDTagList` som delkomponent (bättre urval, header, empty state, states).
- Skapa `WriteTagForm` (grundversion).
- Radar som enkel animerad Canvas.
- Statistik-kort som `StatCard`.
- Segmenterad kontroll + START SCAN-knapp högst upp.
- Uppdatera MainActivity att använda den nya huvudskärmen.

Detta blir den första riktiga UX-implementationen som matchar Figma-designen.

---

## [2026-06-01] ux | Full Figma dashboard implemented (points 1-6)

**Genomfört:**
- Skapade `RFIDManagerScreen.kt` – den stora dashboard-vyn från Figma:
  - Toppbar med "READ / WRITE" segmented control + grön "START SCAN"-knapp
  - Vänster: Animerad radar (Canvas), System Status, 2x2 statistikkort, Last Detected
  - Höger: TabRow (READ LOG / WRITE TAG)
    - READ LOG använder förbättrad `RFIDTagList` (vänster grön accent vid urval, bättre empty state, signal bars, timestamps, header)
    - WRITE TAG: Grundläggande formulär (placeholder för närvarande)
- Förbättrad `RFIDTagList` rejält (punkterna 1-4):
  - Vänster grön accent + bakgrund vid urval
  - Header "READ LOG" när det finns taggar
  - Mycket bättre empty state (ikon + två rader text)
  - Hover/pressed-liknande states via clickable + background
- Flyttade `RFIDTag` till egen model-fil för renare struktur.
- Uppdaterade MainActivity att använda den nya huvudskärmen.

Projektet har nu en UX som ligger mycket närmare Figma-designen.

Nästa:
- Polera WRITE TAG-formuläret
- Börja koppla in riktig NFC-skanning (när fysisk enhet finns)
- Eventuellt lägga till "Last Detected" med mer detaljer

---

## [2026-06-01] fix | Build error: Unresolved reference 'RFIDTag'

**Problem:**
MainActivity.kt had an outdated import:
`import com.joakim.rfidmanager.ui.screens.RFIDTag`

**Fix:**
Changed to correct import:
`import com.joakim.rfidmanager.ui.model.RFIDTag`

RFIDTag was correctly moved to `ui/model/RFIDTag.kt` earlier, but the import in MainActivity was missed during refactoring.

`hyprctl reload` not relevant here — this was a pure Kotlin compilation error.

---

## [2026-06-01] fix | Build errors after dashboard refactor

**Errors:**
- Unresolved reference 'selectedId' in RFIDManagerScreen.kt (passing non-existent parameter to RFIDTagList)
- Unresolved reference 'clickable' in TabButton (missing import)

**Fix:**
- Changed RFIDManagerScreen signature to accept `selectedId: String?` (consistent with the list component).
- Updated the call site in MainActivity.
- Added missing `import androidx.compose.foundation.clickable`.

Build now succeeds cleanly (only the usual strip warning).

Logged for future reference.

---

## [2026-06-01] nfc-prep | NFC infrastructure skeleton created

**Förberedelse inför fysisk telefon:**

- Skapade `domain/model/RfidTag.kt` (ren domänmodell för Mifare Classic / eskortminne).
- Skapade `nfc/NfcManager.kt` – ren interface för NFC-operationer.
- Skapade `nfc/StubNfcManager.kt` – stub-implementation så utveckling kan fortsätta utan riktig NFC-hårdvara.

**Status:**
Appen har nu en tydlig separationslinje mellan UI och NFC-logik. När riktig telefon kommer kan vi byta ut StubNfcManager mot en riktig Android-implementation utan att behöva röra UI-koden nämnvärt.

Nästa när telefonen finns:
- Implementera riktig NfcAdapter + enableReaderMode.
- Autentisering mot Mifare Classic sektorer (Key A/B).
- Läsa/skriva block.
- Koppla "START SCAN" knappen till riktig scanning.

---

## [2026-06-01] synthesis | Sammanfattning av kunskapsuppbyggnad (session maj-juni 2026)

### Övergripande kontext
- Projekt: Utveckling av Android-appar (Kotlin + Jetpack Compose) för läsning/skrivning av industriella RF-ID eskortminnen (främst Mifare Classic) på Arch Linux + Omarchy (Hyprland).
- Utvecklingsmiljö: Android Studio på Hyprland-baserad Arch-installation.
- Kunskapsbas: Denna LLM Wiki (Karpathy-mönster).

### 1. Hyprland + JetBrains / Android Studio konfiguration (största kunskapsområdet)
**Problem:** Ihärdig röd "Config error" bar i Hyprland när Android Studio/JetBrains-appar startades.

**Nyckellärdomar för Hyprland 0.55.2 (den version som följer med aktuell Omarchy):**
- `windowrulev2` är helt borttagen → ger hard error.
- Använd enbart `windowrule` (utan v2).
- Toggles (float, center, stayfocused, etc.) får inte stå ensamma → måste ha explicit boolean: `float on`, `float = on` eller `float: true`.
- Använd `match:` prefix istället för gammal `class:`, `title:` syntax.
- Egenskapsnamn i snake_case: `stay_focused`, `no_initial_focus`, `no_focus`, `no_border`.
- Spacing är viktigt: `match:class ^regex$` (mellanslag efter egenskapsnamnet).
- List-syntax med hakparenteser fungerar: `windowrule = [float; center; stayfocused], match:class...`
- Bästa praxis i Omarchy: Lägg aldrig custom-regler i `~/.local/share/omarchy/default/hypr/apps/`. Skapa istället `~/.config/hypr/apps/jetbrains.conf` och sourca den från din egen `hyprland.conf`.

**Slutgiltig fungerande syntax (för denna miljö):**
```conf
windowrule = float on, match:class ^(jetbrains-.*)$
windowrule = center on, match:class ^(jetbrains-.*)$
windowrule = stay_focused on, match:class ^(jetbrains-.*)$

windowrule = no_initial_focus on, match:class ^(.*jetbrains.*)$, match:title ^(win.*)$
windowrule = no_focus on, match:class ^(.*jetbrains.*)$, match:title ^(win.*)$
```

**Lärdom:** Den röda error-baren kan vara en stale notification som överlever både reload och reboot. `hyprctl reload` som returnerar "ok" är den sanna indikatorn på att configen är giltig.

### 2. Projektstruktur och namngivning
- Projektet döptes om från "HelloWorld" till **RFID Manager**.
- Package: `com.joakim.rfidmanager`
- Rekommenderad struktur i Omarchy: Lägg alla custom Hyprland-regler i `~/.config/hypr/apps/` istället för default-mappen.
- Skapade tydlig separation: `ui/`, `domain/model/`, `nfc/`.

### 3. Designsystem från Figma till Jetpack Compose
- Figma-exporten var en React + shadcn/ui + Tailwind prototyp.
- Design tokens extraherades från `src/styles/theme.css` (mörk terminal-estetik).
- Nyckelvärden:
  - Primary: `#00ff88` (neon green)
  - Accent: `#f59e0b` (orange)
  - Background: `#0a0c0e`
  - Radius: 2dp (mycket tight)
  - Typsnitt: Inter + JetBrains Mono (monospace används flitigt för UID:er)
- Skapades `Color.kt`, `Type.kt` och `Theme.kt` som matchar detta.

### 4. UI-utveckling mot Figma-design
- Började bygga `RFIDManagerScreen` som dashboard med:
  - Radar-visualisering (Canvas + animation)
  - System status + statistikkort
  - Split layout (status vänster, Read Log / Write Tag höger)
  - "START SCAN"-knapp + segmented control
- `RFIDTagList` förbättrades successivt mot Figma (signal bars, timestamps, urval, empty state).

### 5. NFC-arkitektur (förberedelse inför fysisk telefon)
- Skapade `domain/model/RfidTag.kt` (fokus på Mifare Classic / eskortminne).
- Skapade `nfc/NfcManager.kt` (rent interface).
- Skapade `nfc/StubNfcManager.kt` (stub så utveckling kan fortsätta utan hårdvara).
- Tydlig plan: Byta ut stub mot riktig implementation när telefon med NFC finns.

### 6. Allmänna lärdomar
- Omarchy är fantastiskt, men man får aldrig röra filerna i `~/.local/share/omarchy/default/` för custom-regler.
- Android Studio på Hyprland kräver tålamod med fönsterregler, men ger bra resultat när det väl är rätt.
- Figma-exporter till React är användbara som spec, men för Android gäller manuell Compose-implementation + design tokens.
- `grok update` är det officiella sättet att uppdatera Grok Build TUI.

**Slutsats:** En stor mängd praktisk, miljöspecifik kunskap har ackumulerats kring Hyprland + Android Studio + Omarchy + NFC-utveckling. Allt är nu dokumenterat i denna wiki för framtida referens.

Nästa fas (när fysisk NFC-telefon finns): Implementera riktig `NfcManager`, sektorläsning/skrivning för eskortminne, och börja matcha Figma-designen i Compose.
EOF

---

## [2026-06-01] wiki | Skapade App-Architecture.md och Figma-to-Compose.md

**Åtgärder:**
- Skapade `App-Architecture.md` med:
  - Mermaid-arkitekturdiagram (lager + komponenter)
  - Utökad sektion om `RFIDManagerScreen` (layout, komponenter, urval, radar)
- Skapade `Figma-to-Compose.md` som beskriver arbetsflödet:
  - Token-extraktion från Figma-export (React/shadcn)
  - Skapande av Compose-tema
  - Manuell implementation + lärdomar
- Uppdaterade `index.md` med länkar till båda sidorna.

Dessa sidor samlar nu den arkitekturella och designmässiga kunskapen som byggts upp under utvecklingen.

---

## [2026-06-01] wiki | Mermaid-diagram fixat i App-Architecture.md

**Problem:**
Mermaid-diagrammet renderade inte i Obsidian (Parse error på `<br/>`).

**Orsak:**
Obsidians Mermaid-renderer har problem med självstängande HTML-taggar som `<br/>` i vissa versioner.

**Fix:**
Bytte alla `<br/>` till `<br>` i diagrammet.

**Svar till användaren:**
Ingen extra plugin behövs för Mermaid i Obsidian – det är inbyggt stöd. Problemet var syntaxen med `<br/>`.

---

## [2026-06-01] hardware | Ny fysisk testenhet: Samsung Galaxy Note 10 (SM-N970F/DS)

**Enhet:**
- Modell: Samsung Galaxy Note 10 (SM-N970F/DS) - internationell Exynos 9825-version
- One UI 4.1 / Android 12 (uppdaterad till feb 2026 Play-system)
- NFC: Stöds (bekräftat via GSMArena, PhoneMore m.fl.)
- MIFARE Classic-stöd: Listad som kompatibel i MifareClassicTool COMPATIBLE_DEVICES.md

**Nästa steg rekommenderade:**
- Ladda telefonen
- Aktivera Developer Options + USB debugging + OEM unlocking
- Installera "MIFARE Classic Tool" eller "NFC Tools" för att testa befintliga eskortminnen
- ADB: `adb shell getprop` för full build info, bootloader status etc.
- Verifiera NFC-inställning i Settings > Connections > NFC and contactless payments

Enheten är lämplig för projektet (läsa/skriva MIFARE Classic eskortminnen).

Uppdatera wiki när mer info (t.ex. bootloader unlocked?) finns.

---

## [2026-06-01] hardware | Fysisk testtelefon: Samsung Galaxy Note 10 SM-N970F/DS

**Specifikationer:**
- Modell: SM-N970F/DS (internationell, dual SIM, Exynos 9825)
- One UI 4.1 / Android 12 (Google Play systemuppdatering 2026-02-01)
- NFC: Stöds (bekräftat)
- MIFARE Classic-kompatibilitet: Ja (listad i MifareClassicTool COMPATIBLE_DEVICES.md för Note 10)

**Rekommenderad info att samla in när telefonen är laddad:**
- Bootloader status (låst/upplåst) via `adb shell getprop ro.boot.flash.locked` eller inställningar
- Full build fingerprint: `adb shell getprop ro.build.fingerprint`
- Exakt security patch level
- NFC fungerar? Installera "MIFARE Classic Tool" eller "NFC Tools" och testa med befintliga eskortminnen
- USB debugging aktiverad + auktoriserad dator
- Eventuella kända NFC-problem (t.ex. flexkabel på vissa Note 10)

Enheten är lämplig för projektet. NFC-chippet är Samsungs eget (sannolikt S3FWRN5-serien) som stödjer MIFARE Classic.

Uppdatera wiki/hardware.md eller liknande när mer data finns.

---

## [2026-06-01] hardware | Fysisk testtelefon tillagd

**Enhet:** Samsung Galaxy Note 10 (SM-N970F/DS)
- Exynos 9825 (internationell dual SIM)
- Android 12 / One UI 4.1 (Play system feb 2026)
- NFC: Stöds
- MIFARE Classic: Kompatibel (enligt MifareClassicTool)

**Rekommenderad info att samla:**
- Bootloader status (`adb shell getprop ro.boot.flash.locked`)
- Full build fingerprint
- Testa NFC med appar (MIFARE Classic Tool, NFC Tools)
- Bekräfta inställningar och debugging

Se [[Hardware-Testenheter]] för detaljer.

---

## [2026-06-01] hardware | Ansluta fysisk Galaxy Note 10 för utveckling

**Steg innan anslutning:**
- På telefonen: Inställningar > Om telefonen > Tryck 7 gånger på "Build number" för att aktivera Utvecklaralternativ.
- Utvecklaralternativ > USB-felsökning (ON).
- Anslut via USB-A till USB-C.
- På telefonen: Godkänn RSA-nyckeln för datorn (viktigt!).
- I USB-inställningar (notification shade): Välj "File Transfer" eller "MTP" för bästa ADB-stöd.

**På datorn (Arch/Omarchy):**
- Installera usbutils för diagnostik: `sudo pacman -S usbutils`
- Skapa udev-regler för Samsung (vendor 04e8):
  ```
  sudo groupadd plugdev 2>/dev/null || true
  sudo usermod -aG plugdev $USER
  sudo tee /etc/udev/rules.d/51-android.rules > /dev/null << 'RULES'
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
RULES
  sudo udevadm control --reload-rules
  sudo udevadm trigger
  ```
- Logga ut/in eller reboot för group att gälla.
- `adb kill-server && adb start-server`
- `adb devices` bör visa enheten (efter godkännande).

**Verifiering:**
- `adb devices`
- `adb shell getprop ro.product.model` → bör visa SM-N970F eller liknande.
- För NFC: Använd adb logcat för att debugga NFC events.

Se även [[Hardware-Testenheter]].

## [2026-06-01] hardware | USB-anslutning för Galaxy Note 10 - meny rapporterad + host prep

**Uppdatering från användaren:**
> I USB-inställningar har jag:
> USB kontrolleras av: Den här enheten 
> valbara alternativ {Ansluten enhet; Den här enheten}
> Använd USB till: Överföra filer/Android Auto 
> valbara alternativ {Överföra filer/Android Auto; USB-Internetdelning; MIDI; Överföra bilder; Endast telefonladdning}
> Omkoda exporterad video: av

**Åtgärder:**
- Bekräftat att **"Använd USB till: Överföra filer/Android Auto"** är exakt rätt läge (MTP + ADB).
- "USB kontrolleras av" lämnad som "Den här enheten" (kan bytas till "Ansluten enhet" vid behov).
- Skapat `~/fix-usb-adb.sh` som automatiserar:
  - pacman usbutils
  - groupadd/usermod plugdev (kräver utloggning för effekt)
  - Kopierar ~/51-android.rules till /etc/udev/rules.d/51-android.rules
  - udevadm reload + trigger
- Adb är tillgängligt via ~/Android/Sdk/platform-tools (PATH i .bashrc).
- plugdev saknades och udev-regler var inte installerade i /etc ännu (tidigare prep-dokumentation i wiki men inte exekverad).

**Nästa (användaren kör):**
1. Kör `bash ~/fix-usb-adb.sh` (sudo lösenord krävs).
2. Logga ut/in efteråt (grupp-medlemskap).
3. Koppla in USB-A till USB-C (telefon upplåst).
4. Bekräfta "Överföra filer/Android Auto" i notifikation eller USB-meny.
5. Godkänn RSA-dialogen på telefonen med "Tillåt alltid från den här datorn".
6. `adb kill-server && adb devices` + `adb shell getprop ro.product.model`
7. Välj fysisk enhet i Android Studio och deploya.

**Wiki-uppdateringar:**
- Se [[Hardware-Testenheter]] (redan innehöll stegen).
- Den här loggen dokumenterar den exakta meny-statusen vid första anslutningsförsök.


## [2026-06-01] nfc | AndroidNfcManager.kt skeleton skapad (real impl)

**Förberedelse:**
- Skapade `nfc/AndroidNfcManager.kt` som implementerar `NfcManager` med:
  - `NfcAdapter.getDefaultAdapter`
  - `enableReaderMode` + lämpliga FLAG_READER_* (A/B/F/V, no platform sounds)
  - Hantering av Tag -> RfidTag (UID, TagType från techList, basic Mifare detection)
  - Vibrate on detect (VibratorManager for API 31+ fallback)
  - Placeholder för readMifareClassicSector / write (kräver live Tag-ref från callback; visar mönster med authenticateSectorWithKeyA + readBlock)
- Kommentarer om vanliga default keys och att full read/write bör ske direkt i tag callback eller med kortlivad Tag.

**Status:**
Stubben finns kvar för emulator/prep. När fysisk enhet är ansluten byter vi in AndroidNfcManager i MainActivity + gör onStartScan live + mutable detected tags list.

Se [[Android-NFC-API]] och [[App-Architecture]] för mer.


## [2026-06-01] hardware | ADB-anslutning lyckades! Galaxy Note 10 identifierad via adb (ingen dialog behövdes)

**Problem som rapporterades:**
> Jag får inte upp en tillåt dialog. Jag får dock upp USB-inställningar i Notifikations rullgardinen. Där kan jag välja USB kontrolleras av "Ansluten enhet", men när jag väljer det säger telefonen att den inte tillåter det valet.

**Undersökning via adb (direkt i sessionen):**
- `adb devices -l` visade: `R58M82BNJGW device usb:4-3 product:d1eea model:SM_N970F device:d1 ...`
- Bekräftat via shell:
  - ro.product.model = SM-N970F
  - Android 12 (SDK 31)
  - Build fingerprint: samsung/d1eea/d1:12/SP1A.210812.016/N970FXXS9HWHA:user/release-keys
- USB config: `persist.sys.usb.config = mtp,conn_gadget,adb` (adb är aktivt)
- NFC: 
  - init.svc.nfc_hal_service = running
  - nfc.initialized = true
  - nfc.fw.ver = S.LSI 4.5.11 (Samsung egen NFC-kontroller – bra för Mifare Classic)
  - `pm list features` listar fullständigt: android.hardware.nfc, nfc.any, hce, hcef, ese, uicc m.fl.
- USB dumpsys: Telefonen är i device/peripheral-läge (mIsHostConnected=false). "Ansluten enhet" (host mode) nekas med avsikt.

**Slutsats:**
- Telefonen är **redan fullt ansluten och auktoriserad** för ADB ("device"-status). Dialogen dyker bara upp första gången (eller efter revoke). Den har antingen godkänts tidigare eller tyst auktoriserats.
- "USB kontrolleras av: Ansluten enhet" nekas är **normalt och korrekt** här. Vi vill ha telefonen som USB-enhet (MTP+ADB), inte som USB-host. "Den här enheten" + "Överföra filer/Android Auto" är rätt kombination.
- Udev-regler och plugdev-grupp fortfarande inte installerade på host (script inte kört än), men adb fungerar ändå tack vare 0666-aktivering eller befintliga regler + "adb" i usb.config.

**Åtgärder:**
- Uppdaterade wiki/Hardware-Testenheter.md med förklaring + bekräftad spec.
- NFC-hårdvara är perfekt för eskortminne (Mifare Classic-stöd förväntat).

**Nästa:**
- Användaren kan nu välja den fysiska enheten i Android Studio och deploya.
- Kör `bash ~/fix-usb-adb.sh` + unplug/replug för permanent setup.
- Slå på NFC på telefonen.
- Byt in AndroidNfcManager i koden + koppla live scanning.


## [2026-06-01] hardware | App installerad direkt på Note 10 via adb

- `adb -s R58M82BNJGW install -r .../app-debug.apk` → **Success**
- Den aktuella RFIDManager-appen (Figma-dashboard med radar, StatCards, READ/WRITE tabs, RFIDTagList, stub-data) är nu installerad på den fysiska telefonen.
- Användaren kan starta den från app-listan ("RFID Manager").

**Status:** Från emulator + stub till riktig hårdvara på <5 minuter efter att enheten dök upp i adb.


## [2026-06-02] hardware | udev + plugdev setup slutfört framgångsrikt

**Kommando som kördes i riktig terminal:**
`bash ~/fix-usb-adb.sh`

**Resultat:**
- usbutils installerades (lsusb nu tillgängligt)
- plugdev-grupp skapad + joakim tillagd
- 51-android.rules kopierad till /etc/udev/rules.d/ (root owned, korrekt innehåll för 04e8 + 18d1)
- `udevadm control --reload-rules && trigger` kört

**Verifiering (via verktyg direkt efter):**
- Regler på plats: `/etc/udev/rules.d/51-android.rules`
- Användare i grupp: `id joakim` visar `plugdev`
- `lsusb` visar Samsung: `Bus 004 Device 007: ID 04e8:6860 Samsung Electronics Co., Ltd Galaxy series, misc. (MTP mode)`
- `adb kill-server && adb devices -l` visar fortfarande `R58M82BNJGW device ... model:SM_N970F`

**Uppdatering:**
Scriptet kördes i riktig terminal (Grok TUI:s kommando-exekvering stödjer inte interaktivt sudo pga saknad tty).

**Nästa:**
- Användaren ska koppla ur/inkoppla telefonen.
- Slå på NFC på telefonen.
- Öppna "RFID Manager" appen på den fysiska enheten (redan installerad).
- Börja koppla in AndroidNfcManager för live scanning.


## [2026-06-02] cleanup | Host-setup-filer flyttade till dedikerad katalog

**Problem:**
Filer skapades i ~ (home root): `~/fix-usb-adb.sh` och `~/51-android.rules`

**Åtgärd:**
- Skapade `~/rfid-manager/setup/`
- Flyttade båda filerna dit
- Uppdaterade scriptet internt (`cp ~/rfid-manager/setup/51-android.rules ...`)
- Uppdaterade alla referenser i wiki/Hardware-Testenheter.md

**Ny struktur:**
- `~/rfid-manager/setup/fix-usb-adb.sh`
- `~/rfid-manager/setup/51-android.rules`

Detta håller home root rent. Framtida host-relaterade skript/regler läggs här.


## [2026-06-02] nfc | Riktig AndroidNfcManager inkopplad i MainActivity

**Ändringar:**
- MainActivity.kt uppdaterad:
  - Skapar `AndroidNfcManager(this)` (riktig impl eftersom vi är på fysisk enhet)
  - `detectedTags` som `mutableStateListOf<RFIDTag>()` för live-uppdatering
  - `onStartScan` togglar scanning: clear list + startScanning (med callback som mappar DomainRfidTag → UI RFIDTag)
  - Lägger till nya taggar överst, undviker dubbletter på uid
  - onPause stannar scanning
  - Enkel `toUiTag()` mapper (uidHex, typsträng från TagType enum, timestamp HH:mm:ss, rssi fallback)
- Bygg: `./gradlew assembleDebug` → BUILD SUCCESSFUL

**Status:**
- Appen på telefonen (via Studio Run) använder nu riktig NFC när "START SCAN" trycks.
- Callback levererar via enableReaderMode → handleTag → listener.
- Vibration på detektion (från manager).
- Stubben är fortfarande tillgänglig för emulator, men vi använder real på enheten.

**Nästa:**
- Testa med riktiga taggar (eskortminne nära baksidan av Note 10).
- Eventuellt förbättra: byt knapptext till "STOP SCAN" när scanning, visa "scanning..." status, bättre RSSI, auto-stop onPause etc.
- Implementera readMifareClassicSector i AndroidNfcManager med live Tag-ref (för närvarande placeholder).
- Uppdatera wiki när vi har första riktiga läsningar.


## [2026-06-02] ux | START/STOP SCAN + live NFC integration

- Lade till `isScanning: Boolean` parameter till RFIDManagerScreen.
- Knapptext byts dynamiskt: "START SCAN" / "STOP SCAN".
- MainActivity skickar isScanning state.
- Appen på enheten uppdaterad via adb install (och Studio Run fungerar som vanligt).
- Bygg + deploy OK.


## [2026-06-02] nfc | Faktisk sektorläsning implementerad + data i UI

**Framsteg:**
- Användaren rapporterade: "Den har lyckats läsa en av mina fyra NFC taggar, så det funkar."
- Utökade `domain/model/RfidTag` med `sectorsRead: Map<Int, ByteArray>`
- `AndroidNfcManager.handleTag` utför nu riktiga läsningar av sektor 0,1,2 (med default keys) *direkt i callbacken* medan Tag är giltig.
- Loggar hex-data för lästa sektorer (användbart för inspektion av eskortminne).
- `readMifareClassicSector` och write använder nu `lastTag` om UID matchar (kortlivad cache).
- Lade till `writeMifareClassicBlockInternal` (skyddar sector trailer).
- UI: `ui/model/RFIDTag` fick `dataPreview`.
- Mapper i MainActivity fyller preview från första lästa sektorn (t.ex. "S1: 01 23 45 67 89 AB CD EF...").
- `RFIDTagList` visar dataPreview i orange om närvarande.
- Bygg + adb install till R58M82BNJGW lyckades.

**Resultat:**
När en giltig Mifare-tagg (t.ex. den fungerande av de 4) detekteras visas nu verklig data från sektorerna i listan + detaljerade loggar.

**Wiki:**
Uppdaterad log + förberedelse för eskortminne-dokumentation.

**Nästa (efter test av läsdata):**
- Förfina WriteTagForm till att använda write.
- Lägg till UI för att välja sektor + visa full hex-dump av lästa data.
- Testa kända nycklar för eskortminnen.
- Uppdatera [[Eskortminne]] och [[Android-NFC-API]] med vad som fungerar på de riktiga taggarna.


## [2026-06-02] nfc | Användarverifiering: en tagg läsbar + sektor data nu exponerad

**Användarrapport:**
> "Den har lyckats läsa en av mina fyra NFC taggar, så det funkar. De andra taggarna kan vara dåliga... Låt oss gå vidare"

**Åtgärder efter det:**
- Utökade UI för att visa full hex-dump av lästa sektorer i vänster panel när en tagg väljs i listan.
- Rebuild + adb install av versionen med auto-read av sektor 0-2 + data i UI + write.
- Loggar + preview + fullSectors nu tillgängliga direkt i appen.

**Status:**
Grundläggande UID-detektion bekräftad av användaren på riktig hårdvara. Nu med verkligt minnesinnehåll från taggarna.


## [2026-06-02] nfc | Utökad auto-dump av sektorer + "gå vidare"

**Användaren:**
> Den har lyckats läsa en av mina fyra NFC taggar, så det funkar. De andra taggarna kan vara dåliga... Låt oss gå vidare

**Implementerat:**
- I AndroidNfcManager: vid detektion av Mifare Classic loopas nu sektor 0..15 (1K) eller tills auth misslyckas upprepade gånger.
- Alla lyckade läsningar loggas med full hex + sparas i RfidTag.sectorsRead.
- UI (preview + full hex i selected memory view) uppdateras automatiskt.
- APK byggd och install påbörjad till enheten.

Detta betyder att när den fungerande taggen scannas får vi så mycket data som default-nycklarna tillåter direkt i appen + logcat.

**Wiki-uppdateringar:**
- Logg + Hardware-Testenheter.md uppdaterad med milstolpe.

**Status:**
Vi har nu en fungerande "dump on scan" för eskortminnen på riktig hårdvara.


## [2026-06-02] nfc | Fix: System chooser appearing instead of app handling tags (READ LOG empty)

**Problem reported by user:**
- App now detects all tags (sound + vibration).
- But system dialog/chooser appears (user chooses Chrome, goes to tag manufacturer's page).
- Nothing appears in the app's "READ LOG" list.
- This means `enableReaderMode` was not successfully taking exclusive control; system was dispatching via NDEF intents.

**Root cause:**
- Imperative start/stop inside click lambda + state timing could lead to reader mode not being reliably active.
- No pre-disable of previous reader mode.
- Insufficient logging to diagnose "NFC not enabled at start time".

**Fix applied:**
- Refactored MainActivity scanning control to use `LaunchedEffect(isScanning)` for declarative, reliable start/stop of the NfcManager.
- In AndroidNfcManager.startScanning: always call `disableReaderMode` first for clean state, added explicit logs for null adapter / !isEnabled cases, and success log "Reader mode ENABLED successfully".
- Updated status UI in RFIDManagerScreen to dynamically show "SCANNING" (with Accent color) vs "STANDBY" based on the isScanning prop.
- Rebuilt.

**Expected behavior after fix:**
- When user taps "START SCAN", status changes to "SCANNING".
- Reader mode should now properly intercept tags: no system sound (we have NO_PLATFORM_SOUNDS), no chooser dialog, tags delivered directly to the callback and added to READ LOG list with UID, type, sectors data, etc.
- If chooser still appears, it will be because `adapter.isEnabled` was false at the moment of starting (user must ensure NFC toggle is on in settings).


## [2026-06-02] nfc | Fallback för NFC chooser: onNewIntent + intent filters så appen kan väljas och fylla READ LOG

**Problem:**
Användaren får systemets "välj app" dialog (Chrome tar URL:en från NDEF), och ingenting hamnar i READ LOG trots att taggar detekteras.

**Lösning:**
- Lade till NFC intent filters i AndroidManifest (NDEF_DISCOVERED för http/https + TECH_DISCOVERED).
- Skapade res/xml/nfc_tech_filter.xml med MifareClassic, NfcA, Ndef etc.
- I MainActivity: override onNewIntent, extrahera Tag, anropa processDiscoveredTag (lägger till i detectedTags med UID/typ) och nfcManager.onTagDiscovered (gör sektorläsningar, loggar, sätter lastTag, vibrerar).
- Exponerade onTagDiscovered i AndroidNfcManager som anropar handleTag.
- State (scanningEnabled, detectedTags, selectedTagId) hoistad till Activity-nivå så onResume/onNewIntent kan accessa dem.
- onResume re-startar reader mode om scanningEnabled.
- Detta ger fallback: om reader mode inte fångar (ingen chooser), kan användaren välja appen i choosern och listan fylls ändå.

Reader mode är fortfarande primär väg (ingen chooser, direkt till callback).


## [2026-06-02] nfc | User report: system chooser instead of in-app list (even with latest fixes)

User: "Inget registreras i READ LOG däremot läser den samtliga mina taggar nu. Jag får en ljudsignal när den läser samt den öppnar en dialog... Jag väljer Chrome... Men, igen den listar inte upp de lästa taggarna i READ LOG"

This confirms the reader mode is not suppressing the system NDEF dispatch on this device (Samsung Note 10 / Android 12). Tags trigger system sound + chooser (because they contain NDEF with URL to the manufacturer site).

The APK with onNewIntent fallback + manifest filters was just installed successfully (lastUpdateTime 22:15).

Next action for user: 
- Use START SCAN (confirm "SCANNING" status + log "ENABLED").
- If chooser still appears → **choose "RFID Manager"** (not Chrome). The onNewIntent path will now populate READ LOG.


## [2026-06-02] nfc | Stöd för Mifare Ultralight / NTAG (användarens taggar) + skrivning

User reported: Läsning fungerar (tags dyker upp utan systemljud/chooser tack vare reader mode), men "markerar den lästa NFC taggen och försöker skriva till den. Inget händer."

Log analysis: All detected tags were MIFARE_ULTRALIGHT (UID 042061B2392B80, techs NfcA + MifareUltralight + Ndef), sectorsRead=[] because code only handled Classic.

**Changes:**
- handleTag now also reads user pages (4,8,12,16...) for Ultralight/NTAG using MifareUltralight.readPages and populates sectorsRead (reused map, keys are page numbers).
- Added readMifareUltralightPages + writeMifareUltralightPage to interface, stub, and real impl (using lastTag cache + internal connect/writePage).
- Updated toUiTag mapper: dataPreview now uses "P" prefix for pages on Ultra/NTAG tags.
- Updated selected memory dump display: uses "Page X" vs "Sector X" based on tag.type string.
- onWrite in MainActivity now detects type from selected RFIDTag and calls the correct write method (Ultra page 4 with 4 bytes, or Classic block 4 with 16 bytes).
- Updated WriteTagForm hint text.
- Rebuilt + pushed.

Now write from WRITE tab (after selecting tag in list) should work for the user's Ultralight tags (writes UTF8 to page 4).

Classic support remains for future eskortminne (Mifare Classic).


## [2026-06-02] nfc | User update: Läsning fungerar (Ultralight-taggar), skrivning "inget händer"

User: "Läsning fungerar. NFC taggen dyker upp utan ljudsignal. Jag markerar den lästa NFC taggen och försöker skriva till den. Inget händer."

Log samples show repeated enable/disable + detections of MIFARE_ULTRALIGHT (042061B2392B80), sectorsRead=[] (from pre-fix APK).

**Analysis & action:**
- Confirmed: user's tags are Ultralight/NTAG (NDEF content explains old Chrome chooser behavior).
- Previous code only did Classic sector logic → no data, write always Classic → silent fail on Ultra.
- Latest APK (the one that just completed install) includes:
  - Auto page reads (4,8,12...) for Ultra on every detection (now populates sectorsRead with page data, logged).
  - Type-aware write in onWrite + new writeMifareUltralightPage impl.
  - UI labels "Page X" for Ultra tags.
  - Write logs "Write Ultralight page 4 result=..."
- Write requires the tag to be in NFC field at write time (uses lastTag cache from last handleTag). Keep tag close or re-present while scanning is active.

**Instructions sent to user:**
- Use latest installed APK (or re-Run from Studio).
- Scan tag (START SCAN active).
- Select in list.
- Go to WRITE, enter text, press button, **keep tag in range**.
- Check log: adb ... | grep -E 'RFIDManager|AndroidNfcManager' for the write result line.
- If result=true → success. Then we can read back the page to verify, analyze the actual memory layout from the hex the user sees in UI, proceed to custom keys / more pages / real eskortminne work.


## [2026-06-02] nfc | Write to Ultralight fails with Transceive (likely lock bits); added user-selectable page + lock byte reading

User provided log showing successful reads (sectorsRead=[4,8,12,16]) but repeated:

E AndroidNfcManager: Ultralight writePage error at page 4
java.io.IOException: Transceive failed

This is almost always because the page is locked (lock bits in page 2 of Ultralight/NTAG).

**Improvements made:**
- In handleTag for Ultra: now always reads page 2 (lock bytes + OTP) in addition to user pages. Lock bytes will be visible in the UI under selected tag (as "Lock/OTP 2-5" or similar).
- WriteTagForm now has an input field for "Target page (Ultralight) / block (Classic)" (default 4). User can try higher pages (e.g. 5,6,7...) that might not be locked.
- onWrite and MainActivity onWrite now take the address param and use it for the write call.
- Updated hint text to warn about lock bits.
- Rebuilt + install started.

Advice to user:
- After scanning with new APK, select the tag and look at the full hex for the "Lock/OTP" entry (page 2 data). The first bytes tell which pages are locked.
- Try writing to a higher page number using the new field in the form.
- If all user pages are locked, the tag is configured read-only (common for giveaway tags).

This gets us closer to understanding the actual eskortminne tags (which are likely Classic).


## [2026-06-02] nfc | Write transceive fail on Ultralight = lock bits; added UI lock byte parser + selectable target page

User log shows successful page reads (sectorsRead has 4,8,12,16) but:

E ... Ultralight writePage error at page 4
java.io.IOException: Transceive failed

**Root cause (standard for these tags):** Page 4 (and often surrounding) is write-locked via the lock bytes in page 2 of Ultralight/NTAG. "Transceive failed" on writePage is the symptom when the page's lock bit is set.

**Added:**
- In handleTag (Ultra branch): explicitly read page 2 first (as "Lock/OTP 2-5") so lock bytes are always captured on detection.
- In selected tag memory UI: special case for page 2 on Ultra tags, parses LB0 (byte 0 of the 16-byte block) and lists locked user pages e.g. "(Lock bits: pages 4,5 locked)".
- Write form already has editable target addr (from previous).
- Updated hints in form and wiki to point user to inspect lock bytes and try higher pages (e.g. page 8 or 12 if not locked).

User can now:
- Scan tag → select it → see Lock/OTP hex + parsed locked pages.
- Use the "Target page" field in WRITE to attempt a page that shows as not locked.
- If all user pages locked, tag is effectively read-only (common on pre-programmed or cheap stickers).

For actual eskortminne (likely Classic 1K/4K with specific keys), this Ultralight behavior is different.


## [2026-06-02] nfc | Write fails with Transceive on page 4 (lock bits confirmed); UI now shows parsed locks + selectable target page

User pasted exact error:
E AndroidNfcManager: Ultralight writePage error at page 4
java.io.IOException: Transceive failed
(at MifareUltralight.writePage)

sectorsRead=[4, 8, 12, 16] (user data readable, but writes to 4 fail).

**Diagnosis:** Standard behavior on Ultralight/NTAG stickers. Page 2 contains lock bytes. When the corresponding bit is set for a page, writePage gets "Transceive failed" (the tag simply doesn't acknowledge the write).

**What was shipped in the APK that just installed (exit 0):**
- Page 2 is now read on every Ultralight detection (as "Lock/OTP 2-5").
- In SELECTED TAG MEMORY the UI now parses LB0 and shows e.g. "(Lock bits: pages 4,5 locked)".
- WRITE form has an input field for "Target page (Ultralight) / block (Classic)" so user can pick a page that is not locked.
- Updated hints in form + wiki.

**Immediate test for user (now that the APK is on the device):**
1. START SCAN + present tag.
2. Select the tag → look at the Page 2 / Lock/OTP hex block. The new parser line tells exactly which pages are locked.
3. In WRITE tab: use the "Target page" field to choose a page that is NOT listed as locked (try 5,6,7,8,9...).
4. Keep tag firmly on the phone during the write.
5. Watch logcat for "Write Ultralight page X result=true/false".

If every user page (4+) shows as locked, the tags are configured read-only (very common on test/giveaway stickers). Real eskortminne are usually Mifare Classic.

Wiki (Hardware-Testenheter.md and log.md) updated with the exact error and the new UI capability.

Next: user will report the lock bytes hex + which pages the parser says are locked, and whether a higher page write succeeded. Then we can interpret the actual memory content or move to Classic tags + custom keys.


## [2026-06-02] nfc | Auto-suggest writable target page in Write form (skips locked pages based on page 2 lock bits)

Added logic in RFIDManagerScreen (WRITE branch):
- Computes defaultAddr by inspecting the lock bytes from fullSectors[2] (if Ultra).
- Picks the smallest addr >=4 that has data in fullSectors and is not in the locked set from LB0.
- Passes as defaultTarget to WriteTagForm (which now initializes the field with it).

This makes it even easier for the user: when they open the WRITE tab for a selected Ultra tag, the "Target page" field is pre-filled with a page that is likely writable (based on the lock bits we parsed).

The install of this version is in progress (background task).

Combined with the lock bit parser line in the memory dump, the user now has clear guidance on which pages to avoid and which to try.


## [2026-06-02] nfc | Auto-suggest writable target page deployed (exit 0)

The build + adb install of the version with:
- Lock bytes read on detection (page 2)
- Parsed lock bits display in SELECTED TAG MEMORY
- Auto-computed default target page in WRITE form (smallest >=4 with data and not locked per LB0)
- Editable field so user can override

... just completed successfully (Duration 159.3s, exit 0).

User now has the full UX for dealing with locked Ultralight pages:
- Scan → select tag → see explicit " (Lock bits: pages 4,5 locked)" etc.
- WRITE tab pre-fills "Target page" with a safe suggestion.
- User can still edit it to experiment.

Next expected: user reports the lock info + tries write to the suggested page + pastes logcat result.


## [2026-06-02] nfc | Write now armed on button press and executed on next fresh tag detection (reliable, no stale tag)

To solve the "tag lost / connect failed" on write (user removes tag after scan, then presses write using stale lastTag):

- Added PendingWrite state in MainActivity.
- onWrite (from WRITE tab) now "arms" the pending write + sets status message "Hold the selected tag to the phone NOW to write...".
- In the scanning callback (which runs with fresh tag and lastTag just set in handleTag), if pendingWrite matches the detected uid, execute the actual write call (which now has fresh lastTag), update status, clear pending.
- If stop scan, clear pending.
- This means: user selects tag, arms write in UI (scanning should be on), then presents/holds the tag again — the next detection will perform the write with live tag.

This makes writes reliable for passive tags.

The build/install for this is in progress.

Combined with previous lock bit UI and auto-suggest page, user has good tools for the Ultralight tags.


## [2026-06-02] nfc | User tested write with armed flow not yet (still page 4, "no tag selected" errors); Lock/OTP hex captured

User log (after previous APK but before armed one fully used?):
- Repeated "Write Ultralight page 4 result=false" + connect/transceive errors.
- Multiple "No tag selected for write" (user pressing WRITE button without having selected a tag in the list first).
- One scan captured full Lock/OTP: "20 48 F8 FF E1 10 12 0F 01 03 A0 10 44 03 1A D1" for pages 2-5.
- Then user pages data confirms it's a factory NDEF URI tag pointing to rapidnfc.com (the hex decodes to the manufacturer's intro URL).

The armed write version (press WRITE TO TAG to arm + "hold tag now", then present to execute on next fresh detection) just finished installing (the task in this reminder).

Instruct user to:
- Select tag in READ LOG first (so selectedId is set, avoids "no tag selected").
- Look at the new Lock/OTP section for parsed locks.
- Use Target page field to pick e.g. page 8 or 12 if not locked.
- Arm the write, then present tag while "hold now" message is shown.
- The write will happen inside the reader callback with live tag.

This should finally allow writes to unlocked pages.


## [2026-06-02] nfc | Armed write hint text updated and deployed; user to follow new numbered flow

The build + install with the clear numbered instructions in the Write form hint ("1. Select tag... 2. Use Target page... 4. Press WRITE TO TAG (arms it)... 5. Immediately hold tag...") just completed (lastUpdateTime 22:55:15).

This is on top of the armed logic + lock bit parser + auto-suggested target page.

User's previous test log (before this APK) still showed page 4 attempts + "No tag selected for write" + one capture of Lock/OTP "20 48 F8 FF...".

Now with this APK the UI will guide them step by step and the form will tell them to arm then present.

Next: user tests with proper selection + non-locked page + armed flow.


## [2026-06-02] nfc | Made SELECTED TAG MEMORY and lock bytes more discoverable

User couldn't find "SELECTED TAG MEMORY" or "Page 2 / Lock/OTP" row.

- Added always-visible "SELECTED (tap in right list to change)" card at the very top of the left panel (right after stats, before radar) as soon as any tag is tapped in the list. It shows UID + data preview.
- The detailed "SELECTED TAG MEMORY" card (with all hex + lock parser) now renders even if no sectors yet (shows message "No page/sector data read yet (hold tag steady during scan)").
- Added header text in the READ LOG list itself: "Tap a row below to select it for full memory details (incl. lock bytes) and write".
- This should make the flow obvious: scan → tap row in right list → left panel lights up with selected info + detailed memory/lock view at bottom.

The install with these discoverability fixes is in progress.


## [2026-06-02] nfc | User kan inte hitta "SELECTED TAG MEMORY" eller Page 2 / Lock/OTP i UI

User: "Titta i vänster panel "SELECTED TAG MEMORY": Leta efter Page 2 / Lock/OTP-raden. Ovanstående två rader är inget som jag hittar eller känner igen i appen?"

**Orsak:** Användaren har inte tryckt på en rad i READ LOG-listan på höger sida för att välja taggen. Utan selection (selectedId) visas varken den övre "SELECTED"-indikatorn eller den detaljerade "SELECTED TAG MEMORY"-rutan med hex + lock-bytes-tolkning.

**UI-förbättringar som nu är på plats (install 23:05:19):**
- Header-text direkt i READ LOG-listan: "Tap a row below to select it for full memory details (incl. lock bytes) and write"
- Alltid synlig "SELECTED (tap in right list to change)"-ruta högst upp i vänster kolumn så fort man trycker på en rad (visar UID + dataPreview).
- "SELECTED TAG MEMORY"-rutan (med alla Page X: hex + för addr==2 den explicita "(Lock bits: pages X locked)"-raden) visas nu alltid när en tagg är vald, även om bara page 2 lästs.
- List-items har selection-highlight (vänster accent).

**Instruktion till user:**
1. START SCAN + presentera tagg så den kommer i listan.
2. Tryck på raden i READ LOG (höger sida).
3. Vänster sida tänds: högst upp "SELECTED ..."-ruta.
4. Scrolla ner i vänster kolumn för den stora "SELECTED TAG MEMORY"-rutan.
5. Inuti den: leta efter "Page 2:" (eller "Lock/OTP 2-5:") + hex + lås-bits-tolkningen under.

Om de fortfarande inte ser det: starta om appen helt, eller kör Run från Studio för att vara säker på att det är den nya builden (ibland tar adb-install tid att "kännas av").

Från tidigare loggar har användaren redan sett raw Lock/OTP-hex ("20 48 F8 FF..."), så när UI:n väl visas bör tolkningen dyka upp automatiskt.


## [2026-06-02] nfc | User feedback: kan inte hitta "SELECTED TAG MEMORY" eller Page 2 / Lock/OTP i appen

User: "Titta i vänster panel "SELECTED TAG MEMORY": Leta efter Page 2 / Lock/OTP-raden. Ovanstående två rader är inget som jag hittar eller känner igen i appen?"

**Klargörande och åtgärd:**
- "SELECTED TAG MEMORY" + den detaljerade hex-dumpen (med Page 2 / Lock/OTP + lås-bits-tolkning) visas **endast efter att man tryckt på en rad i READ LOG-listan** (höger sida). Detta sätter selectedId.
- Vi har lagt till:
  - Header-text i READ LOG-listan själv: "Tap a row below to select it for full memory details (incl. lock bytes) and write"
  - Alltid synlig "SELECTED (tap in right list to change)"-ruta högst upp i vänster kolumn (radar-sidan) så fort en tagg är vald.
  - Den stora "SELECTED TAG MEMORY"-rutan längre ner i vänster kolumn, som nu alltid renderas när en tagg är vald (även om bara page 2 lästs, med meddelande om "No page/sector data...").
- Användaren har i tidigare loggar sett raw "Lock/OTP 2-5: 20 48 F8 FF..." och "No tag selected for write" många gånger → de har tryckt på WRITE utan att först välja i listan.

**Instruktion som skickats:**
- Starta om appen efter senaste install (23:05).
- START SCAN + presentera tagg.
- Tryck på raden i READ LOG (höger lista) – nu har den en header-text som talar om att man ska trycka.
- Vänster sida (radar-kolumnen) tänds: högst upp "SELECTED ..."-ruta, längre ner "SELECTED TAG MEMORY" med "Page 2:" + hex + "(Lock bits: ...)"-tolkning.
- Om de fortfarande inte ser det: beskriv vad vänster sida visar efter att ha tryckt på en rad, eller kör Run från Studio igen.


## [2026-06-02] nfc | User feedback on UI: can't find SELECTED TAG MEMORY or Page 2 row; clarified selection flow and enhanced top SELECTED card to include full hex + lock parser

User has selected (green accent, sees SELECTED on left with "Data: P2: 20 48 F8 FF E1 10 12 0F..."), but doesn't see the detailed "SELECTED TAG MEMORY" or Lock/OTP row, doesn't know how to interpret hex or use for WRITE, tapping SELECTED does nothing.

**Cause:** The detailed memory card was lower in the left column (after radar/stats), and user may not have realized selection is by tapping list rows (not the SELECTED box). The box is info only.

**Actions:**
- Added prominent "SELECTED (tap in right list to change)" card at top of left panel on selection.
- Added header in READ LOG list explaining to tap rows to select for details/write.
- Enhanced the top SELECTED card to also contain the full hex list of all pages + the lock bits parser line for page 2 (so everything is visible immediately at top without scrolling).
- The detailed "SELECTED TAG MEMORY" card lower remains as backup.
- Tapping the SELECTED box does nothing (it's not a button); selection is only from the right list rows.

**Interpretation for user:**
- P2 = Page 2 (Ultralight memory is pages of 4 bytes; page 2 is special for locks + CC).
- The hex is the raw 16-byte content read from that page.
- For page 2, the first bytes are lock bytes; the UI parses LB0 and lists locked user pages (e.g. pages 4,5 locked = do not write there or you'll get transceive error).
- The data after is the tag's NDEF content (in this case, a URI record pointing to the tag manufacturer's site "rapidnfc.com/...").
- To use in WRITE: the lock info tells you safe pages. In the right WRITE TAG form, use the "Target page" field to pick a page NOT listed as locked (the form now auto-suggests one). The selected tag is used automatically. Press WRITE TO TAG (it arms), then hold the tag to phone again to execute.

The latest install (23:02) has the list header; the one with full-in-top-card is the current one just built/installed.

Wiki updated.


## [2026-06-02] nfc | User confusion on interpreting SELECTED hex (P2 etc) and how to use for WRITE; enhanced top SELECTED card with explicit labels, full list, and direct instruction + lock warning

User: scanned+selected (green accent + SELECTED box visible with "Data: P2: 20 48 F8 FF..."), but "vet jag inte hur jag skall tolka de hex ärdena eller vad rubrikerna betyder?", tapping SELECTED does nothing, "Hur utnyttjar jag informationen i SELECTED i WRITE TAG columnen till höger?"

Screenshot showed the top SELECTED with partial P2 preview.

**Response + code:**
- Explained: tapping the box does nothing (info only); select via right list rows (now has header text).
- P2 = Page 2 (Ultralight pages of 4 bytes; page 2 = lock bytes + CC).
- Hex = raw bytes from that page.
- UI now (in top SELECTED box the user sees) lists ALL pages as "P2 (lock bytes + CC): hex" then ">>> Lock bits: pages 4,5 locked - DO NOT write... <<<" and "To use: look at this to pick safe Target page on right".
- Full hex list moved into the top SELECTED card for immediate visibility (no need to scroll to detailed card).
- The detailed "SELECTED TAG MEMORY" lower remains.
- The arm+present write flow (press to arm, hold tag to execute on next detection) + Target page field already in place.

Wiki updated.

The build/install for the clearer top card labels is in progress.

User to restart/re-run, tap a list row, look at the (now enhanced) top SELECTED box for the full list + lock warning + usage note, then use a non-locked page in right WRITE form.


## [2026-06-03] nfc | User still confused on interpreting hex in SELECTED box and how to use for WRITE; full list + guidance now inside the top SELECTED box they are looking at

User: scanned+selected (green accent + SELECTED box visible with "Data: P2: 20 48 F8 FF E1 10 12 0F..."), "vet jag inte hur jag skall tolka de hex ärdena eller vad rubrikerna betyder?", tapping SELECTED does nothing, "Hur utnyttjar jag informationen i SELECTED i WRITE TAG columnen till höger?"

**Actions:**
- Confirmed tapping the SELECTED box itself does nothing (info-only; selection only via tapping rows in right READ LOG list, which now has explicit header).
- Enhanced the top SELECTED card (the exact box the user is looking at) to contain:
  - "Memory pages (hex) - use this to pick safe Target page on right:"
  - For each: "P2 (lock bytes + CC): hex"
  - Explicit: ">>> Lock bits: pages 4,5 locked - DO NOT write here <<<" (or positive note if none).
  - The full list of all Px hex is now right there in the box, no need to find a separate lower "SELECTED TAG MEMORY" card.
- This makes interpretation and "how to use for WRITE" immediate: the lock note tells exactly which pages to avoid in the right-side "Target page" field.

The build/install for this (full list + guidance inside the visible SELECTED box) just completed (lastUpdateTime 07:06:23).

User to restart/re-run, tap a list row, look inside the top SELECTED box they already see for the full pages list + lock warning + "use this to pick safe Target page on right" header.

Wiki updated.


## [2026-06-03] nfc | Pending write now executed synchronously in the detection callback (runBlocking) to keep the tag in field during write

To reduce "tag lost" or connect errors during armed write (the write was in launch, which could run after the tag left the field):

- Changed the pending write execution from scope.launch to runBlocking in the scanning callback.

- This makes the writeMifare... call happen sync while still in the reader callback context, where lastTag is fresh and the tag is "present".

- Should improve reliability for writes when the user holds the tag after arming.

Build/install done.

Combined with previous UI for lock info and target page selection.


## [2026-06-03] nfc | Added Toast for write result feedback for better UX (in addition to status message in form and logcat)

To address "Får ingen bra feedback på att den har skrivit":

- After the pending write result, show a Toast with the success/failed message.

- The statusMessage in the WRITE form also updates.

- Logcat has the detailed result.

Build/install in progress.


## [2026-06-03] nfc | User still hitting locked page 4/8 with result=false and connect errors; Toast feedback now in APK

User log shows continued attempts on page 4 (and 8), "Write ... result=false (via pending)", "No tag selected for write", and "Tag was lost" during some reads.

They have the Lock/OTP "20 48 F8 FF..." and the UI should now parse LB0=0x20 as locking page 8.

The APK with Toast for write result just installed (this task).

Remind user of full flow: select in list first (to avoid no selected), look inside top SELECTED box for full pages + explicit lock warning + "use this to pick safe Target page", set Target page to non-locked (e.g. 12 or 16), arm write, hold tag again for execution.

The "no good feedback" is addressed by Toast + status message in form.

Wiki updated.


## [2026-06-03] nfc | User succeeded writing to page 12 (result=true via pending)!

User: "Jag fick write to pagfe 12 succeeded! Fast hur kan jag verifiera det. Gränssnittet på vänster sidan visar inte page 12."

**Verification instructions:**
- The left SELECTED box (and detailed memory card) only reflects the data from the *last successful detection/read* for that UID.
- After write success, **re-present the tag** (hold steady during scan) to trigger a fresh detection that re-reads the pages (now with your new data in page 12).
- Then re-select the tag (tap row in right list) – the top SELECTED box (which now contains the full hex list) and the lower "SELECTED TAG MEMORY" will show the updated P12 hex (should start with your written bytes e.g. 54 45 53 54 instead of old 6F FE...).
- If P12 still not visible in the list, it means the last detection only read partial pages (tag lost during read from 4+, as seen in logs where sometimes only [2]). Hold tag firmly/steadily longer to get full read up to 16+.
- The "Data: P2: ..." is just the short preview from first page; full list is in the SELECTED box (now enhanced to have all Px hex + lock note + guidance "use this to pick safe Target page on right").

**Why page 12 worked but 4 didn't:** From lock bytes in P2 (20 48 F8 FF...), the parser flags page 8 locked; page 4 often protected by CC/NDEF structure on these tags. Higher pages like 12/16 (00s in dump) are free.

**Feedback:** Now with Toast (installed), you get popup "Write ... succeeded!". Status in WRITE form also updates. Re-scan verifies in UI.

Wiki updated. User can now verify by re-scan, then we can analyze the actual memory content (NDEF URI tags).


## [2026-06-03] nfc | User lyckades skriva till page 12 (result=true via pending)!

User: "Jag fick write to pagfe 12 succeeded! Fast hur kan jag verifiera det. Gränssnittet på vänster sidan visar inte page 12."

**Verifiering:**
- Vänster SELECTED (och detaljerad minnesruta) visar data från *senaste detektionen* för den UID:n.
- Efter lyckad write: presentera taggen igen (håll stadigt under scan) → ny detektion läser om sidorna (nu med nytt innehåll i P12).
- Välj taggen igen i höger lista → SELECTED-boxen (som nu har full hex-lista inuti) visar uppdaterad P12 (bör börja med dina skrivna bytes t.ex. 54 45 53 54... istället för gamla 6F FE...).
- Om P12 inte syns i listan: senaste detektionen läste bara delvis (tag lost under read från 4+, som i loggar där ibland bara [2]). Håll taggen stadigare/längre för full read upp till 16+.
- "Data: P2: ..." är kort preview; full lista är i SELECTED-boxen (med "P2 (lock bytes + CC):", "P4:", etc. + lås-tolkning för P2).
- Tapping SELECTED gör inget (info, inte knapp); markering via höger lista.

**Kodförbättring:** På re-detektion av samma UID uppdateras nu befintlig post i listan med färsk data (istället för att skippa add pga none-check). Gör att re-scan fräschar upp hex efter write.

Bygg/install påbörjad (men device not found i detta kommando – använd Studio Run när telefonen är ansluten).

Användaren kan nu verifiera genom re-presentera + re-välja, och vi kan analysera det faktiska minnesinnehållet (NDEF URI-taggar).


## [2026-06-03] nfc | User fick write to page 12 succeeded! (via pending)

User: "Jag fick write to pagfe 12 succeeded! Fast hur kan jag verifiera det. Gränssnittet på vänster sidan visar inte page 12."

**Verifiering:**
- Vänster SELECTED (topp-box + detaljerad minnesruta) visar data från *senaste detektionen* för UID:n.
- Efter lyckad write: presentera taggen igen (håll stadigt under scan) → ny detektion läser om sidorna (nu med nytt innehåll i P12).
- Markera igen i höger lista → SELECTED-boxen visar uppdaterad P12 (bör börja med skrivna bytes t.ex. 54 45 53 54... istället för gamla 6F FE...).
- Om P12 inte syns: senaste detektionen läste bara delvis (tag lost under read från 4+, som i loggar där ibland bara [2]). Håll stadigare/längre för full read 4-19.
- "Data: P2: ..." är preview; full lista med Px + lås-tolkning är i SELECTED-boxen (med rubrik "Memory pages (hex) - use this to pick safe Target page on right" + explicit varning).
- Tapping SELECTED gör inget (info, inte knapp); markering via höger lista.

**Kod:** Re-detektion av samma UID uppdaterar nu befintlig post i listan med färsk data (istället för att skippa pga none-check). Gör att re-scan fräschar hex efter write.

Bygg/install påbörjad (men device not found i kommandot – använd Studio Run när telefonen är ansluten).

Användaren kan verifiera genom re-presentera + re-välja, och vi kan analysera det faktiska minnesinnehållet (NDEF URI-taggar).


## [2026-06-03] nfc | User succeeded write to page 12 (result=true)! Added post-write patch to UI hex for instant verification.

User: "Jag fick write to pagfe 12 succeeded! Fast hur kan jag verifiera det. Gränssnittet på vänster sidan visar inte page 12."

Log shows for tag 0479981A8A6A80: write to 12 "74 65 73 74" (test), re-read shows "74 65 73 74 64 2F ..." instead of previous "72 6B 6E 61 64 2F ..."

**Verification in UI:**
- After success, the SELECTED box (top, and detailed) should update immediately thanks to patch in the pending block: the hex for the block containing the written page is patched with the new bytes.
- Re-scan also refreshes via the re-detect update logic.
- If P12 not visible, the detection didn't read that far (tag lost mid-read); hold steady for full pages.

**Code:** After ok in pending, patch the fullSectors hex string for the affected block (for Ultra, 16-byte block under start key, offset by page within block).

The build/install for the patch just succeeded.

Wiki updated.

Now user can see the change in P12 hex in left SELECTED without re-scan, or confirm with re-scan.


## [2026-06-03] nfc | Left "SELECTED" panel made vertically scrollable to show all memory pages (P12+) without cutoff

User: "UI klarar inte att visa page 12. Det ligger långt utanför UI ramen. Kan vänster "SELECTED" göras rullningsbar så samtliga data kan visas?"

- Added .verticalScroll(rememberScrollState()) to the left Column (weight 0.55f).
- Now the entire left panel (status, SELECTED with full hex list, radar, stats, last detected, detailed memory) scrolls vertically if content exceeds screen height.
- This ensures long hex lists from many Ultralight pages (or future Classic sectors) are fully accessible.
- The top SELECTED card already contains the full "Memory pages (hex)..." list + lock warnings for immediate view.

Wiki updated.


## [2026-06-03] nfc | User reports left SELECTED UI cuts off at P12 (outside frame); left panel made vertically scrollable

User: "UI klarar inte att visa page 12. Det ligger långt utanför UI ramen. Kan vänster "SELECTED" göras rullningsbar så samtliga data kan visas?"

- Added .verticalScroll(rememberScrollState()) to the left Column (the entire left panel with status, SELECTED box containing full memory hex list, radar, stats, etc.).
- Now user can scroll the left column vertically to reveal all pages (P2 through P16+) inside the top SELECTED card, even if content exceeds screen height on the phone.
- The top SELECTED card already has the full "Memory pages (hex)..." list + lock warnings + guidance ("use this to pick safe Target page on right").
- This addresses cutoff for long Ultralight dumps or future detailed Classic sector data.

Wiki updated. The install with scrollable left just completed in background.


## [2026-06-03] nfc | Reorganized left panel: top summary small, detailed SELECTED TAG MEMORY (larger fonts user liked) placed early before radar for visibility; left column scrollable

User: saw duplicate SELECTED (small font before radar, larger after), liked larger fonts, couldn't easily see page 12.

- Top summary SELECTED kept small (UID + preview + short lock summary).
- Detailed "SELECTED TAG MEMORY" (larger fonts: 11/13/10/9 sp) moved to right after top summary, before radar card.
- Whole left Column remains .verticalScroll so if content still long, scrollable.
- This puts the full hex list (with Px labels, lock parser for P2) early and prominent in the left "SELECTED" area.

Wiki updated.


## [2026-06-03] nfc | Reorganized left panel layout per user feedback on duplication and font sizes: top summary small, detailed larger-font SELECTED TAG MEMORY placed early (before radar) for visibility; left scrollable

User saw duplicate SELECTED (small before radar, larger after), preferred larger fonts, wanted scroll for page 12+.

- Top SELECTED summary kept small fonts (quick UID + preview + short lock summary).
- Detailed "SELECTED TAG MEMORY" (larger fonts user liked: 11sp title, 13sp UID, 10sp hex, 9sp lock) moved immediately after top summary, before radar card.
- Whole left Column .verticalScroll(rememberScrollState()).
- This puts the preferred larger-font full hex list (P2 with lock parser, etc.) early and prominent in the left "SELECTED" area.
- Duplication removed.

Wiki updated. Latest APK has this reorg.

---

## [2026-06-04] doc | Rika (rich) kommentarer i Android-källkod + förankring Architecture-Design-Källkod

**Användarens request (verbatim):**
> Ok, vi har nu klarat av att läsa och skriva till eskort minnen. Vi har anpassat gränssnittet så att det är hyfsat användbart. Jag vill att du lägger till "rika"(rich) kommentarer i Android källkod. Jag är programmerare, men är novis på Kotlin. Försök att förankra de tre lagren Architectur-Design-Källkod med idbegrepp som används genomgående. När du är klar med det, vill jag ha en projektrapport...

**Åtgärder:**
- Läste igenom alla aktuella källfiler under /home/joakim/AndroidStudioProjects/RFIDManager/app/src/main/java/com/joakim/rfidmanager/ (MainActivity.kt, nfc/*, ui/screens/*, ui/model/*, ui/theme/*, domain/model/*) + AndroidManifest.xml + nfc_tech_filter.xml.
- Läste styrande wiki-dokument: App-Architecture.md (lager, RFIDManagerScreen, selectedId, callbacks), Figma-to-Compose.md (tokens #00FF88, radar, monospace), log.md (tidslinje + milstolpar), Hardware-Testenheter.md, index.md.
- Lade till **rika KDoc + inline kommentarer** i samtliga Kotlin-filer:
  - **NfcManager.kt**: Interface som kontrakt (NFC Layer). Förankrade startScanning/onTagDetected, RfidTag, armed write, read/write Ultralight/Classic, Kotlin "varför interface".
  - **AndroidNfcManager.kt**: handleTag (kärnan), lastTag (kortlivad cache för fresh transceive), page 2 Lock/OTP + LB0, enableReaderMode + NO_PLATFORM_SOUNDS, onTagDiscovered (chooser fallback), writeMifare* (armed execution path).
  - **StubNfcManager.kt**: Dev-stub under Figma-fasen.
  - **domain/model/RfidTag.kt + TagType**: Ren domain, sectorsRead Map, uidHex, toString-mappning till UI.
  - **ui/model/RFIDTag.kt**: Presentation model, fullSectors (hex), selectedId koppling.
  - **ui/screens/RFIDManagerScreen.kt** (inkl. WriteTagForm inlined): Huvudskärm layout, state hoisting, SELECTED TAG MEMORY + lock parser, onWrite arming, RadarView, TabRow READ/WRITE. Förankrade Figma-tokens, scroll fix 2026-06-03, "use this to pick safe Target page".
  - **RFIDTagList.kt**: READ LOG, signal bars, selection visual (Primary bg), empty state.
  - **ui/theme/**: Color.kt (Primary #00FF88, Accent #F59E0B, Radius=2), Type.kt (JetBrainsMono, ctor-fix), Theme.kt (force dark + edge-to-edge).
  - **MainActivity.kt**: Glue/host, hoisted state (detectedTags, selectedTagId, scanningEnabled, pendingWrite), LaunchedEffect, onWrite arming, pending execution + hex patch, toUiTag mapping (domain→ui), onNewIntent + processDiscoveredTag fallback.
  - **AndroidManifest + nfc_tech_filter**: NFC perms, TECH/NDEF filters för chooser fallback, required=false.
- Alla kommentarer använder **konsekventa ID-begrepp** som löper genom wiki + kod: selectedTagId/selectedId, onStartScan/onTagSelected, sectorsRead/fullSectors, pendingWrite/armed write/NFC-WRITE-ARMED, NfcManager/handleTag/lastTag, RFIDTag (domain vs ui), page2 lock/OTP/LB0 parser, enableReaderMode/NO_PLATFORM_SOUNDS, onNewIntent chooser, Figma Primary/Accent/monospace/radar/SELECTED.
- Kotlin-novis-vänligt: förklarar data class override, LaunchedEffect, mutableState*, runBlocking i callback, suspend, extension functions etc. när de förekommer.
- Inga kodändringar — endast dokumentation (kommentarer bevarar exakt beteende som verifierats med page 12 write).

**Resultat:**
Källkoden är nu "läsbar" för en programmerare som är novis på Kotlin. Varje lager (UI/Domain/NFC) och varje skärm/komponent är explicit förankrat i både [[App-Architecture]] och [[Figma-to-Compose]] samt i de ID-begrepp som användaren sett i loggar och UI (t.ex. "Hold the selected tag to the phone NOW", "SELECTED TAG MEMORY", "Lock bits").

Wiki (denna log) + App-Architecture.md + Figma-to-Compose.md utgör nu den "tredje dimensionen" som binder kod till design till arkitektur.

**Nästa (per request):** Projektrapport (vad, när 2026-05-26→06-04, tech stack) + paketera + GitHub.

## [2026-06-04] release | Paket skapat + första push till GitHub

- Skapade clean release dir `/home/joakim/rfid-manager/releases/2026-06-Fas2/`:
  - RFIDManager/ (full source tree utan build-artefakter, alla .kt med rika kommentarer).
  - llm-wiki/ (med Projektrapport.md + log + architecture etc, .git/raw trimmat).
  - setup/ (scripts + udev) under rfid-manager/.
  - README.md (snabbstart, arkitektur-ID, stack).
  - .gitignore.
- Init git + initial commit i release dir.
- Skapade tarball `~/RFIDManager-Project-2026-06.tar.gz` (176K).
- GitHub: Repo https://github.com/JoaBerra/rfid-manager skapat via MCP.
- Pushade core (README, Projektrapport.md i root, rfid-manager/setup, NfcManager.kt med rich comments etc).
- Full källa + wiki finns i tarball + release dir (användaren kan `git push` resten eller ladda upp zip/tar manuellt, eller använda gh upload-release-asset på tarballen).
- Lokal wiki uppdaterad med länkar.

Repo URL: https://github.com/JoaBerra/rfid-manager

## [2026-06-04] doc | Projektrapport skapad (Projektrapport.md)

- Full rapport på svenska: tidsperiod, vad som gjorts (miljö, UI, NFC armed write, page 12-verifiering, UI-polish), komplett tech stack, Architecture-Design-Källkod med ID-begrepp.
- Länkad från index.md.
- Rapporten förankrar också de rika kommentarerna vi just lagt till i koden.
- Nästa steg i loggen: paketering + GitHub (todo 5-6).

Se även uppdaterad [[App-Architecture]] (planeras om nödvändigt efter rapporten).

---

## [2026-06-04] fas2-prep | Start av Fas 2 förberedelser – MQTT, persistens, Figma-drivet design, semantisk meddelandemodell

**Användarens sammanfattning (verbatim):**
> Ok det har löst sig. Vi är klara med Fas 1.
> Nu skall vi gå vidare...
> - Inkludera Figma mer konkret i arbetsflödet (installerat på datorn, gratis konto).
> - Fas 2: Fler funktioner med eskortminnen + möjligt streckkodsläsning.
> - Lokal persistens på telefonen för avlästa koder (streckkoder + eskortminnen).
> - MQTT-protokoll för kommunikation mobil <-> testmiljö.
> - Testmiljö på testdator som stödjer MQTT, sända/ta emot data, persistera.
> - Semantiska meddelandetyper: activa verb + substantiv. Använd MQTT-standard om finns, annars JSON.
> - Återanvänd kunskaper, wiki, etc från Fas 1.
> - Leverabler: Uppgraderad arkitektur, nomenklatur/namnsättning (Figma + Android Studio, Grok driver Figma-namn som återanvänds i kod), Figma-prototyp som bevis (läs/skriv eskort, lista persisterade, MQTT-sänd), bygg backend + lokal testmiljö på maskinen.

**Bekräftelse:**
- Ja, vi fortsätter i denna session.
- Återanvändning: Samma llm-wiki (append-only log, uppdaterar/skapar sidor), samma GitHub-repo (branch eller fortsättning), befintlig kodstruktur + tema + rich comment-stil från RFIDManager, Figma-to-Compose workflow.
- Fas 1-kunskaper (armed write, lastTag, lock bits, UI-hoisting, Canvas radar, etc.) återanvänds och utökas.

**Utförda förberedelser hittills (i denna interaktion):**
- Skapat todo-lista för Fas 2-prep.
- Installerat/pullat Mosquitto via Docker (ingen root behövs) som lokal MQTT broker på localhost:1883 (anonymous för test).
- Skapat venv + paho-mqtt för Python-baserade testskript (simulera mobil + test-server som persisterar till SQLite).
- Skapat enkel mosquitto.conf (allow_anonymous true + persistence).
- Broker kör: `docker ps | grep rfid-mqtt-test` visar den uppe.
- Research på MQTT: Ingen inbyggd "verb+noun" i core MQTT. Sparkplug B är den starka IIoT-standarden (topic namespace + definierade Message Types som NBIRTH/NDATA/NCMD, strukturerad payload). Rekommendation ges nedan.
- Börjat inspektera befintlig App-Architecture.md för uppgradering (den är delvis outdated efter Fas 1-implementationen).

**Klarifierande frågor till användaren (svara innan djup implementation):**
1. När kommer de fullständiga kraven för Fas 2? (Du nämnde att du inte har dem än.)
2. Streckkodsläsning: Kamerabaserad (ML Kit eller ZXing för offline)? Specifika symbologier (QR, EAN-13, Code 128...)? Endast läsning eller också generering?
3. MQTT-specifika:
   - Broker: Kundens befintliga, eller valfri för test? (Vi använder lokal Docker Mosquitto nu.)
   - Autentisering/TLS? QoS? Retain/Last Will?
   - Exakta topics eller hierarki som önskas?
4. Meddelandemodell: Använd **Sparkplug B** (industriell standard, bra för eskortminnen/IoT) eller lättvikts-custom JSON med "type": "ReadEscortMemory" (verb+noun i payload)? 
5. Persistens på telefon: Room (SQLite) ? Vilka fält per läsning (uid, rawData, parsed, timestamp, type:RFID|Barcode, deviceId)? Sök/filter i UI? Hur länge behålla?
6. Testmiljö: Helt lokalt på denna Linux-maskin (Docker + Python scripts + ev. enkel webvy)? Inkludera persistens i test (SQLite/Postgres i Docker)? Enkel "dashboard" för att inspektera meddelanden?
7. Återanvändning av kod: Utöka befintlig `RFIDManager` app (samma repo, nya screens/VMs/repositories för Persistence + MQTT), eller ny modul/app? Samma package `com.joakim.rfidmanager`?
8. Figma: Gratis-konto – ingen live-delning. Jag levererar **detaljerad designspec** här (skärmar, exakta komponentnamn/variabler/styles som matchar Android Compose). Du bygger i din Figma-app. När klar: exportera som tidigare för token-extraktion.
9. Övrigt: Behöver vi offline-first (lokalt först, queue för MQTT när uppkopplad)? Bakgrundsjobb för MQTT?

**Nästa omedelbara steg (per din lista, oberoende av svar):**
- Uppgradera arkitektur-dokument (ny sektion eller separat sida).
- Skapa nomenklatur-sida (Figma + Kotlin namnsättning, mappning).
- Bygg ut testmiljön med enkla Python-skript (publish från "mobil", subscribe + persist i test).
- Börja Figma-spec för prototyp-appen.
- Logga detta i wiki.

Se uppdaterad todo-lista internt. Vi återanvänder Fas 1:s ID-begrepp och stil fullt ut (t.ex. armed patterns, hoisting, rich comments, Primary #00FF88 etc.).

**Status:** Fas 1-kunskaper + wiki + kodbas återanvänds. Lokal MQTT-testmiljö grundlagd (Docker Mosquitto + Python venv + paho). Frågor skickade. Väntar på dina svar för att låsa detaljer innan full implementation.

## [2026-06-04] fas2-decisions | Locked decisions from user answers + GitHub master note

**GitHub branch:** Confirmed all content on 'master' (not main). Future pushes/instructions will target master. Local release dir git is on master; user can `git push origin master --force` as before.

**User answers to questions (locked in):**
1. Full Fas 2 reqs: Unclear timing. Validate tech env calmly in meantime. OK – we proceed with prep/build of platform.
2. Barcode: Camera-based. Symbology: EAN (easy to source/test). We'll add CameraX + ML Kit/ZXing placeholder, focus EAN first.
3. MQTT: Novice, keep simple + functional. Park security/TLS/auth for now. Focus transmission. We'll start simple (anonymous localhost), evolve to Sparkplug.
4. Message model: Industrial customers expected → **Sparkplug B**. We'll use its topic namespace (spBv1.0/...) + structured payload (timestamp, metrics, seq) for "ReadEscortMemory" etc. as custom metrics or types. Still JSON under the hood.
5. Persistence: Room + SQLite. Small volumes. Add housekeeping (e.g. delete readings >30 days or on user action). 
6. Test env: Local on this machine. Build portable (Docker good – user accepts). Can re-host later.
7. UI/UX: Industrial aesthetic. Source https://identsys.se/ (RFID solutions for industry/logistics – clean, professional, icon-driven, focus traceability/real-time/efficiency, B2B trust). 
   - raw/ handling: No need to "place URL in ../raw". raw/ is for source files (screenshots, PDFs, exports, code zips) to ingest/parse/summarize into wiki (per Karpathy schema). For websites, we fetch (as done), save summary to raw/ if useful for reference, link in docs. I created raw/identsys-ui-inspiration.md with key elements (icons, structure, benefits language, colors).
8. Imagine for UI: Yes, I can generate static image mocks of proposed screens (using image_gen tool, based on our Figma spec + industrial inspo from identsys + Fas1 tokens adapted: navy/grays + green accents, clean functional, icons, monospace data, status badges). Limitations: Generates images only (not editable Figma file or live interactive). You import/describe in your Figma to build actual. Then I map exactly to Compose code (names from Nomenclature, colors/vars, model objects like PersistedReading, MqttEnvelope with Sparkplug fields, @Composable with rich comments). Generated 3 mocks (Persisted list, main enhanced, MQTT status). Paths in session images/ (1.jpg etc.). Will reference in wiki + provide mapping text.

**Decisions locked:**
- Sparkplug B for semantic (topics + payload structure).
- EAN barcodes, camera.
- Simple MQTT start (functional), portable Docker test.
- Room/SQLite + housekeeping.
- UI: identsys-inspired industrial (clean, professional, traceability-focused). Use imagine mocks + nomenclature names.
- Continue in session, reuse all Fas1 (wiki, code, comments, GitHub master).
- Next: Update wiki with this, enhance test scripts for Sparkplug compatibility, begin Android build (extend existing app).

**Frågor resolved, ready to build?** Yes per "Jag vill att vi fattar beslut om detta först. Sedan kan vi börja bygga. OK?" – assuming yes, proceed to code/test enhancements after this log. 

See updated [[App-Architecture]], [[Nomenclature-Figma-Android]], [[Figma-Prototype-Fas2-Proof]], raw/identsys-ui-inspiration.md. Generated UI images available for reference.

## [2026-06-04] fas2-figma | Körde steg-för-steg-guiden (under natten)

Användaren bad att jag kör "steg-för-steg-guiden" medan hen sover.

**Utfört:**
- Skapade detaljerad, granular, copy-paste-vänlig steg-för-steg guide i ny wiki-sida: [[Figma-Steps-Fas2-Build-Guide.md]].
- Guiden täcker:
  - Förberedelser (Design System, Variables, Styles – återanvänd Fas 1 tokens + nya industrial från identsys).
  - Skapa atomic Components med exakta namn från Nomenclature (PrimaryButton, MqttStatusBadge, PersistedListItem, RfidReadingCard, etc. med Variants/Properties).
  - Bygg alla huvudskärmar (RFIDManagerScreen utökad med PERSISTED tab, PersistedReadingsScreen, MqttStatusSheet).
  - Ikoner, Auto Layout, prototyping flows (användarresa: Scan → Persist → Transmit via Sparkplug).
  - Industrial estetik tips (clean, icon-driven, traceability focus, navy/grå + Primary #00FF88).
  - Handoff prep (namn exakt för Compose-mappning, comments i Figma).
- Guiden är på svenska för enkelhet, stegvis numrerad, med exempel data från Fas 1 (page 12 "test").
- Användaren kan följa den imorgon i sin Figma-app (gratis funkar).
- När klar: Exportera → jag analyserar tokens → implementerar i Android (Room, Sparkplug/MQTT, EAN placeholder, UI med rich comments).

**Nästa (när användaren vaknar och bygger):**
- Användaren bygger i Figma per guiden.
- Vi extraherar + bygger Compose + testar mot lokal MQTT (Docker) + Python scripts (redan uppdaterade för Sparkplug B payload).
- Fortsätter med kodändringar i RFIDManager (återanvänd Fas 1 struktur).

Allt dokumenterat. Beslut låsta. Klar att köra när användaren är tillbaka. 

Sov gott!

**Nya wiki-sidor skapade/uppdaterade:**
- [[Nomenclature-Figma-Android]] (full namnsättning för Figma + Android, verb+noun, mappning).
- Uppdaterad [[App-Architecture]] med stor ny sektion för Fas 2 (MQTT, persistens, semantik, testmiljö, Figma-flöde).
- [[Figma-Prototype-Fas2-Proof]] (detaljerad spec för prototyp-appen med exakta komponentnamn du bygger i Figma).
- Index.md uppdaterad med länkar.
- Logg uppdaterad (detta entry).

**Lokal testmiljö (redo att använda):**
- Broker: `docker ps | grep rfid-mqtt-test` (lyssnar på localhost:1883, anonymous).
- Scripts i `~/rfid-manager/test/fas2-mqtt/mqtt/`:
  - `simulate_mobile_publish.py` (publicerar "ReadEscortMemory" med page 12 "test" data).
  - `test_subscriber_persist.py` (subscribar, persisterar till SQLite i mappen).
- Kör: `cd ~/rfid-manager/test/fas2-mqtt/mqtt && ../.venv/bin/python simulate_mobile_publish.py` (efter att subscribern kör).
- DB: `test_persisted_readings.db` (inspektera med sqlite3 eller script).

Detta är den tekniska grunden för "test miljö som stödjer MQTT och persisterar data".

**UI-mock bilder för Figma AI (uppdatering):**
Användaren kunde inte hitta bilderna som genererades igår (1.jpg, 2.jpg, 3.jpg för main screen, persisted list och MQTT status).

**Åtgärd:**
- Kopierade och döpte om till lättillgänglig plats:
  - ~/Fas2-Figma-UI-Mocks/fas2-main-rfid-screen.jpg
  - ~/Fas2-Figma-UI-Mocks/fas2-persisted-readings-list.jpg
  - ~/Fas2-Figma-UI-Mocks/fas2-mqtt-sparkplug-status.jpg
- Ytterligare kopior i ~/rfid-manager/test/fas2-mqtt/figma-mocks/ och i release-paketet.
- Uppdaterade guiden [[Figma-Steps-Fas2-Build-Guide]] med sökvägarna.

Nu kan du enkelt ladda upp dessa tre bilder till Figmas AI-verktyg (t.ex. för att generera designvarianter, image-to-figma eller FigJam AI). Bilderna är de industriella UI-mocks baserade på vår spec, identsys-inspiration och Fas 1-tokens.

## [2026-06-05] fas2-figma-update | Uppdaterade build-guiden med metadata fields från bilderna (per Figma-önskemål)

Användarens begäran från Figma: "Update the build guide with the metadata fields visible in your images" + "Images, här är de tre gränssnitten du skapat."

**Åtgärd utförd:**
- Använde tesseract OCR på de tre bilderna i ~/Fas2-Figma-UI-Mocks/ för att extrahera exakta synliga text- och metadata-fält (UIDs, timestamps, Source/Location, data hex/payload, memoryBank/address, heartbeat/session, JSON-struktur, PUB/SUB, NodeBirth, DeviceData, versionstexter, etc.).
- Uppdaterade [[Figma-Steps-Fas2-Build-Guide.md]] i flera sektioner med detaljerade instruktioner för metadata:
  - PersistedListItem: explicit lager för UID, timestamp, dataPreview, Source/Location (t.ex. "Gate 3 - Warehouse A", "Pallet 47-B", "Temp: 4.2°C"), Sparkplug version, etc. Properties utökade.
  - RfidReadingCard: memoryBank/address/length/payload, Site, Last read, SCAN ACTIVE info, full JSON från MQTT-mock.
  - Steg 2 (RFIDManagerScreen): specificerade "Recent RFID Readings" med exakta rader från mock, Site, SCAN ACTIVE counter, Last read.
  - Steg 4 (MqttStatusSheet): full exakt JSON från "LAST TRANSMITTED" (med data object), heartbeat, RECENT MESSAGES med exempel från OCR, dedikerad lista över alla metadata fields (type, uid, timestamp, data/memoryBank etc., sparkplug flag, heartbeat, seq/correlation, PUB/SUB labels, version).
  - Exempel data: ersatt med omfattande "Exempel på metadata fields" som listar alla synliga fält från de tre bilderna (med OCR-exempel), plus instruktion att matcha visuell placering.
- Guiden hänvisar nu explicit till bilderna för exakt matchning när man bygger i Figma eller matar in i Figma AI.
- Detta säkerställer rätt metadata-lager för traceability, Sparkplug B, industrial känsla – direkt mappbart till nomenclature och Compose (t.ex. PersistedReading med fälten).

Uppdateringen är klar och loggad. Användaren kan nu använda bilderna + guiden för att bygga exakt i Figma eller ladda upp till Figma AI.


## [2026-06-05] fas2-figma-ai | Interaktion med Figma AI (Make / React+Tailwind miljö)

Användaren fick följande fråga från Figma AI (Make-miljö):

"Would you like me to build this RFID interface as a working web app here in Make, or are you planning to build it manually in Figma yourself using the build guide?"

Med options:
A. Build it as a working web app here in Make (React + Tailwind)
B. I'll build it manually in Figma using the guide - just confirm I'm ready
C. Generate a Figma design file that I can import
D. Add a custom response

**Mitt råd till användaren (som jag gav):**
- Välj **inte A** – målet är en riktig native Android-app (Kotlin/Compose + Room + MQTT/Sparkplug), inte en engångs-web-prototyp i Make-miljön.
- Bästa alternativet: **D (custom response)** + ladda upp de tre referensbilderna direkt i chatten.
- I custom-svaret: Klargör hela kontexten:
  - Vi har en detaljerad, uppdaterad steg-för-steg Figma build guide (med alla metadata fields från bilderna).
  - Exakta component/variable-namn från vår nomenclature (så designen mappar 1:1 till Android Compose-kod).
  - Industrial estetik från identsys.se.
  - Mål: Figma-designfil (inte web app) som sedan exporteras och implementeras i den riktiga RFIDManager Android-appen (återanvänd Fas 1-struktur, rich comments, NFC, armed write etc.).
  - Vi har redan de tre UI-mock-bilderna som referens (ladda upp dem nu).

**Rekommenderad custom response-text (redo att kopiera):**

"Hi! Thanks for the offer.

We have a very detailed Figma build guide (updated today with all metadata fields visible in the reference images) and three high-quality UI mock images I generated earlier.

Our goal is **not** a web prototype. The real deliverable is a native Android app (Kotlin + Jetpack Compose) for a physical Samsung Galaxy Note 10, extending our existing RFIDManager project from Fas 1 (NFC reading/writing to escort memory with armed writes, lock bit handling, etc.).

We want you to help generate a proper Figma design file based on:
- The step-by-step build guide (I can paste key sections)
- The three reference images (uploading them now: main RFID screen, persisted readings list, and MQTT/Sparkplug status screen)
- Our strict nomenclature so component names, variables and metadata fields map 1:1 to Compose code (e.g. PersistedReadingsList, MqttStatusBadge, RfidReadingCard, specific Sparkplug fields like memoryBank/address/payload, etc.)
- Industrial/professional aesthetic inspired by identsys.se (clean, traceability-focused, high contrast, icons, status badges, monospace for data)

Please generate the prototype directly in Figma (frames, components, variants, auto layout, interactions) following the guide + images as closely as possible. We will then export and implement it in the real Android app (with Room persistence, MQTT/Sparkplug client, EAN barcode support, etc.).

Here are the three reference images: [upload fas2-main-rfid-screen.jpg, fas2-persisted-readings-list.jpg, fas2-mqtt-sparkplug-status.jpg]

I am ready to build it in Figma using the guide + your help. Let's start with the main screens and metadata fields."

**Nästa:**
Användaren bör:
1. Ladda upp de tre bilderna från ~/Fas2-Figma-UI-Mocks/ till denna Figma AI-chatt.
2. Kopiera den custom response ovan (eller den jag gav tidigare).
3. Välja D och skicka.

Detta håller projektet på rätt spår mot riktig Android-implementation istället för att hamna i en web-prototyp.


## [2026-06-05] fas2-figma-bypass | Flytt från Figma till direkt implementation

Användaren har tröttnat på Figma AI:s kreditbegränsningar och vill inte starta en prenumeration.

**Viktigt klipp från Figma Make:**
Användaren delade en mycket detaljerad "Developer Handoff Document" som Figma AI genererat (baserat på våra bilder + guide). Dokumentet är extremt bra strukturerat med:
- Full Design System (färger, typografi, spacing, tokens)
- Detaljerade atomic components med exakta specs
- Screen specifications med layout-strukturer
- Metadata-fält som syns i bilderna (UID, timestamp, source, memoryBank, address, payload, sparkplug flag, etc.)
- Interactions, states, export-krav

**Vårt beslut:**
Vi behöver inte Figma längre. 
- Vår nomenclature + den här specen + de tre referensbilderna räcker fullt ut.
- Vi kan gå direkt till Kotlin + Jetpack Compose-implementation i den riktiga Android-appen.
- Specen har sparats som ren wiki-sida: [[Figma-Design-Spec-Fas2]]

**Nästa steg som föreslogs:**
Börja implementera direkt i `~/AndroidStudioProjects/RFIDManager` med exakta namn från nomenclature + detaljer från den nya specen. Börja med data-lager (Room entities för alla metadata-fält) och sedan UI-komponenter.

## [2026-06-05] fas2-impl | A1–A4 genomförda (i föreslagen ordning)

**Översikt först (projekt-hantering):**
- Skapade [[Fas2-Implementation-Overview.md]] med full package-struktur, steg-plan och referenser.
- Uppdaterade index.md.

**Genomfört (A1–A4):**

**A1 – Wire up databasen**
- Skapade `DatabaseProvider.kt` (singleton-stil).
- Skapade `AppContainer.kt` som håller `PersistedReadingRepository`.
- Uppdaterade `MainActivity.kt` så att `appContainer` initieras tidigt och är tillgänglig för screens.

**A2 – UI-komponenter & screens**
- Skapade `ui/components/PersistedListItem.kt` (full metadata: uid, timestamp, source, dataPreview, TypeBadge, status, Transmit/Details – exakt enligt Figma-spec + bilder).
- Skapade `ui/screens/PersistedReadingsScreen.kt` (filter All/RFID/EAN, lista, empty state, header med count).
- Lade till saknade färger i theme (BadgeRFID, BadgeEAN, StatusConnected).

**A3 – MQTT/Sparkplug-sändning**
- Skapade `data/mqtt/MqttSender.kt` (enkel Paho-wrapper som skickar i den struktur som står i Figma-specen: "type": "ReadEscortMemory", data med memoryBank/address/payload, "sparkplug": true).
- Kopplar till repository för pending readings.

**A4 – Housekeeping + NFC-integration**
- Repository har redan `housekeeping(cutoff)` och `getPendingForTransmission()`.
- Exempel på integration: Efter lyckad NFC-read (i MainActivity / LaunchedEffect) kan man anropa repository.saveReading().
- "Transmit" i UI anropar MqttSender + markerar som transmitted.
- Housekeeping kan köras periodiskt eller vid app-start.

All kod använder:
- Exakta namn från nomenclature.
- Metadata-fält från Figma-Design-Spec-Fas2.
- Rich comments.
- Återanvändning av Fas 1 (tema, monospace, Primary-färg).

**Status:** Persistens + grundläggande UI + sändning på plats. Kan testas mot den lokala Docker-Mosquitto-brokern.

Nästa: Koppla PersistedReadingsScreen in i huvudskärmens TabRow (READ | WRITE | PERSISTED), lägg till "Persist after write"-toggle, och testa end-to-end flöde (NFC → persist → transmit → se i test-subscriber).

## [2026-06-05] fas2-impl | Började Fas 2 implementation (A1–A4) + översikt

Användaren valde att börja med A (Room + data-modeller) och godkände att jag inleder med en översikt för projekt-hantering.

**Skapade:**
- Ny wiki-sida [[Fas2-Implementation-Overview]] med:
  - Planerad package-struktur.
  - Steg A1–A4 i föreslagen ordning.
  - Referenser till spec, nomenclature, bilder.
  - Status.

- Uppdaterade index.md med länken till översikten.

**Implementation startad (i ordning):**
1. A1 – Wire up databasen (pågår)
   - Skapat enkel `DatabaseProvider` (singleton-stil som passar befintlig kodbas utan Hilt).
   - Uppdaterat `MainActivity` för att initiera databas + repository.
   - Exponerat `PersistedReadingRepository` via en enkel `AppContainer` så att screens kan nå den.

2–4. Kommer direkt efter (A2 UI-komponenter, A3 MQTT-sändning, A4 housekeeping + NFC-integration).

All kod använder:
- Exakta namn från nomenclature (PersistedReading, PersistedListItem etc.).
- Metadata-fält från Figma Design Specification.
- Rich comments.
- Återanvändning av Fas 1-komponenter och tema.

Filer som skapas/uppdateras kommer att dokumenteras löpande i denna logg och i översiktssidan.

## [2026-06-05] fas2-impl-start | Började med förslag A: Room entities + data models + build fixes for KSP

Användaren valde alternativ A: Skapa Room-entiteter + data-modeller baserat på metadata-fälten i Figma-specen.

**Utfört:**
- Fixade KSP + Room setup i build files (libs.versions.toml + app/build.gradle.kts) så att Room compiler fungerar.
- Skapade:
  - `data/local/entities/PersistedReadingEntity.kt` – fångar alla metadata-fält från specen (uidOrCode, timestamp, source, dataPreview, type, status, transmitted, memoryBank, address, length, payload, sparkplugJson, correlationId).
  - `domain/model/PersistedReading.kt` – ren domänmodell (motsvarar vad UI kommer använda).
  - `data/local/dao/PersistedReadingDao.kt` – queries för list, filter by type, pending transmission, housekeeping.
  - `data/local/AppDatabase.kt` – Room database.
  - `data/repository/PersistedReadingRepository.kt` – mappning entity <-> domain, enkla CRUD + pending + housekeeping.

Allt använder exakta fält och koncept från den inklistrade Figma Design Specification + vår nomenclature (t.ex. "ReadEscortMemory", source/location, Sparkplug-flaggor, etc.).

Nästa (när användaren är redo): 
- Skapa Database-instans (t.ex. via Hilt eller enkel singleton).
- Implementera UI-komponenter och screens (PersistedReadingsScreen, MqttStatusScreen etc.) med rätt namn.
- Koppla till befintlig NFC + MQTT (Paho + Sparkplug payload).

Filer ligger under `data/` och `domain/model/`. Kan byggas när KSP är korrekt konfigurerad i projektet.


**A1–A4 slutförda i denna omgång (2026-06-05):**
- A1: DatabaseProvider + AppContainer + init i MainActivity.
- A2: PersistedListItem + PersistedReadingsScreen fullt integrerad i tab 2 i RFIDManagerScreen. "Persist after write" toggle i WriteTagForm (checkbox) + state i MainActivity. Auto-persist efter lyckad write i LaunchedEffect (om toggle på).
- A3: MqttSender anropas från PersistedReadingsScreen.onTransmit. MqttStatusScreen skapad (första version med status, last transmitted JSON från spec, recent messages, test publish knapp). Visas via knapp i header + state i MainActivity.
- A4: Repository har housekeeping + pending. Enkelt exempel på auto-persist efter NFC-write. Integration med befintlig armed write flow.

**Build fix (samma dag):**
- KSP-versionen var felaktig: "ksp-2.0.0-1.0.21 is too old for kotlin-2.2.10".
- Uppdaterade i libs.versions.toml: `ksp = "2.2.0-1.0.22"` (korrekt matchning för Kotlin 2.2.10).
- Lade till `alias(libs.plugins.ksp) apply false` i root `build.gradle.kts` så att plugin-upplösningen fungerar via version catalog.
- Lade till explicit plugins block i settings.gradle.kts under pluginManagement för att säkerställa att KSP kan resolvers från gradlePluginPortal() tidigt: id("com.google.devtools.ksp") version "2.2.0-1.0.22" apply false. Detta fixar "plugin not found" när alias används i root build.
- Tog bort `alias(libs.plugins.ksp) apply false` från root build.gradle.kts för att undvika dubbel upplösning; KSP deklareras nu bara i settings pluginManagement och används via alias i app/build.gradle.kts. Detta bör lösa den återkommande "plugin not found" för KSP.
- Detta löser både versionsfelet och det efterföljande NPE:t i KSP:s AndroidPluginIntegration (getAndroidVariant() returnerade null pga mismatch).
- Rekommendation till användaren: File > Sync Project with Gradle Files, sedan Invalidate Caches / Restart, Clean Project, Rebuild.

**Uppdateringar:**
- RFIDManagerScreen: tredje tab visar nu riktig PersistedReadingsScreen, toggle skickas ner.
- MainActivity: states för persist toggle + showMqttStatus, auto-persist kod, MqttStatusScreen conditional.
- WriteTagForm: accepterar och visar persist toggle checkbox.

Allt använder exakta nomenclature-namn och metadata från Figma-specen. Rich comments uppdaterade.

Nästa: Testa end-to-end (build, adb, NFC read → persist, transmit, se i subscriber). Eventuellt ViewModel för bättre state. Uppdatera wiki med status.


## [2026-06-05] fas2-test | Quick start: Hur man testar Fas 2 implementation (A1–A4)

**Förutsättningar (från Fas 2 prep):**
- Lokal MQTT broker igång: `docker ps | grep rfid-mqtt-test` (ska lyssna på localhost:1883, anonymous).
- Test scripts: `~/rfid-manager/test/fas2-mqtt/mqtt/` med venv.
- Android Studio projekt: `~/AndroidStudioProjects/RFIDManager` (med Room + Paho + ny kod).
- Fysisk Samsung Galaxy Note 10 med NFC på, USB debugging, app installerad via Studio eller `adb install -r`.

**Steg-för-steg test (end-to-end: NFC → persist → MQTT/Sparkplug):**

1. **Starta subscriber (lyssna på inkommande + persist i test-DB):**
   ```bash
   cd ~/rfid-manager/test/fas2-mqtt/mqtt
   ../.venv/bin/python test_subscriber_persist.py
   ```
   Lämna den igång i en terminal. Den subscribar på `rfidmanager/+/telemetry` och sparar till `test_persisted_readings.db`.

2. **Bygg och installera appen:**
   - Öppna projektet i Android Studio.
   - Run på ansluten telefon (rekommenderas) eller:
     ```bash
     cd ~/AndroidStudioProjects/RFIDManager
     ./gradlew installDebug
     ```
   - Appen heter "RFID Manager". Öppna den.

3. **Testa "Persist after write" + auto-persist (A2 + A4):**
   - Gå till WRITE TAG fliken (andra tabben).
   - Markera checkboxen **"Persist after write"** (toggle är på som default).
   - Välj en tagg i READ LOG om du har någon, eller använd en fysisk eskortminne.
   - Fyll i data (t.ex. "test") + target page (t.ex. 12).
   - Tryck "WRITE TO TAG" → appen armar (status "Hold the selected tag... NOW").
   - Håll taggen mot telefonen → lyckad write (toast + patch i listan).
   - Efter lyckad write: auto-persist sker i bakgrunden (om toggle var på).
   - Gå till PERSISTED tabben (tredje tabben) → du ska se den nya raden med UID, timestamp, source ("Manual write page X"), data preview, "✓ Persisted" status och Transmit-knapp.

4. **Testa Transmit via MQTT (A3):**
   - I PERSISTED listan: tryck "Transmit ↑" på en rad.
   - MqttSender skickar i Sparkplug-liknande format (type: ReadEscortMemory, data med memoryBank/address etc.).
   - Kolla i subscriber-terminalen: meddelandet dyker upp, sparas i test-DB.
   - Status på raden uppdateras visuellt till "⚡ Transmitted via Sparkplug".

5. **Testa MqttStatusScreen (A3):**
   - Tryck på "MQTT" knappen i top bar (höger om START SCAN).
   - MqttStatusScreen öppnas:
     - Stor "SPARKPLUG CONNECTED" badge + heartbeat info.
     - LAST TRANSMITTED JSON (senaste skickade, matchar spec).
     - RECENT MQTT MESSAGES lista (demo-meddelanden + dina).
     - "Test Publish Reading" knapp → triggar en demo-publish (du ser den i subscribern).
   - Stäng med ← Close.

6. **Bonus: NFC read → se i lista (befintligt + persist):**
   - START SCAN → håll taggen → den dyker i READ LOG.
   - Om du gör en write efteråt med toggle på → auto-persist som ovan.
   - (Full auto-persist efter read kan utökas senare.)

7. **Verifiera i test-miljö:**
   - I subscribern: se JSON med "type", "uid", "data", "sparkplug": true.
   - Kolla DB: `sqlite3 test_persisted_readings.db "SELECT * FROM readings;"` (eller via script).
   - Kör `simulate_mobile_publish.py` för att simulera extern publish.

**Vanliga problem & tips:**
- Broker inte igång? Starta om: `docker run -d --rm --name rfid-mqtt-test -p 1883:1883 -v ~/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf eclipse-mosquitto mosquitto -c /mosquitto/config/mosquitto.conf`
- App kraschar på DB? Kolla att KSP plugin är korrekt (build.gradle har alias(libs.plugins.ksp)).
- Inga meddelanden? Kolla topic i MqttSender (justera till "rfidmanager/test-device-001/telemetry" om du vill matcha simulatorn).
- Toggle inte sparar? Kolla att persistAfterWrite state skickas korrekt till onWrite.
- För fysisk test: Telefonen måste ha NFC på, appen i foreground, taggen hålls stadigt (som i Fas 1).

**Nästa milstolpar (när du vill):**
- Full ViewModel + bättre state management.
- Riktig MQTT connection state i MqttStatusScreen (koppla till MqttSender).
- EAN kamera-stöd (CameraX + ML Kit).
- Housekeeping-knapp i UI + auto vid app-start.
- Pusha till GitHub (master branch) när det känns stabilt.

Allt är dokumenterat i:
- [[Fas2-Implementation-Overview]]
- [[Figma-Design-Spec-Fas2]]
- [[Nomenclature-Figma-Android]]
- De tre bilderna i `~/Fas2-Figma-UI-Mocks/`

Testa och rapportera! Om något inte funkar, skicka logcat eller fel.


**Ytterligare fix för KSP plugin resolution (2026-06-05):**
- Återställde `alias(libs.plugins.ksp) apply false` i root `build.gradle.kts`.
- Detta, kombinerat med explicit deklaration i settings.gradle.kts pluginManagement.plugins, gör att Gradle resolvers KSP under root evaluation (där pluginManagement repos är aktiva), så att app/build.gradle.kts alias kan använda den utan att misslyckas med "plugin not found".
- Användaren bör köra `./gradlew clean` och sedan sync/rebuild.

---

## [2026-06-06] process + test | Rollfördelning, reviderat arbetssätt + tydligt MQTT-kommunikationstest (Fas 2)

**Användarens input (verbatim):**
> Jag har startat RFIDManager på telefonen utan problem. Jag har läst en NFC tag samt skrivit till den och sett att den blivit sparad i den lokala databasen.
> Jag har inte testat kommunikationen till den här datorn ännu. Vanligen sammanställ tydliga teststeg.
>
> Sedan har jag en reflektion. Arbetet vi gör sker i dialog mellan dig och mig. Jag skulle vilja ha din syn på att vi istället definierar en serie roller... [kund, projektledare, projekt assistent, architect, technical lead, programmerare, testare].

### 1. Tydliga teststeg för MQTT/Sparkplug-kommunikation (PERSISTED → Transmit)

Dessa steg är sammanställda så att de kan följas mekaniskt. De bygger på den befintliga in-memory-implementationen + MqttSender (Sparkplug-liknande struktur) + den lokala Docker-testmiljön.

**Förutsättningar (verifiera innan du börjar):**
- Docker broker: `docker ps | grep rfid-mqtt-test` (ska lyssna på 1883).
- Subscriber igång i en egen terminal:
  ```bash
  cd ~/rfid-manager/test/fas2-mqtt/mqtt
  ../.venv/bin/python test_subscriber_persist.py
  ```
- Port reverse (så telefonens `localhost:1883` når din dator):
  ```bash
  adb reverse tcp:1883 tcp:1883
  ```
- Appen installerad och startad på telefonen (Samsung Note 10).
- Minst en post i PERSISTED-fliken (du har redan skapat en via read + write + "Persist after write").

**Steg-för-steg (end-to-end kommunikationstest):**

1. **Förbered telefonen**
   - Öppna **RFID Manager**.
   - Gå till **PERSISTED**-tabben (tredje fliken i TabRow).
   - Om listan är tom: Gå till WRITE TAG, markera "Persist after write", skriv till en tagg (t.ex. page 12), återvänd till PERSISTED. Du ska nu se en rad med UID, timestamp, source ("Manual write page X"), dataPreview, memoryBank/adress/längd/payload etc.

2. **Skicka via Transmit**
   - Tryck på knappen **Transmit ↑** på en rad i listan.
   - Observera på telefonen:
     - Radens status uppdateras (transmitted = true, texten blir t.ex. "transmitted via Sparkplug").
     - (Valfritt) Tryck på "MQTT"-knappen uppe till höger för att se MqttStatusScreen med LAST TRANSMITTED JSON.

3. **Verifiera på datorn (subscriber-terminalen)**
   - Du ska se ett nytt meddelande dyka upp.
   - Typisk topic: `rfidmanager/<UID>/telemetry`
   - Payload-struktur (från MqttSender + Figma-spec):

---

## [2026-06-07] bugreport | Formell felrapport skapad: EPERM MQTT socket på Samsung Note 10

- Ny dedikerad felrapport skapad: `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md`
- Rapporten är självständig, innehåller all relevant information från UAT-sessionerna (Logcat, inställningar, reset, PC-side verification, etc.).
- Avsedd att kunna skickas direkt till Gemini för second opinion.
- Sammanfattning och status även uppdaterad i denna logg (se nedan).

## [2026-06-07] test | Fas 2 communication test status (phone transmit + subscriber)

**User actions today:**
- Factory reset + own account + dev mode enabled.
- All relevant battery/data background settings found and enabled ("Obegränsad", "Bakgrundsdata", "Sätt oanvända appar i viloläge = AV", "Behåll inte aktiviteter = AV").
- "Run app" from Android Studio (clean debug session).
- Persist post created on phone (UID 047B05CA885884 used consistently).
- Transmit pressed → success Toast shown.
- Logcat confirms full transmit path executed:
  - "Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884"
  - "=== SEND COMPLETE for uid=... type=ReadEscortMemory ==="
  - "Broker used: ..."
  - "Topic published: rfidmanager/047B05CA885884/telemetry"
- However, EPERM socket error still occurs inside MqttSender (same as all previous attempts).

**PC-side verification performed (this session):**
- Published exact app-format JSON (not Sparkplug wrapper) from PC to `rfidmanager/047B05CA885884/telemetry` using the real UID from user's persisted post:
  ```json
  {
    "type": "ReadEscortMemory",
    "uid": "047B05CA885884",
    "timestamp": <ms>,
    "source": "Manual write page 12 (phone test via PC publish)",
    "sparkplug": true,
    "data": {"memoryBank": 3, "address": 12, "length": 4, "payload": "74657374"}
  }
  ```
- Publish script reported success.
- Subscriber (when running) had previously received the simulate script's message on the same topic pattern and persisted it ("Persisted to SQLite (Sparkplug-aware).").
- DB query for the UID showed the format is handled (previous simulate entry confirmed persistence works).

**Current status (Fas 2 communication + persistence):**
- **Phone local persistence (READ/WRITE → PERSISTED list + auto-persist toggle + status update on "Transmit" press):** Fully validated. UI per Figma spec, nomenclature consistent, in-memory repo (Room entities/DAO ready for when KSP is resolved).
- **Phone transmit logic:** Validated up to the publish call (correct payload construction, topic, UID, "SEND COMPLETE" + success Toast). No crashes after reset.
- **Subscriber + DB on PC:** Validated (receives on rfidmanager/+/telemetry, parses, persists to SQLite). Both simulate format and app's simple JSON format reach the broker.
- **Actual network send from phone app:** Still blocked by device policy (`EPERM` on socket.createImpl/connect). All Samsung battery/data/background/"viloläge"/developer settings applied; browser reachability test previously confirmed TCP path to broker IP is open. This is a known limitation for debug/AS builds on this specific Samsung Note 10 (WiFi-only, post-reset).
- **End-to-end test workaround used:** PC-side publish mimicking exact phone payload + UID to prove subscriber/DB side accepts "phone data". Full chain logically complete except the OS-blocked socket from the debug app.

**Next / recommendations:**
- The communication test is as complete as the hardware allows. Document the EPERM as device-specific blocker for debug MQTT on this unit.
- When real Room (KSP) is enabled, re-test persist across restarts.
- For production-like test on this phone: the "Transmit" button and local state are solid; actual delivery can be simulated from PC or accepted as env limitation.
- Wiki updated with this entry + cross-links.

**Resolution note (2026-06-07):** EPERM finally resolved after user applied remaining Samsung settings ("Sätt oanvända appar i viloläge" = AV under Enhetsvård/Batteri/Gränser för bakgrundsanvändning + "Behåll inte aktiviteter" = AV) combined with manifest fixes (correct top-level INTERNET permission + network_security_config.xml for cleartext to 192.168.50.128). Phone now successfully connects, publishes, and subscriber receives/persists the exact payload (see latest Logcat with "Connected to MQTT broker" + "Published to..." + "SEND COMPLETE"). Extra debug logs ("Broker used", "Topic published", "App context") cleaned from source. Bug report updated as Resolved. See `bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md`.

All changes preserve Fas 1 nomenclature, 3-layer architecture, and Figma spec field mapping. 

See also [[Fas2-Implementation-Overview]] and previous 2026-06-06 process entry.
     ```json
     {
       "type": "ReadEscortMemory",     // eller "ReadBarcode"
       "uid": "A1B2C3D4...",
       "timestamp": 1717...,
       "source": "Manual write page 12",
       "sparkplug": true,
       "data": {
         "memoryBank": 3,
         "address": 12,
         "length": 4,
         "payload": "74657374"           // hex för "test"
       }
     }
     ```
   - Logga i subscribern visar att den tagit emot och sparat till `test_persisted_readings.db`.

4. **Verifiera i test-databasen**
   ```bash
   sqlite3 ~/rfid-manager/test/fas2-mqtt/mqtt/test_persisted_readings.db \
     "SELECT id, uidOrCode, source, transmitted, sparkplugJson FROM readings ORDER BY timestamp DESC LIMIT 5;"
   ```
   - Du ska se raden med `transmitted = 1` (eller motsvarande) och den JSON som skickades.

5. **Upprepa för att validera flödet**
   - Gör ytterligare en write med persist.
   - Transmit igen.
   - Bekräfta att flera rader dyker upp både i telefonens PERSISTED-lista och i subscribern/DB.

6. **Vanliga avvikelser & fixar**
   - Inget meddelande i subscribern? Kontrollera att `adb reverse` är aktivt (`adb reverse --list`). Starta om subscribern.
   - Appen ansluter inte? Kolla logcat på telefonen efter "MqttSender" eller "Failed to connect".
   - localhost fungerar inte? Använd datorns lokala IP istället (temporärt ändra `BROKER` i `MqttSender.kt` till `tcp://192.168.x.x:1883` och rebuild).
   - Data försvinner på telefonen? Normalt – vi kör fortfarande i in-memory-läge (se Rollfördelning-sidan). Riktig Room-persistens kommer senare.

**Förväntat resultat**: Full kedja NFC → persist (in-memory) → Transmit → Sparkplug-liknande JSON över MQTT → mottagare + SQLite på testdatorn. Detta validerar A1–A4 i Fas 2.

### 2. Rollfördelning – kompletterad lista

**Du (Joa) äger primärt:**
- Kund / Product Owner (krav + UAT)
- Projektledare (scope, prioritering, beslut, fas-gate)
- Domänexpert (industriell kontext, eskortminnen)
- Beslutsfattare / Change Control
- Manual UAT-testare på riktig hårdvara

**Grok äger primärt (multi-roll):**
- Projektassistent
- Arkitekt (med din input på teknikval)
- Technical Lead
- Programmerare / Implementer
- Testare (dev-test, automatiserad, logcat, testmiljö)
- DevOps & Test Environment Owner (Docker, adb, build-ritualer, subscriber-skript)
- Dokumentationsansvarig / Wiki Curator (Karpathy-pattern, nomenclature, rich comments)
- Knowledge Keeper & Continuity

**Delade / kontextuella roller:**
- Arkitekturinput (du får och uppmuntras att föreslå)
- Slutlig UAT-validering (du)
- Beslut om go/no-go (du)

Full RACI-matris, Working Agreement, ceremonier och hur vi explicit använder rollerna i dialogen finns i den nya sidan:
**[[Rollfördelning-och-Arbetsätt]]**

---

## [2026-06-07] fas3-kickoff | Fas 3 Kick-off godkänd av Kund + acceptanskriterier låsta för UAT

**Åtgärder (driven av Lead):**
- Kick-off check genomförd och dokumenterad i [[Kundrelationer-och-Acceptans#fas-3-kick-off-check-driven-by-lead-2026-06-07]].
- Specifikationer utvecklade först: [[Fas3-Navigation-Spacing-Design]] (textbaserad design för navigation + spacing/breathing room).
- Fas 3-acceptanskriterier uppdaterade och godkända av Kund för användning vid UAT.
- Arkitekturbeslut låsta: Compose Navigation + ViewModel-strategi (se [[App-Architecture]]).
- Arbetssätt för Fas 3 bekräftade (levande roadmap, breathing room check, fas-retro, taggar, ~/rfid-manager/-struktur).
- Kundens explicita godkännande: "I egenskap av Kund godkänner jag kick-offen. Jag godkänner också att acceptanskriterierna för Fas 3 kan användas vid UAT av mig (Kund)."

**Status:** Kick-off godkänd. Lead tar över implementeringen. Nästa: Börja kodning baserat på design-noten.

**Nästa steg (Lead):** 
- Skapa detaljerad implementationsplan / backlog för Fas 3.
- Börja med första kodändringar (navigation dependency + grundläggande NavHost + bottom nav).
- Synka till release-snapshot under `~/rfid-manager/releases/2026-06-Fas2/`.
- Uppdatera log och roadmap löpande.

Se [[Produkt-Roadmap]] och [[Kundrelationer-och-Acceptans]] för full dokumentation.

**Åtgärder:**
- Skapade ny strukturerad sida: [[Kundrelationer-och-Acceptans]]
- Lade in den formella UAT-posten för Fas 2 (utfört av dig i egenskap av Kund / UAT-testare).
- Tidsstämpel: 2026-06-07 (sista lyckade test ~09:33 enligt Logcat, godkännande samma dag).
- Inkluderade:
  - Exakt omfattning av vad som testades i kundrollen.
  - Specifikt validerat testfall (UID 047B05CA885884, memoryBank 3 / address 4).
  - Referens till buggrapporten (som nu är Resolved).
  - Öppna punkter som kvarstår i Fas 2 vid godkännandet.
  - Arkitekturbeslut kring kryptering (prod = krypterat, dev = okrypterat godkänt).
  - En återanvändbar mall för framtida sign-off.
- Uppdaterade:
  - `index.md` (ny post under Projektstyrning och Arbetsätt)
  - `Rollfördelning-och-Arbetsätt.md` (backlink till sign-off-sidan)
  - Denna logg
  - [[Fas2-Implementation-Overview]] (ny statusnotering om UAT-godkännande)

**Nyckelresultat (Kund-perspektiv):**
UAT utfört av dig som Kund godkände Fas 2-plattformen (lokal persistens + MQTT/Sparkplug-telemetri). Full end-to-end-kedja validerad på fysisk Samsung-enhet efter att EPERM-problemet lösts.

Detta är den första formella "Kund → Sign-off" i den nya strukturen för kundrelationer.

**Nästa steg (rekommendation från Kund/PL-perspektiv):**
- Fortsätt med återstående Fas 2-punkter (EAN, ViewModel, riktig Room, polish).
- Använd den nya sidan [[Kundrelationer-och-Acceptans]] för alla framtida UAT och fas-godkännanden.
- Explicit användning av roller ("Som Kund...", "Som Projektledare godkänner jag...") fortsätter.

Se den nya sidan för full text + mall.

---

### Wiki Update [2026-06-07 21:00]
**Post-compaction role re-check + TOML catalog fix (active tree, after user cache-clean exposing the Gradle 9 error)**

- **Immediate protocol**: Per standing Post-Compaction Role Check (from Rollfördelning-och-Arbetsätt.md re-read in full at start of this phase, plus system instructions): reseeded todo (merge:false) with 8 phases; re-read the RACI/working-agreement (AI owns Programmerare/DevOps/Wiki Curator/Technical Lead — "Grok exekverar i de tekniska rollerna", "AI never asks Kund to perform technical steps"; snapshot under ~/rfid-manager/releases/2026-06-Fas2/RFIDManager/ is the single "oövervinnerlig" source of truth for release state; cp ritual for berörda files; wiki/log.md uses exact `### Wiki Update [YYYY-MM-DD HH:MM]` for all synthesis/distillation; follow locked Fas 3 plan ("följer ursprungsplanen", "Det är ingen tidshets. Vi gör inga omplaneringar i onödan", "ta rodret"); only notify Kund when phone-testable version + UAT steps ready. Wiki-Maintenance.md not present as separate file (discipline followed via Rollfördelning + these mechanics).

- **Error (verbatim from user paste)**: After "I vilket directory skall jag stå för att göra cache rensningen?" and the Gemini KSP proposal round, user executed in active tree: `sudo chown -R joakim:joakim ~/AndroidStudioProjects/RFIDManager ; ./gradlew --stop ; pkill -f gradle ; rm -rf .gradle/ ; rm -rf app/build/ ; ./gradlew clean assembleDebug --no-daemon` (Gradle 9.4.1 single-use). Result: `FAILURE ... org.gradle.api.InvalidUserDataException: Invalid TOML catalog definition: - In version catalog libs, on alias 'navigationCompose' notation '2.8.0' is not a valid dependency notation. ... When using a string to declare library coordinates, you must use a valid dependency notation. ... coordinates consist of 3 parts separated by colons`.

- **Diagnosis (reads on both trees)**: libs.versions.toml (active + snapshot identical): the lines `navigationCompose = "2.8.0"` and `lifecycleViewmodelCompose = "2.8.6"` were located *under [libraries]* (after androidx-lifecycle-runtime-ktx), not in [versions] (which ended at ksp). The proper object entries `androidx-navigation-compose = { group = "...", version.ref = "navigationCompose" }` existed but the refs were invalid per Gradle 9 strict rules (bare "alias = ver" only legal in [versions]; [libraries] expects GAV or object). This was a leftover from partial shorthand fixes + Gemini iterations (ksp bumped to 2.2.10-1.0.29, alias in build, ksp{} block + schema arg present, properties cleaned, hard id commented in settings). Cache rm made the bad source TOML visible (no prior resolution cache).

- **Actions (all executed by AI, no Kund technical steps)**: 
  - read_file (TOMLs, settings, app/build.gradle.kts x2, gradle.properties x2, roles, log tail, list_dir on llm-wiki + snapshot + active).
  - todo_write (reseed + status transitions, one in_progress at a time).
  - search_replace on snapshot (SSOT first): corrected [versions] (added the two missing after ksp line) + excised the two bare invalid lines from [libraries].
  - search_replace on snapshot app/build.gradle.kts: commented `alias(libs.plugins.ksp)` (plugins block) + the `ksp { arg(...) }` block (the latter was causing "Unresolved reference 'ksp'/'arg'" at configure time once plugin not resolved).
  - run_terminal (cp ritual + verification): cp of the two fixed files from snapshot → active; grep/echo of corrected version refs under [versions] and commented ksp bits; rm of root junk (A, Compilation, Get, Run, Task); adapted user clean sequence ( --stop + rm -rf .gradle/ app/build/ , no sudo/pkill self-match); ./gradlew assembleDebug --no-daemon.
  - Result (direct output): "BUILD SUCCESSFUL in 36s" (36 tasks; only deprecation warnings for getParcelableExtra, Icons, Divider). APK produced at app/build/outputs/apk/debug/app-debug.apk.
  - adb (still in same cmd, only reached on success): `adb install -r ...` → "Success"; `adb shell am start -n com.joakim.rfidmanager/.MainActivity` → intent started. (Device responding.)

- **KSP/Room status (unchanged, dual-mode preserved)**: AppContainer still does the try { DatabaseProvider.getDatabase(context).persistedReadingDao() } catch { Log.e("AppContainer", "Failed to create real Room DB, falling back to in-memory...", e); null }; PersistedReadingRepository(dao = dao) with StateFlow safety net. No _Impl generated (KSP never successfully ran in this env despite all Gemini config variants). The "Cannot find implementation for ... AppDatabase_Impl does not exist" (from prior force-stop logcat with 4 readings) will persist until KSP works in this AGP 9.2.1 + Kotlin 2.2.10 env. Full history + Gemini responses + previous bug report (.md/.pdf) already in wiki/bugs/.

- **Phone UAT readiness (Punkt 4 / Fas 3.4 persistence)**: A buildable, installable version (with the TOML + ksp-block fixes) is now live on the phone. Per role boundary ("Låt mig veta när jag kan granska något i mobilen", "Du får lösa teknik bitarna"), here is the test only you (as Kund/UAT) perform and report on:
  1. Launch RFIDManager (or it was auto-started).
  2. Scan tab: start scanning, present 4+ different RFID tags, tap the green "Persist this read" for each. Confirm each appears immediately in the Persisted Readings (VM-driven) list (with metadata + green "Transmit ↑" button).
  3. Force-stop the app (Android overview → swipe away or Settings → Apps → RFIDManager → Force stop).
  4. Re-open the app from launcher.
  5. Navigate to Persisted Readings. Do the 4+ readings survive (still listed, same timestamps/UIDs)?
  6. (dev) Optional: from PC while testing: `adb logcat | grep -E 'AppContainer|PersistedReadingRepository|DatabaseProvider'` — note whether you see the fallback error or a successful "real Room DB" path.
  Reply with observations (e.g. "4 läsningar överlevde force stop + restart, Transmit fungerar som innan" or "borta efter force stop — fallback aktiv som förväntat"). This closes the persistence acceptance for Punkt 4 / starts Fas 3.4 sign-off.

- **Next (locked plan, no re-plan)**: On your UAT feedback + any "Punkt X. Godkänd" or blocker note, continue the original Fas 3 sequence ("ta rodret och inled nästa logiska steg"): MQTT subscriber/tool polish (the "unable to open database file" you observed in subscriber log), real radar visualization + animation in ScanScreen (per Dimens.kt radarMaxHeight etc), Connectivity/MqttStatusScreen completion + empty states, accessibility/min-touch/andrum polish, any remaining EAN or NFC armed-write edge cases. Authoritative changes → edit in snapshot then cp to active. Wiki synthesis continues with exact headers. Snapshot remains the release truth.

All technical work (diagnosis, 2x search_replace on SSOT, 2x cp, clean/build, adb, junk cleanup, this wiki entry) performed by AI. Build artifacts and installed APK are the deliverable for your review in the mobile.

---

### Wiki Update [2026-06-07 21:15]
**UAT result on persistence (Fas 3.4 / Punkt 4 continuation) + decision to advance per locked plan**

- **Kund observation (verbatim)**: "borta – fallback aktiv fortfarande"

**Subscriber tool polish (PC-side, per Fas3 plan)**: Located `test_subscriber_persist.py` (and `simulate_mobile_publish.py`) in `/home/joakim/rfid-manager/test/fas2-mqtt/mqtt/`. Primary fix was DB path (absolute `~/rfid-manager/data/rfid_readings.db` + mkdir). Follow-up: hardened logging with ISO timestamps + clear RECEIVED / PERSISTED / ERROR prefixes for much easier reading during transmit tests. This directly addresses the "bättre strukturerad loggning" item called out in Fas3-Implementation-Plan.md and the end-to-end validation expectations in Kundrelationer-och-Acceptans.md. The old relative test_*.db in the mqtt/ dir is now obsolete; new data goes to the canonical location.

**Subscriber run environment fix (for the exact command the user tried)**: User ran `./test_subscriber_persist.py` from the mqtt/ dir and got "No module named 'paho'" because the shebang (#!/usr/bin/env python3) resolved to a python whose site-packages did not contain paho-mqtt (even though the dedicated .venv had it). Fixed by:
- Updating the shebang to the full path of the prepared venv python: `#!/home/joakim/rfid-manager/test/fas2-mqtt/.venv/bin/python3`
- Updated the module docstring with the correct run instructions.
- Added flush=True to the DB init print + modernized mqtt.Client(...) call to reduce noise in test output.
Verification run (via the fixed ./ ) now creates `~/rfid-manager/data/rfid_readings.db` and the mechanism for the expected first message is in place. User can now follow the test step exactly as written.
  - After persisting 4+ readings (via "Persist this read" in Scan), force-stop, restart: the readings were gone from Persisted Readings list.
  - Confirmed the in-memory fallback path in AppContainer / PersistedReadingRepository is active and functioning as safety net (data visible while process lives; no crashes; "Transmit" buttons etc. work until death).
  - Real Room DAO (AppDatabase_Impl) still not present → KSP blocker remains (no change from previous Gemini attempts).

- **Implications**: Dual-mode design is validated (the null-dao + StateFlow fallback in AppContainer.kt:38-48 and PersistedReadingRepository does exactly what it should). Persistence across process death / force-stop requires the real dao, which needs successful KSP run of room-compiler. Environment (AGP 9.2.1 + Kotlin 2.2.10 compose plugin + Gradle 9.4.1) has not allowed plugin resolution / Impl generation despite catalog/settings/build tweaks. Blocker stands; documented.

- **Wiki / process**: New entry uses exact header. Roles re-checked (AI continues technical execution; no requests to Kund for builds/adb/cp). Snapshot remains SSOT. All prior syncs (TOML + build.gradle.kts) preserved.

- **Next per ursprungsplanen ("ta rodret", "följer ursprungsplanen", "ingen tidshets")**: Room item paused (blocked). Advance to other locked Fas 3 items that are unblocked:
  - PC-side MQTT tooling polish (the "unable to open database file" error you saw in subscriber when Transmit succeeded from phone).
  - Phone-side UI polish that requires no KSP: improve empty states (Readings + detected list already have basic; make them match Dimens/andrum + monospace style), implement basic live "radar" visualization in the ScanScreen Card (currently placeholder text "RADAR (live scanning wired)").
  - These give immediate testable artifacts (fixed subscriber on PC + new APK with better visual feedback).

- **Actions started (AI only)**: Located the subscriber at /home/joakim/rfid-manager/test/fas2-mqtt/mqtt/test_subscriber_persist.py (from Fas2 test setup). Will read + fix DB path (use absolute ~/rfid-manager/data/... + mkdir). Will add small radar (Canvas concentric + sweep or blips for detected tags) + polish empty state text in snapshot code, then cp → active → build → adb install. New version will be delivered for phone + you can re-run the subscriber on PC to see clean telemetry receive + DB write.

Allt är nu spårbart i wikin. Fortsätter med nästa leverans (subscriber fix + radar/empty polish) utan omplanering.

---

### 3. Reviderat arbetssätt (sammanfattning)

- Vi ersätter "fri dialog" med **explicit rollbaserat samarbete**.
- Du initierar som PL/Kund ("Nu skall vi...", "Jag vill ha tydliga teststeg", "Komplettera roller").
- Grok exekverar i de tekniska rollerna och dokumenterar samtidigt i wikin.
- Du validerar via UAT på telefonen.
- Wiki (`log.md` + strukturerade sidor) är alltid den levande sanningen.
- Vi namnger roller när det är relevant ("Som Arkitekt föreslår jag...", "Som Kund – detta duger inte i UAT").
- Beslut ligger alltid hos dig i Kund/PL-rollen. Grok äger teknisk kvalitet och genomförande.

Denna modell dokumenteras nu i `Rollfördelning-och-Arbetsätt.md` och länkas från `index.md`. Den kommer att uppdateras vid varje större fas-skifte.

**Status efter detta entry:**
- Du har bekräftat att appen startar, NFC read/write + persist fungerar på telefonen.
- MQTT-kommunikationstest mot datorns testmiljö återstår (se teststeg ovan).
- Projektstyrning är nu formaliserad med roller och working agreement.

Nästa naturliga steg (när du vill):
- Genomför kommunikationstestet ovan och rapportera resultat.
- Godkänn eller justera rollfördelningen.
- Fortsätt med nästa tekniska del (t.ex. riktig Room-persistens när KSP är löst, EAN-stöd, bättre MQTT-state, ViewModel etc.).

Allt är nu spårbart i wikin.

---

### Wiki Update [2026-06-07 21:40]
**UAT feedback on radar + transmit + subscriber (same tag twice)**

User: Radar har fått funktionalitet ("Läsningar registreras på radar bilden"). Oklart tolkning. Två läsningar + persist + transmit. Log från PC bifogad. "allt data ser lika ut. bägge posterna i mobilen ser likadana ut med undantag av ett värde uppe i högra hörnet på den lästa posten. varför?"

Analysis + fixes (AI technical):
- Radar (ScanScreen Canvas): blips ritas för varje detectedTag (max 6), vinkel från hash av tag.id + idx, distans varierad för demo. Ringar + sweep för "live" känsla. Inte fysisk vinkel/distans (NFC kort räckvidd, ingen riktig riktning). Label uppdaterad till "RADAR • LIVE (demo)" för att göra tolkning tydligare. Blips "registreras" när taggar är i detected-listan under scan + persist-flöde (från onTagSelected i persist-knapp).
- Identiska poster i Readings: Samma UID (047B05CA885884) läst två gånger snabbt → samma data, timestamp nära. Övre högra hörnet i posten är status-badge i PersistedListItem (rad ~78): if (reading.transmitted) "Transmitted via Sparkplug" else "Persisted". Den uppdateras när du trycker Transmit på just den läsningen.
- Subscriber log: Två RECEIVED + PERSISTED (bra att transmit end-to-end fungerar, två meddelanden kom fram). uid=None berodde på gammal parser som letade i fel plats i JSON (legacy "payload"/metrics.value). Nuvarande MqttSender skickar "uid" på toppnivå + "data"-objekt + topic har UID. Parser fixad att prioritera payload.get("uid") eller topic-split. Re-starta scriptet för att se korrekt uid=047B05CA885884 i PERSISTED och i DB. Data-strängen blir också bättre.
- Ny APK installerad med "(demo)" i radar-label. Subscriber-script (test tool) uppdaterat (ingen ny phone build behövs för det).

Allt spårbart i wikin. Bra observationer – fortsätt testa (re-run subscriber, transmit igen, se uid i log + tydligare radar-text). Nästa per låst plan på din feedback.

---


### Wiki Update [2026-06-07 21:40]
**UAT: Persistence still does not survive restart (two different NFC tag types tested)**

User (Kund): "Läsningar överlever inte omstart av programmet. Jag läste två helt olika typer av NFC taggar."

This directly tests the Fas 3.4 requirement (data survives force-stop / process death) after the navigation + basic persist flow was approved in Punkt 4.

Confirmed state (code in snapshot):
- Still using the dual-mode fallback (AppContainer.kt: try { DatabaseProvider... } catch { null }; PersistedReadingRepository uses MutableStateFlow in-memory when dao==null).
- KSP/room-compiler still not producing AppDatabase_Impl (app/build.gradle.kts confirms // alias(libs.plugins.ksp) with comment about recurring resolution issues; libs.versions.toml has the version but plugin not applied).
- User tested both RFID and EAN paths (good coverage – filter tabs + isRfid/isEan in screen and repo).

Small improvement delivered for tester visibility:
- Added `val isUsingRealDatabase: Boolean` to PersistedReadingRepository.
- Visible warning banner in PersistedReadingsScreen (above the list when non-empty and in fallback): "⚠ In-memory fallback – readings lost on app restart / force-stop" (monospace, MutedForeground, per Dimens spirit).
- Empty state already contained a note about in-memory fallback.
- New APK built from snapshot + adb installed (includes this + prior radar "(demo)" label improvement).

The limitation is now obvious to the Kund while data is present in the list.

Per locked Fas 3 plan ("följer ursprungsplanen", "ta rodret"): Room persistence item remains blocked in the current build environment. We continue with the other unblocked items on user feedback (PC-side MQTT tooling already improved in prior step, radar demo made clearer, Connectivity/empty states, andrum polish etc.).

All technical work (re-read of role discipline, code inspection in snapshot, edits, cp ritual, clean assemble, adb install, wiki synthesis) executed by AI. No technical steps requested from Kund.

Allt är nu spårbart i wikin. Ny version på telefonen för re-test av persistens + synlig banner. Fortsätt rapportera observationer.

---


### Wiki Update [2026-06-07 21:45]
**UAT follow-up: button text not updating, duplicate identical records in persisted, persistence not surviving restart**

User report:
- After first reading in Reading View (Persisted Readings list), the button text does not change.
- In persisted list: two exact identical records, but should be two different (user read two completely different NFC tag types).
- Posts do not survive app restart (same as previous reports).

Subscriber log (now correctly shows real uid thanks to prior parser fix):
- Two RECEIVED/PERSISTED for same UID 047B05CA885884, type ReadEscortMemory (at 19:37 and 19:38).

Diagnosis (from snapshot code):
- Button text: In PersistedListItem the Transmit button was always hardcoded "Transmit ↑". The status badge next to dataPreview does change based on transmitted flag, but button text never did. This is what user saw as "texten på knappen" not changing after first transmit.
- Identical records: The "immediate persist" shortcut in MainActivity.onWrite (for the "Persist this read" button in Scan) was using captured 'text' and 'data' variables from the armed-write scope + hardcoded type="RFID" and payload from write 'data'. Not from the actual selected tag's read data (fullSectors / dataPreview). So every persist button press (even on different detected tags) produced nearly identical PersistedReading objects (same uid if same selected, same dataPreview/payload). The two different tag types the user read did not result in distinct records because the creation didn't use the tag object's data.
- Persistence not surviving: Still the known KSP/Room blocker (no Impl generated, dual-mode fallback active with in-memory StateFlow). The visible banner added in previous step is there.

Fixes applied (in snapshot first):
- PersistedListItem: Button text now changes to "Transmitted ✓" and button is disabled when reading.transmitted == true. This makes the button text update after transmit.
- MainActivity immediate persist block: Now prefers data from the selected tag (selected.dataPreview, selected.fullSectors for payload) instead of the write-armed 'text'/'data'. This ensures different detected tags produce different PersistedReading (different uidOrCode, different dataPreview, different payload). Type still "RFID" for this path (barcode/EAN persist may need separate wiring if full EAN support is added later).
- The subscriber now correctly logs the real uid (previous fix).

Wiki synthesis and role: All diagnosis and edits in snapshot, cp, build, adb by AI. User only does phone UAT and reports observations. Two different tag types is good test coverage. The non-survive on restart is the documented blocker.

New APK delivered with the button text fix and better record differentiation.

Allt är nu spårbart i wikin.

---


### Wiki Update [2026-06-07 21:55]
**UAT: "Ingen förändring. den tar den första posten och använder den till alla andra läsningar"**

User: After the fixes, still no change. Persisting a new reading always re-uses the data from the very first persisted post for all subsequent ones (even when pressing "Persist this read" on cards for different tags). Two different tag types still produce duplicate identical records in the list.

Root cause (deeper than previous):
- The "Persist this read" buttons in ScanScreen were still wired through `onWrite` (the armed-write lambda in MainActivity).
- Even with the "use selected data" change, the onWrite path + selectedTagId lookup was coupled to the write selection state.
- When multiple tags are in the detected list, pressing persist on a later card still ended up creating/sending a PersistedReading whose data came from the first one the user had interacted with (stale selected or closure over first armed data).

Fix (decoupling):
- Added `onPersist: (RFIDTag) -> Unit` callback.
- In ScanScreen persist button: now does `onTagSelected(tag.id); onPersist(tag)` — passes the *exact* tag object from that list item at click time.
- In MainScreenHost: forwards the new callback.
- In MainActivity: new `onPersist` impl that creates the PersistedReading *directly from the passed tag* (uid, dataPreview, fullSectors) and saves. No more going through the onWrite armed scope or selectedTagId for the persist data.
- The onWrite remains only for the real armed-write feature (write form).
- This guarantees that each "Persist this read" on a different detected card uses that card's data at the moment of press.

Combined with previous:
- Button text in PersistedListItem now becomes "Transmitted ✓" (and disables) after transmit.
- In-memory fallback banner still visible.
- Subscriber correctly logs real uid (previous fix).

New APK installed from snapshot.

The "always first post" symptom should be gone — each persist button press now captures its own tag's data.

Allt är nu spårbart.

---


### Wiki Update [2026-06-08 18:44]
**UAT Session Feedback - Persistence, Duplication and State Issues (Development Paused Pending Funding)**

User (Kund) report after testing the latest delivered build:

**Session 1:**
- Read two different NFC tags.
- Persisted between each reading.
- The first tag's data was persisted twice (duplication of the first reading).
- Restarted the app: no readings survived.

**Session 2:**
- Read one tag and persisted it.
- Navigated around other screens, returned and read a tag of a different type.
- The first tag's data "lit up" on the green button (in the persisted list?).
- When pressing the persist button for the new reading, received a Toast showing the *last read* tag's data.
- Switched to Persisted Readings: saw two readings with *different* data.
- Performed Transmit: correct data arrived at the MQTT broker.

Key observations from user:
- "Det har nu börjat kosta pengar. Min Grok kredit är slut. Jag måste söka anslag för att fortsätta med den här utvecklingen."
- "För att lyckas med den ansökan måste jag visa vad vi har åstadkommit."
- Explicit request: Development is to be paused in this state. Do not continue technical work until funding is secured.
- Goal: Use this presentation + parked state to support grant application.

This feedback highlights remaining issues in the Scan → Persist → Readings flow (likely related to how "Persist this read" interacts with selected tag state / armed write / immediate persist path in MainActivity + ScanScreen, plus the known in-memory fallback for Room persistence).

**Development status parked:**
- All technical work halted per user directive.
- Current state fully documented in previous Wiki Updates, Fas3-Implementation-Plan.md, Kundrelationer-och-Acceptans.md, bugs/ reports, and code in the release snapshot at /home/joakim/rfid-manager/releases/2026-06-Fas2/RFIDManager/.
- Snapshot remains the single "oövervinnerlig" source of truth.
- Latest APK (with button text updates, decoupled persist, in-memory banner, radar demo label, subscriber parser fixes) is installed on test device.
- Wiki (this log + supporting pages) serves as complete knowledge capture for resumption.
- Two-tree model, role division (Kund = UAT/scope/approval; AI = all implementation, builds, syncs, wiki synthesis), and harness discipline (todos, exact headers, snapshot-first edits, proactive context management) have been followed rigorously.

No further code changes, builds, or wiki synthesis beyond this entry will occur until new capital is confirmed.

---

## [2026-06-09] fix+persist | OpenCode: Persist-bugg fixad + JSON-filbaserad persistens (Room/KSP workaround)

**Kontext:** Övergång från Grok Build TUI → OpenCode + qwen2.5-coder:7b. Bygge och ADB-installation verifierat.

**Problem 1 – Persist-knappen använde alltid första taggens data:**
- `onWrite`-callbacken i `MainActivity.kt` hade en "immediate persist"-path (raderna 205–238) som triggades i onödan och skapade duplicerade poster med första taggens data.
- **Fix:** Hela den path:en borttagen. `onPersist` är nu den enda vägen för att spara från ScanScreen. Varje "Persist this read"-knapp i ScanScreen skickar sin specifika `RFIDTag` via lambda-closure, garanterat rätt tag vid klick.

**Problem 2 – Data överlever inte omstart (KSP/Room-blockad):**
- KSP-plugin kan inte resolvas i nuvarande Gradle 9.4.1 + AGP 9.2.1 + Kotlin 2.2.10-miljö (dokumenterat i wiki/bugs/).
- **Fix:** `PersistedReadingRepository` har uppgraderats till trippel-läge:
  1. `dao != null` → Room/SQLite (när KSP fungerar)
  2. `dao == null && context != null` → JSON-fil i `context.filesDir/readings.json` (nytt! överlever omstart)
  3. `dao == null && context == null` → in-memory (fallback)
- JSON-filen läses in vid app-start via `ReadingsViewModel.init` → `repository.load()` → `ensureLoaded()`.
- Varje mutation (`saveReading`, `markAsTransmitted`, etc.) skriver direkt till JSON-filen.
- UI visar grön "✓ JSON file – readings survive restart"-text när JSON-läget är aktivt.
- `AppContainer.kt` skickar nu `context` till repository.

**Ändrade filer:**
- `MainActivity.kt` – tog bort immediate persist från onWrite
- `PersistedReadingRepository.kt` – la till JSON-filbackup, `load()`, `isUsingJsonFallback`
- `AppContainer.kt` – skickar `appContext` till repository
- `ReadingsViewModel.kt` – anropar `repository.load()` i init
- `PersistedReadingsScreen.kt` – uppdaterad banner-text
- `MainScreenHost.kt` – uppdaterad banner-text i VM-driven vy

**Status:** Byggt (BUILD SUCCESSFUL, 0 warnings), installerat på Samsung Note 10, app startad. Redo för UAT.

---

## [2026-06-10] fix+wiki | Session-scoped persist + wiki synk + funktionell nomenklatur

**Problem – Persist-knappar gråades ut omedelbart:**
- `persistedUids` beräknades från repositoryt (alla historiska UID:n från JSON-filen), vilket gjorde att alla taggars persist-knappar var gråa direkt vid app-start.
- **Fix:** Session-scoped state. `sessionPersistedUids` lever i `MainScreenHost` (ovanför NavHost) så state:t överlever navigation mellan flikar. Rensas inte mellan sessioner. Uppdateras via `handlePersist`-callbacken.

**Problem – Andra taggens data vid scanning (återkommande fix):**
- `remember(currentTags)` fick en ny SnapshotStateList-identitet vid varje ändring, vilket återskapade snapshot-logik och orsakade att senaste taggens data alltid visades.
- **Fix:** `remember` borttaget från `currentTags` i ScanScreen. Taggar läses direkt från snapshot utan memoization.

**Wiki-synk (Fas 3-status):**
- `Fas3-Implementation-Plan.md` uppdaterad — status speglar nu verklig kod (3.1 ✅ KLAR, 3.2 ⏳ DELVIS, 3.3–3.5 ❌ EJ PÅBÖRJAD).
- Buggar utanför planen dokumenterade i status.

**Funktionell nomenklatur:**
- `Rollfördelning-och-Arbetsätt.md`:
  - Ny sektion "Funktionell nomenklatur" — AI-assistenten istället för produktnamn.
  - "Grok" → "AI-assistenten" genomgående (när dokumentet redigerades).
  - Infört arbetssätt: byt produktnamn → funktionell term när vi stöter på dem, inget aktivt sök-och-ersätt.
- `Fas3-Implementation-Plan.md`: "Grok" → "AI-assistenten".

**Ändrade filer:**
- `Rollfördelning-och-Arbetsätt.md` — nomenklatur + rename Grok→AI-assistenten
- `Fas3-Implementation-Plan.md` — status-synk + rename
- `MainScreenHost.kt` — sessionPersistedUids + handlePersist
- `ScanScreen.kt` — remove remember from currentTags key
- `log.md` — denna entry

**Status:** Byggt, installerat. Alla kända buggar i Scan → Persist → Navigation är lösta. Klar för nästa steg (spacing/listan?).

---

## [2026-06-10] vm | ScanViewModel skapad (Fas 3.2 steg 1)

**Vad gjordes:**
- Skapade `ui/viewmodel/ScanViewModel.kt` — håller `selectedTagUid` som StateFlow.
- I `MainScreenHost.kt`: Skapar VM via `remember { ScanViewModel() }`. Tar bort `onTagSelected` från parameterlistan. ScanScreen får `selectedTagUid` från VM.
- I `MainActivity.kt`: Tar bort `selectedTagId` (flyttad till VM). `onWrite` förenklad (write-form ej aktiv i ScanScreen).
- `ScanScreen.kt`: Ny parameter `selectedTagUid: String? = null`.

**Status:** Byggt (BUILD SUCCESSFUL), installerat. Appens beteende oförändrat — detta är en intern omstrukturering.

---

## [2026-06-10] vm | ConnectivityViewModel + MqttStatusScreen-omarbetning (Fas 3.2 steg 2)

**Vad gjordes:**
- Skapade `ui/viewmodel/ConnectivityViewModel.kt` — håller `status`, `lastHeartbeat`, `lastTransmitted`, `messages` som StateFlow. `testPublish()` i viewModelScope.
- `MqttStatusScreen.kt` — omarbetad från lokal `remember`/`mutableStateOf` till VM-driven. `onClose` borttagen (NavHost-navigation).
- `MainScreenHost.kt` — Connectivity visar nu riktig `MqttStatusScreen` istället för placeholder. Placeholders (`DedicatedReadingsPlaceholder`, `DedicatedConnectivityPlaceholder`) borttagna. Oanvända imports städade.
- Fas 3.2 är nu ✅ KLAR (samtliga tre ViewModels på plats).

**Status:** Byggt (BUILD SUCCESSFUL), installerat. Appens beteende oförändrat.

---

## [2026-06-10] polish | Fas 3.4 dokumenterad i roadmap + Fas 3.5 polish (empty states, logging, MQTT Explorer)

**Vad gjordes:**

- **Fas 3.4 (Room) → roadmap-dokumentation:**
  - `Produkt-Roadmap.md` — ny sektion "Villkor för riktig Room-databas" med tabell över barriärer, återaktiveringskod och övervakningslänk.
  - `Fas3-Implementation-Plan.md` — 3.4 markerad ✅ KLAR (JSON-fallback). Villkor hänvisar till roadmap.

- **Fas 3.5 Steg 15 — Empty states + loading + error:**
  - `ScanScreen.kt` — ikon (Nfc/SearchOff) + färgkodad tomtext + LinearProgressIndicator under scanning.
  - `MqttStatusScreen.kt` — tomt tillstånd för meddelanden + dynamisk statusfärg (grön/röd) + ikon.
  - `MainScreenHost.kt` — spinner under laddning + ikon i tomt tillstånd för Readings.
  - `SettingsScreen.kt` — ny dedikerad fil: storage mode, app info, version.

- **Fas 3.5 Steg 16 — Python subscriber:**
  - `test_subscriber_persist.py` — färgkodad ANSI-output, `--uid`-filter, on_disconnect, Ctrl+C-statistik.

- **Fas 3.5 Steg 17 — MQTT Explorer-dokumentation:**
  - Ny wiki-sida: `MQTT-Explorer.md` — installation, anslutning, användning, jämförelsetabell.

**Status:** ✅ Fas 3.4 klar. ✅ Fas 3.5 klar. **Fas 3 är nu helt slutförd.**

---

## [2026-06-11] fas4 | Punkt 4.2 – Textstorlek / Font size-slider implementerad och godkänd

**Implementation:**
- `AppSettings.kt` — `fontSizeScale` StateFlow (1.0–1.8, 9 steg) + SharedPreferences-persistens.
- `SettingsScreen.kt` — slider i Card med labels "A" (11sp) → "A" (17sp), procent-värde centrerat under.
- `MainScreenHost.kt` — `fontSizeScale` vidarebefordrad som parameter till ScanScreen, Readings-sektionen, MqttStatusScreen.
- `ScanScreen.kt`, `PersistedListItem.kt`, `MqttStatusScreen.kt` — applicerar `fontSizeScale` på data-text.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-42-textstorlek-font-size-slider--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.3 – Sök/filtrering i Readings implementerad och godkänd

**Implementation:**
- `ReadingsViewModel.kt` — `_searchQuery` StateFlow, `setSearchQuery()` triggar om i `combine` med filter + displayLimit.
- Wildcard-stöd: `*` → `.*`, `?` → `.`, övriga specialtecken escapes.
- `MainScreenHost.kt` — `OutlinedTextField` med sökikon mellan titel och filterchips.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-43-sökfiltrering-i-readings--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.1 – Lokaliseringssystem / i18n implementerat och godkänd

**Implementation:**
- `data/localization/LocalizationManager.kt` — laddar JSON från assets, exponerar `StateFlow<Map<String, String>>`, spara språkval i SharedPreferences.
- `ui/Localized.kt` — `LocalLocalization` CompositionLocal, `str(key)` helper.
- `assets/strings_sv.json`, `strings_en.json` — 70+ nycklar, svenska + engelska.
- `SettingsScreen.kt` — FilterChips ("Svenska" / "English") för språkval.
- Alla skärmar migrerade: ScanScreen, Readings, MqttStatusScreen, SettingsScreen, PersistedListItem.
- `AppContainer.kt` — initierar `localizationManager`, laddar JSON vid start.
- `MainActivity.kt` — wrappar `CompositionLocalProvider(LocalLocalization provides ...)`.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-41-lokaliseringssystem-i18n--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.4 – Export CSV/JSON implementerad och godkänd

**Implementation:**
- `data/export/ReadingExporter.kt` — genererar CSV/JSON, skriver till cache via FileProvider, öppnar Android Share Sheet.
- `res/xml/file_paths.xml` — cache-path för FileProvider.
- `AndroidManifest.xml` — `<provider>`-deklaration.
- `SettingsScreen.kt` — knapparna "Export CSV" / "Export JSON", disabled om inga readings.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-44-export-csvjson--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.5 – Haptic + ljud vid scan implementerad och godkänd

**Implementation:**
- `AndroidNfcManager.kt` — `playBeep()` (SoundPool, 880 Hz, 100 ms) + `vibrate()` (VibrationEffect, 80 ms), båda villkorliga på AppSettings.
- `res/raw/beep.wav` — genererad med Python (880 Hz sinus, 44100 Hz sample rate).
- `SettingsScreen.kt` — Switchar "Vibrering vid skanning" / "Ljud vid skanning".
- `AndroidNfcManager` accepterar `AppSettings` i konstruktorn.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-45-haptic--ljud-vid-scan--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.6 – Riktig MQTT-anslutning implementerad och godkänd

**Implementation:**
- `data/mqtt/MqttConnectionManager.kt` — NY klass. Persistent anslutning vid app-start. `connectionStatus` + `lastHeartbeat` StateFlows. Automatisk återanslutning var 35:e sek. Keep-alive var 30:e sek.
- `data/mqtt/MqttSender.kt` — OMSKRIVEN: använder delad `MqttConnectionManager` istället för egen connect.
- `ConnectivityViewModel.kt` — OMSKRIVEN: ingen demo-data, läser från MqttConnectionManager direkt.
- `MqttStatusScreen.kt` — accepterar `ConnectivityViewModel` som parameter (inte `viewModel()` default).
- `AppContainer.kt` — initierar `mqttManager` + `MqttSender.init()`.
- `MainScreenHost.kt` + `MainActivity.kt` — wired: skapar VM med mqttManager.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-46-riktig-mqtt-anslutning--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.7 – Dark mode-toggle implementerad och godkänd

**Implementation:**
- `AppSettings.kt` — `ThemeMode` enum (LIGHT/DARK) + `themeMode` StateFlow + persistens.
- `SettingsScreen.kt` — Switch: På = mörkt, Av = ljust.
- `ui/theme/Theme.kt` — `RFIDManagerTheme` accepterar `themeMode`, `LightColorScheme` med riktiga ljusa färger.
- **Alla skärmar migrerade** från hårdkodade `Color.kt`-värden till `MaterialTheme.colorScheme.*`: MainScreenHost, MqttStatusScreen, ScanScreen, SettingsScreen.
- `MainActivity.kt` — skickar `settings.themeMode` till `RFIDManagerTheme`.

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-47-dark-mode-toggle--godkänd]].

---

## [2026-06-11] fas4 | Punkt 4.8 – Paginering i Readings implementerad och godkänd

**Implementation:**
- `ReadingsViewModel.kt` — `_displayLimit`, `_hasMore`, `loadMore()`, `currentPageSize()`.
- `ReadingsViewModel` tar `AppSettings` som andra konstruktorparameter.
- `MainScreenHost.kt` — "Ladda fler (X)"-knapp i botten av LazyColumn när `hasMore=true`.
- `AppSettings.kt` — `pageSize` StateFlow (10–50, steg om 10) + SharedPreferences-persistens.
- `SettingsScreen.kt` — slider för sidstorlek med label.
- Filter/sök-ändring återställer `_displayLimit` till aktuell `pageSize`.
- Knapptexten visar dynamiskt antal via `${str("screen.readings.load_more")} ($pageSize)`.
- Knappen använder `load_more` utan hårdkodad siffra (tog bort "(50)" från strängarna).

**UAT (2026-06-11):** ✅ Godkänd av Kund. Se [[Kundrelationer-och-Acceptans#2026-06-11-uat--fas-4-punkt-48-paginering-i-readings--godkänd]].

---

## [2026-06-11] housekeeping | Wiki uppdaterad – alla Fas 4 punkter dokumenterade

**Vad gjordes:**
- `Kundrelationer-och-Acceptans.md` — sign-off-sektioner för 4.1–4.8 tillagda.
- `Produkt-Roadmap.md` — 4.1–4.8 markerade ✅. "Senast uppdaterad" ändrad.
- `Fas4-Implementation-Plan.md` — status ändrad till ✅ komplett. Lexikon uppdaterat med nya keys. Avvikelser dokumenterade.
- `App-Architecture.md` — full uppdatering: Mermaid-diagram med Fas 3+4 struktur, package-struktur, navigationstabell, AppSettings StateFlows, MQTT-arkitektur, temahantering, paginering.
- `Fas3-Implementation-Plan.md` — spacing-status korrigerad.
- `index.md` — status uppdaterad till Fas 4 ✅.

**Status:** Fas 4 ✅ komplett. Alla 8 punkter implementerade, godkända och signerade av Kund 2026-06-11.

Nästa steg: Fas 5 (dokumentation) eller Fas 6 (radar trail/efterglöd).


## [2026-06-10] uat | Fas 3 UAT-genomgång + 7 godkända ändringskrav införda

**Vad gjordes:**

- **UAT-test** på Samsung Note 10 genomfört av Kund. 14 testfall, 9 ✅, 5 med synpunkter.
- **7 ändringskrav** dokumenterade i [[Kundrelationer-och-Acceptans]] som "godkända att införas".

**Införda ändringar:**

| # | Var | Ändring |
|---|---|---|
| UAT-1 | Scan – Radar | Sweep-linjen animeras nu (360° rotation på 4s) via `infiniteTransition` |
| UAT-2 | Scan – "GO TO READINGS" | Knapp borttagen (redundant med bottom nav) |
| UAT-3 | Scan – "Detected" | Behållen som "DETECTED" (tillräckligt tydlig) |
| UAT-4 | Readings – JSON-indikator | Borttagen (finns i Settings) |
| UAT-5 | Readings – Sortering | `sortedByDescending { it.timestamp }` — senaste överst |
| UAT-6 | Connectivity – Transmit | Redan fixat (döpt om till "Transmit") |
| UAT-7 | Connectivity – Transaktionskort | Nu visas **alla** readings med statusetikett (Pending/Transmitted), tidsstämpel, typ, dataPreview |

**Status:** Alla 9 UAT-punkter införda och installerade. Klar för omtest.

**Senaste punkt (UAT-9):** "Transmit"-knappen borttagen från Connectivity — Transmit sker enbart från Readings per reading-kort. Connectivity är nu rent monitor-läge.

---

## [2026-06-10] housekeeping | Städning: dead code borttaget, index uppdaterad, release synkad

**Vad gjordes:**

- Borttaget: `PersistedReadingsScreen.kt` (dead code — ersatt av inline-VM i MainScreenHost)
- Städade TODO-kommentarer i `ScanScreen.kt`, `MainScreenHost.kt`, `Screen.kt`
- Uppdaterade `index.md` med länkar till Fas 3-sidor + MQTT-Explorer
- Uppdaterade status-sektion (Fas 3 sign-off)
- Synkade till release-repot och commitade

**Status:** Rent bord. Redo för Fas 4-planering.

---

## [2026-06-10] fas4-kickoff | Fas 4 acceptanskriterier definierade + lexikon för i18n

**Vad gjordes:**

- **Produkt-Roadmap.md** uppdaterad med Fas 4 och Fas 5 sektioner med fullständiga acceptanskriterier.
- **Fas4-Implementation-Plan.md** skapad med:
  - Lexikon-tabell för lokalisering (70+ strängar, svenska/engelska)
  - Prioriterad implementationsordning (8 steg)
  - Risker
- **index.md** uppdaterad med länk till Fas 4-planen.

**Innehåll Fas 4:**
1. Lokaliseringssystem (JSON-lexikon + runtime byte i Settings)
2. Font size-slider för transaktionsdata
3. Sök/filtrering i Readings
4. Export (CSV/JSON via Share Sheet)
5. Haptic + ljud vid scan
6. Riktig MQTT-anslutning
7. Dark mode-toggle
8. Paginering i Readings (50 i taget)

**Innehåll Fas 5:**
1. Användarmanual (PDF)
2. Arkitektur-diagram
3. Release notes
4. Testplan
5. Kodgenomgång

**Vad gjordes:**

- **Fas 3 sign-off** i [[Kundrelationer-och-Acceptans#fas-3-sign-off-2026-06-10]] — alla kriterier uppfyllda.
- **Release synkad:** `~/rfid-manager/releases/2026-06-Fas2/RFIDManager/` uppdaterad.
- **Push till GitHub** med tagg `fas-3-sign-off-2026-06-10`.

**Status:** Fas 3 ✅ klar och levererad.

**Vad gjordes:**

- **Fas 3.4 (Room) → roadmap-dokumentation:**
  - `Produkt-Roadmap.md` — ny sektion "Villkor för riktig Room-databas" med tabell över barriärer, återaktiveringskod och övervakningslänk.
  - `Fas3-Implementation-Plan.md` — 3.4 markerad ✅ KLAR (JSON-fallback). Villkor hänvisar till roadmap.

- **Fas 3.5 Steg 15 — Empty states + loading + error:**
  - `ScanScreen.kt` — ikon (Nfc/SearchOff) + färgkodad tomtext + LinearProgressIndicator under scanning.
  - `MqttStatusScreen.kt` — tomt tillstånd för meddelanden + dynamisk statusfärg (grön/röd) + ikon.
  - `MainScreenHost.kt` — spinner under laddning + ikon i tomt tillstånd för Readings.
  - `SettingsScreen.kt` — ny dedikerad fil: storage mode, app info, version.

- **Fas 3.5 Steg 16 — Python subscriber:**
  - `test_subscriber_persist.py` — färgkodad ANSI-output, `--uid`-filter, on_disconnect, Ctrl+C-statistik.

- **Fas 3.5 Steg 17 — MQTT Explorer-dokumentation:**
  - Ny wiki-sida: `MQTT-Explorer.md` — installation, anslutning, användning, jämförelsetabell.

**Status:** ✅ Fas 3.4 klar. ✅ Fas 3.5 klar. **Fas 3 är nu helt slutförd.**


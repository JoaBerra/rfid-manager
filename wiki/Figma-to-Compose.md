# Figma → Compose

Denna sida beskriver hur vi har arbetat med att föra över design från Figma till Jetpack Compose i projektet **RFID Manager**.

## Bakgrund

Figma-designen för applikationen ("RFID Tag Management App") exporterades initialt som en React + Tailwind + shadcn/ui-prototyp. Denna export innehåller:

- En fullständig uppsättning UI-komponenter
- Flera färdiga skärmar (RFIDTagList, WriteForm, PresentationScreen)
- Design tokens i CSS (`--primary`, `--background`, `--radius`, etc.)
- Ett anpassat mörkt tema med neon-grön accent (`#00ff88`)

## Arbetsflöde

Eftersom det inte finns någon högkvalitativ, direkt export från Figma till Jetpack Compose (juni 2026), har vi valt följande pragmatiska approach:

### 1. Token-extraktion
- Extraherade färger, radie, typsnitt och övriga tokens från `src/styles/theme.css` och `default_shadcn_theme.css` i Figma-exporten.
- Skapade `Color.kt` och `Type.kt` som speglar designen (mörk terminal-estetik med monospace-inslag).

### 2. Tema i Compose
- Byggde ett `RFIDManagerTheme` baserat på Material 3 som tvingar mörkt läge och använder de extraherade värdena.
- Primär accent: `#00ff88` (neon green)
- Accent: `#f59e0b` (orange)
- Mycket tight `radius` (2 dp) enligt designen

### 3. Manuell implementation
- Skärmar och komponenter byggs manuellt i Compose.
- Figma-designen + React-prototypen används som detaljerad spec (layout, hierarki, interaktioner, tomma tillstånd).
- Exempel på komponenter som byggts:
  - `RFIDTagList` (med signal bars, timestamps, urval)
  - `RFIDManagerScreen` (dashboard med radar, statistikkort, split layout)
  - `WriteTagForm` (grundversion)

### 4. Radar & animationer
- Radar-visualiseringen i Figma implementerades som en enkel animerad `Canvas` med pulserande ringar.

## Verktyg och alternativ som övervägdes

| Verktyg              | Status                  | Kommentar |
|----------------------|-------------------------|---------|
| **Relay for Figma**  | Ej använt               | Googles officiella verktyg. Kräver att designern aktivt använder Relay i Figma. Kan ge bra Compose-kod men kräver förarbete. |
| **Figma Dev Mode**   | Används som referens    | Bra för att läsa ut exakta värden, men genererar ingen kod. |
| **Manuell översättning** | Primär metod       | Det som används i projektet. Ger bäst kontroll och kvalitet för mindre team. |
| **Tredjepartsverktyg** | Ej använda         | Ofta låg kvalitet på Compose-output. |

## Lärdomar

- Figma-exporter till React är mycket användbara som **spec och komponentbibliotek**, även om de inte går att använda direkt i Android.
- Att ha en tydlig "design token"-källa (CSS-variabler) gör det betydligt enklare att hålla Compose-temat synkat med Figma.
- Det lönar sig att tidigt skapa en `RFIDManagerTheme` som speglar designen – det sparar mycket tid senare.
- Radar och mer grafiska element går utmärkt att göra med `Canvas` i Compose utan att behöva custom views.

## Länkar

- [[App-Architecture]] – Övergripande arkitektur för appen
- [[Projektöversikt]] – Bakgrund till projektet

---

**Senast uppdaterad:** 2026-06-01

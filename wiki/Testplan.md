# Testplan — RFID Manager

> **Fas:** 5.4  
> **Senast uppdaterad:** 2026-06-13  
> **Testmiljö:** Samsung Galaxy Note 10 (SM-N970F), Android 12, MQTT broker 192.168.50.128:1883  
> **Testtyp:** Manuell

---

## Syfte och omfattning

Verifiera att alla vyer och funktioner i RFID Manager beter sig korrekt inför varje release.
Samtliga testfall körs manuellt på fysisk enhet med riktig NFC-tagg.

---

## Testmiljö

| Resurs | Detalj |
|---|---|
| Telefon | Samsung Galaxy Note 10 (SM-N970F/DS) |
| Android | 12 (One UI 4.1) |
| NFC | Aktiverat, enhetens egna kontroller (S.LSI 4.5.11) |
| MQTT-broker | `192.168.50.128:1883` (Docker eclipse-mosquitto) |
| Testtaggar | Minst 1 Mifare Ultralight / NTAG, minst 1 Mifare Classic 1K |
| Appversion | Senaste debug-build från Android Studio |
| Anslutning | USB ADB + MTP (filöverföring) |

---

## Testfall

### 1. Scan-vy

#### TC-SCAN-001 — Starta/stoppa skanning

| | |
|---|---|
| **Beskrivning** | Starta/stoppa skanning |
| **Förutsättningar** | NFC på, appen öppen på Scan-vyn |
| **Steg** | 1. Tryck "STARTA SKANNING"<br>2. Tryck "STOPPA SKANNING" |
| **Förväntat resultat** | Knappen växlar mellan "STARTA SKANNING" (vid stopp) och "STOPPA SKANNING" (vid scanning). Radarn roterar under scanning. Texten "SKANNING AKTIV" visas. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SCAN-002 — Detektera NFC-tagg under scanning

| | |
|---|---|
| **Beskrivning** | Detektera NFC-tagg under scanning |
| **Förutsättningar** | Scanning aktiv, NFC-tagg inom räckvidd |
| **Steg** | 1. Håll NFC-tagg mot telefonens baksida<br>2. Vänta på detektion |
| **Förväntat resultat** | Vibration (80ms) + ljudsignal (880Hz). Taggen visas i listan med UID och typ. Radar-tag-svans syns medsols. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SCAN-003 — Radar-sweep trail syns under scanning

| | |
|---|---|
| **Beskrivning** | Radar-sweep trail syns under scanning |
| **Förutsättningar** | Scanning aktiv |
| **Steg** | 1. Observera radarns efterglöd |
| **Förväntat resultat** | 72° wedge (72 segment med linjär alpha-fade) roterar medsols bakom sweep-line. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SCAN-004 — Detektera tagg utan scanning (background NDEF)

| | |
|---|---|
| **Beskrivning** | Detektera tagg utan scanning (background NDEF) |
| **Förutsättningar** | NFC på, app i bakgrunden eller på annan vy |
| **Steg** | 1. Håll NFC-tagg mot telefonen |
| **Förväntat resultat** | Appen kan fortfarande ta emot taggen via onNewIntent (system chooser kan visas vid första tillfället). |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Reader mode alltid aktiv. Taggar detekteras även utan aktiv scanning. Write-flödet fungerar parallellt.

---

#### TC-SCAN-005 — Spara detekterad tagg (Persist)

| | |
|---|---|
| **Beskrivning** | Spara detekterad tagg (Persist) |
| **Förutsättningar** | Minst en tagg detekterad i listan |
| **Steg** | 1. Tryck på "Spara" / Persist-knappen för en detekterad tagg |
| **Förväntat resultat** | Taggen sparas till Readings. Bekräftelse ges (snackbar/toast). |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SCAN-006 — Skriva till tagg (Write)

| | |
|---|---|
| **Beskrivning** | Skriva till tagg (Write) |
| **Förutsättningar** | Tagg detekterad, skrivbar (inte låst), inom räckvidd |
| **Steg** | 1. Välj tagg i listan<br>2. Gå till Write-fliken<br>3. Välj target page/block<br>4. Ange data<br>5. Tryck "Armed" (gul indikator)<br>6. Håll taggen mot telefonen |
| **Förväntat resultat** | Data skrivs till taggens angivna page/block. Bekräftelse visas. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Write-flödet fungerar. Välj tagg, fyll i adress + hex-data, tryck SPARA, håll taggen mot telefonen. Data skrivs korrekt.

---

#### TC-SCAN-007 — Låst tagg (Lock bytes) hanteras korrekt

| | |
|---|---|
| **Beskrivning** | Låst tagg (Lock bytes) hanteras korrekt |
| **Förutsättningar** | Ultralight-tagg med låsta sektorer |
| **Steg** | 1. Försök skriva till skrivskyddad page |
| **Förväntat resultat** | Transceive failed-fel visas. Appen kraschar inte. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Minneskarta i write-formuläret. Grön/röd prick per block. Adressfältet visar låsstatus i realtid.

---

#### TC-READ-003 — Sök i readings

| | |
|---|---|
| **Beskrivning** | Sök i readings |
| **Förutsättningar** | Flera readings sparade |
| **Steg** | 1. Skriv i sökfältet (UID eller annan text)<br>2. Använd wildcards `*` och `?` |
| **Förväntat resultat** | Listan filtreras i realtid. Wildcards fungerar. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-READ-004 — Filter-chips (All / RFID / EAN)

| | |
|---|---|
| **Beskrivning** | Filter-chips (All / RFID / EAN) |
| **Förutsättningar** | Blandade RFID + EAN readings |
| **Steg** | 1. Tryck på respektive chip<br>2. Kombinera med sökfält |
| **Förväntat resultat** | Listan filtreras per typ. Chips + sök kan kombineras. |

- [ ] `Godkänt`
- [x] `Ej godkänt`

Resultat:
> Det här är inget giltigt test fall ännu eftersom vi inte har implementerat EAN kod scanning.

---

#### TC-READ-005 — Paginering — "Ladda fler"

| | |
|---|---|
| **Beskrivning** | Paginering — "Ladda fler" |
| **Förutsättningar** | Fler readings än sidstorleken (t.ex. 11 st med page size 10) |
| **Steg** | 1. Scrolla till botten av listan<br>2. Tryck "Ladda fler" |
| **Förväntat resultat** | Nästa sida läses in. Knappen visas endast om fler resultat finns. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-READ-006 — Transmit reading till MQTT

| | |
|---|---|
| **Beskrivning** | Transmit reading till MQTT |
| **Förutsättningar** | MQTT-broker igång, enhet ansluten |
| **Steg** | 1. Tryck "Transmit" på en reading |
| **Förväntat resultat** | Status ändras till "Transmitted" (eller "Pending" om broker ej nås). Payload skickas till `192.168.50.128:1883`. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Transmit från Readings → "✓ Skickad". Syns i Connectivity med status "Skickad".

---

#### TC-READ-007 — Sortering — nyast först

| | |
|---|---|
| **Beskrivning** | Sortering — nyast först |
| **Förutsättningar** | Flera readings med olika tider |
| **Steg** | 1. Observera listans ordning |
| **Förväntat resultat** | Nyaste reading överst (fallande tidsstämpel). |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

### 3. Connectivity-vy

#### TC-CONN-001 — Anslutningsstatus visas

| | |
|---|---|
| **Beskrivning** | Anslutningsstatus visas |
| **Förutsättningar** | MQTT-broker körs |
| **Steg** | 1. Navigera till Connectivity-vyn |
| **Förväntat resultat** | Grön "CONNECTED"-badge visas (eller röd "DISCONNECTED" om broker ej nås). Heartbeat-tidstämpel visas. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Telefonen visar "SPARKPLUG CONNECTED ✓". Badge i primary-färg.

---

#### TC-CONN-002 — Meddelandehistorik visas

| | |
|---|---|
| **Beskrivning** | Meddelandehistorik visas |
| **Förutsättningar** | Minst en reading transmitterad |
| **Steg** | 1. Observera meddelandelistan |
| **Förväntat resultat** | Varje meddelande visar UID, tidstämpel, status (Pending/Transmitted) och data-preview. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Reading syns med UID, tidstämpel, status "Skickad" / "Väntar" och data-preview.

---

#### TC-CONN-003 — Detaljvy för meddelande

| | |
|---|---|
| **Beskrivning** | Detaljvy för meddelande |
| **Förutsättningar** | Minst ett meddelande i listan |
| **Steg** | 1. Tryck på ett meddelande |
| **Förväntat resultat** | AlertDialog visas med full metadata + hela JSON-payloden. Dialog kan stängas med OK. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> AlertDialog visas vid tryck på reading. Visar UID, typ, tidsstämpel, JSON-payload. Stäng med Stäng.

---

#### TC-CONN-004 — Auto-återanslutning vid förlorad anslutning

| | |
|---|---|
| **Beskrivning** | Auto-återanslutning vid förlorad anslutning |
| **Förutsättningar** | Ansluten till broker |
| **Steg** | 1. Stäng av MQTT-broker (t.ex. docker stop)<br>2. Vänta 35 sekunder<br>3. Starta MQTT-broker igen<br>4. Vänta |
| **Förväntat resultat** | Status går till "DISCONNECTED" vid frånkoppling. Återansluter automatiskt när broker är tillgänglig igen. |

- [ ] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-CONN-005 — Keep-alive (30s intervall)

| | |
|---|---|
| **Beskrivning** | Keep-alive (30s intervall) |
| **Förutsättningar** | Ansluten till broker |
| **Steg** | 1. Observera heartbeat-tidstämpeln<br>2. Vänta 30 sekunder |
| **Förväntat resultat** | Heartbeat uppdateras var 30:e sekund. |

- [ ] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

### 4. Settings-vy

#### TC-SETT-001 — Språkväxlare (Svenska / English)

| | |
|---|---|
| **Beskrivning** | Språkväxlare (Svenska / English) |
| **Förutsättningar** | Appen öppen |
| **Steg** | 1. Välj "English" i språkväljaren<br>2. Navigera mellan vyer<br>3. Byt tillbaka till "Svenska" |
| **Förväntat resultat** | Alla UI-strängar växlar omedelbart. Inget omstart krävs. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-002 — Fontstorlek-slider

| | |
|---|---|
| **Beskrivning** | Fontstorlek-slider |
| **Förutsättningar** | Appen öppen |
| **Steg** | 1. Dra slidern från 1.0x → 1.8x<br>2. Navigera till Scan och Readings |
| **Förväntat resultat** | Textstorleken i listor och etiketter ändras i realtid. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-003 — Dynamisk layout vid stor text (1.3×-tröskel)

| | |
|---|---|
| **Beskrivning** | Dynamisk layout vid stor text (1.3×-tröskel) |
| **Förutsättningar** | Readings med data |
| **Steg** | 1. Sätt fontstorlek ≤ 1.2× — observera layout<br>2. Sätt fontstorlek ≥ 1.3× — observera layout |
| **Förväntat resultat** | Kort övergår till vertikal stapling vid ≥ 1.3×. MaxLines truncation aktiveras för lång text. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-004 — Dark mode-toggle

| | |
|---|---|
| **Beskrivning** | Dark mode-toggle |
| **Förutsättningar** | Appen öppen |
| **Steg** | 1. Slå på Dark mode<br>2. Navigera alla vyer<br>3. Slå av Dark mode |
| **Förväntat resultat** | Temat växlar omedelbart mellan LIGHT och DARK. Alla komponenter syns korrekt i båda lägena. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Light mode: primary bytt till British Racing Green, onSurfaceVariant till mörkgrön. All text läsbar.

---

#### TC-SETT-005 — Haptic toggle

| | |
|---|---|
| **Beskrivning** | Haptic toggle |
| **Förutsättningar** | Scan-vy, NFC-tagg tillgänglig |
| **Steg** | 1. Stäng av haptik i Settings<br>2. Scanna en tagg<br>3. Slå på haptik<br>4. Scanna en tagg |
| **Förväntat resultat** | Utan haptik: ingen vibration (men ljud fortfarande). Med haptik: vibration 80ms. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-006 — Sound toggle

| | |
|---|---|
| **Beskrivning** | Sound toggle |
| **Förutsättningar** | Scan-vy, NFC-tagg tillgänglig |
| **Steg** | 1. Stäng av ljud i Settings<br>2. Scanna en tagg<br>3. Slå på ljud<br>4. Scanna en tagg |
| **Förväntat resultat** | Utan ljud: inget pip. Med ljud: 880Hz, 100ms. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-007 — Page size-slider (paginering)

| | |
|---|---|
| **Beskrivning** | Page size-slider (paginering) |
| **Förutsättningar** | Flera readings sparade |
| **Steg** | 1. Ändra page size (10–50)<br>2. Gå till Readings och använd "Ladda fler" |
| **Förväntat resultat** | Sidstorleken respekteras i pagineringen. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-008 — Export CSV

| | |
|---|---|
| **Beskrivning** | Export CSV |
| **Förutsättningar** | Minst 1 reading sparad |
| **Steg** | 1. Tryck "Exportera CSV" |
| **Förväntat resultat** | CSV-fil genereras. Android Share Sheet öppnas. Innehållet är korrekt formaterad CSV. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-009 — Export JSON

| | |
|---|---|
| **Beskrivning** | Export JSON |
| **Förutsättningar** | Minst 1 reading sparad |
| **Steg** | 1. Tryck "Exportera JSON" |
| **Förväntat resultat** | JSON-fil genereras. Android Share Sheet öppnas. Innehållet är korrekt formaterad JSON. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-SETT-010 — Export-knappar inaktiva när inga readings finns

| | |
|---|---|
| **Beskrivning** | Export-knappar inaktiva när inga readings finns |
| **Förutsättningar** | Inga sparade readings |
| **Steg** | 1. Observera CSV/JSON-knapparna |
| **Förväntat resultat** | Båda knapparna är disabled (grå). |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Rensade readings via "Rensa alla". Båda export-knapparna disabled (grå).
FIxat, knapparna är disablade när listan av läsningar är tömd.
---

#### TC-SETT-011 — Versionsinformation visas

| | |
|---|---|
| **Beskrivning** | Versionsinformation visas |
| **Förutsättningar** | Appen installerad |
| **Steg** | 1. Scrolla längst ner i Settings |
| **Förväntat resultat** | Version/build-nummer visas. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

### 5. Navigation och övergripande

#### TC-NAV-001 — Navigering mellan alla vyer

| | |
|---|---|
| **Beskrivning** | Navigering mellan alla vyer |
| **Förutsättningar** | Appen öppen |
| **Steg** | 1. Tryck på varje navigeringsikon (Scan → Readings → Connectivity → Settings)<br>2. Växla fram och tillbaka |
| **Förväntat resultat** | Varje vy visas korrekt. Tillstånd bevaras per vy (t.ex. scanning fortsätter). Navigation Bar highlight följer aktiv vy. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-NAV-002 — Datapersistens mellan omstarter

| | |
|---|---|
| **Beskrivning** | Datapersistens mellan omstarter |
| **Förutsättningar** | Minst 1 reading sparad |
| **Steg** | 1. Stäng appen helt (swipe away)<br>2. Öppna appen igen |
| **Förväntat resultat** | Sparade readings finns kvar (JSON-fallback). |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-NAV-003 — Mörkt tema — alla vyer

| | |
|---|---|
| **Beskrivning** | Mörkt tema — alla vyer |
| **Förutsättningar** | Dark mode aktiverat |
| **Steg** | 1. Navigera till alla vyer i mörkt läge |
| **Förväntat resultat** | All text, bakgrunder, kort, knappar, filterchips, dialoger syns korrekt. Ingen oläsbar text. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:


---

#### TC-NAV-004 — Ljust tema — alla vyer

| | |
|---|---|
| **Beskrivning** | Ljust tema — alla vyer |
| **Förutsättningar** | Light mode aktiverat |
| **Steg** | 1. Navigera till alla vyer i ljust läge |
| **Förväntat resultat** | All text, bakgrunder, kort, knappar, filterchips, dialoger syns korrekt. Ingen oläsbar text. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Light mode: primary British Racing Green, onSurfaceVariant mörkgrön. All text läsbar.

---

### 6. End-to-end flöden

#### TC-E2E-001 — Fullt flöde: Scanna → Spara → Transmittera

| | |
|---|---|
| **Beskrivning** | Fullt flöde: Scanna → Spara → Transmittera |
| **Förutsättningar** | NFC-tagg, MQTT-broker igång |
| **Steg** | 1. Starta scan, scanna tagg<br>2. Spara till Readings<br>3. Gå till Readings, hitta sparade taggen<br>4. Tryck "Transmit"<br>5. Gå till Connectivity, verifiera meddelandet |
| **Förväntat resultat** | Tagg detekteras → sparas → syns i Readings → transmitteras → syns i Connectivity med status "Transmitted". |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Scanna → Spara → Transmit → "✓ Skickad". Syns i Connectivity med status "Skickad".

---

#### TC-E2E-002 — Export efter fullt flöde

| | |
|---|---|
| **Beskrivning** | Export efter fullt flöde |
| **Förutsättningar** | Readings finns sparade |
| **Steg** | 1. Gå till Settings<br>2. Exportera CSV och JSON |
| **Förväntat resultat** | Filer genereras med korrekt data från sparade readings. Share Sheet öppnas. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> 

---

#### TC-E2E-003 — Skriv till tagg och verifiera

| | |
|---|---|
| **Beskrivning** | Skriv till tagg och verifiera |
| **Förutsättningar** | Skrivbar tagg (Ultralight eller Classic) |
| **Steg** | 1. Scanna tagg<br>2. Skriv data till valfri page/block<br>3. Ta bort taggen<br>4. Scanna samma tagg igen |
| **Förväntat resultat** | Skriven data syns i hex-dump på den angivna adressen. |

- [x] `Godkänt`
- [ ] `Ej godkänt`

Resultat:
> Write-flödet fungerar. Data syns i hex-dump efter omscanning.

---

## Pre-release checklista (Fas 6.5)

Inför varje release (1.0, patch, etc.):

- [ ] Alla TC-SCAN-* testfall körda och godkända
- [ ] Alla TC-READ-* testfall körda och godkända
- [ ] Alla TC-CONN-* testfall körda och godkända
- [ ] Alla TC-SETT-* testfall körda och godkända
- [ ] Alla TC-NAV-* testfall körda och godkända
- [ ] Alla TC-E2E-* testfall körda och godkända
- [ ] Inga blockerande buggar i [[bugs/README]]
- [ ] Appen bygger utan varningar (green build)
- [ ] Version/build bump i gradle
- [ ] Release notes uppdaterade ([[Produkt-Roadmap#53-release-notes|5.3]])

---

## Länkar

- [[Produkt-Roadmap#54-testplan|Produkt-Roadmap — 5.4 Testplan]]
- [[Kundrelationer-och-Acceptans]]
- [[bugs/README]]
- [[Kanban]]

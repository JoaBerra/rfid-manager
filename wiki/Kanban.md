---

kanban-plugin: board

---

## 🔜 Att göra (To Do)

- [ ] **Fas 6 — 1.0 Release**
	- [ ] **6.3** Release build-setup (signing, proguard)
	- [ ] **6.4** Borttagning av stub-kod
	- [ ] **6.5** End-to-end test
	- [ ] **6.6** Version 1.0 + GitHub Release
	- [ ] **Write-förbättringar** (önskemål från test) — under Fas 6
		- [x] Visa låsta/skrivbara block i write-vyn
		- [ ] Hex-checkbox — ASCII auto-konverteras till hex
		- [ ] Fullskärms-editor för write-data

## 🔄 Pågår (In Progress)








## ✅ Klart (Done)

- [x] **BUG-002** | Write-funktion borta | TC-SCAN-006, TC-E2E-003 | ✅ Godkänt 2026-06-13
- [x] **BUG-003** | Background NDEF | TC-SCAN-004 | ✅ Godkänt 2026-06-13
- [x] **BUG-004** | Light mode — svaga texter | TC-SETT-004, TC-NAV-004 | ✅ Godkänt 2026-06-13
- [x] **BUG-005** | Rensa readings-kommando | TC-READ-002, TC-SETT-010 | ✅ Godkänt 2026-06-13
- [x] **BUG-006** | Testbeskrivning stämde ej | TC-SCAN-001 | ✅ Godkänt 2026-06-13
- [x] **BUG-007** | Låst tagg-gränssnitt | TC-SCAN-007 | ✅ Godkänt 2026-06-13
- [x] **BUG-008** | MQTT-miljö-dokumentation | TC-READ-006, hela Connectivity | ✅ Godkänt 2026-06-13
- [x] **5.3** Release notes — dokument per fas + GitHub Releases
- [x] **5.4** Testplan — 22 testfall, alla Godkänt
- [x] **6.1** MQTT-broker-konfiguration i UI — ✅ Godkänt 2026-06-13
- [x] **6.2** App-ikon — ✅ Godkänt 2026-06-13
- [x] **5.2** Arkitektur-diagram — uppdatera App-Architecture med Fas 3+4+5
- [x] **5.1** Användarmanual (PDF med skärmbilder, svenska)
- [x] **[[bugs/2026-06-13-connectivity-message-detail|BUG-001]]** — Connectivity: detaljvy visas inte vid tryck på meddelande
- [x] **5.7** Radar sweep trail/efterglöd — drawArc 72° wedge, 72 segment, tag-svansar medsols
- [x] **5.5** Kodgenomgång — demo-etiketter, duplicerat build-block, fas-referenser, hårdkodade färger, deprecation fix
- [x] **5.6** Dynamisk layout — kort skalas, vertikal stapling vid stor text, overflow-skydd
- [x] **Fas 4.1** — Lokaliseringssystem (i18n)
- [x] **Fas 4.2** — Font size-slider
- [x] **Fas 4.3** — Sök/filtrering i Readings
- [x] **Fas 4.4** — Export CSV/JSON
- [x] **Fas 4.5** — Haptic + ljud vid scan
- [x] **Fas 4.6** — Riktig MQTT-anslutning
- [x] **Fas 4.7** — Dark mode-toggle
- [x] **Fas 4.8** — Paginering i Readings
- [x] Fas 4 pushad till GitHub
- [x] Fas 3 — Navigation, ViewModels, spacing, polish
- [x] Fas 2 — MQTT, persistens, end-to-end
- [x] Fas 1 — NFC-läsning/skrivning, UI, radar
- [x] Android Studio + Omarchy-setup
- [x] Wiki-struktur (Karpathy pattern)
- [x] GitHub-repo skapat

## ❌ Blockerat

- [ ] Riktig Room-databas — inväntar KSP med AGP 9-stöd

## 📝 Anteckningar

- **Flöde:** Roadmap ([[Produkt-Roadmap]]) → långsiktig plan. Kanban → veckovis taktik. När en fas påbörjas bryter vi ner acceptanskriterierna från roadmap till Kanban-kort.
- **Arbetssätt:** När AI-assistenten påbörjar en punkt flyttas den från Att göra → Pågår. När Kund godkänt (sign-off i [[Kundrelationer-och-Acceptans]]) flyttas den till Klart. Aldrig klarmarkerad före Kund-godkännande.
- **Format (buggar):** `ID | Vad | Testfall | Status`
- **Länkar:** Skriv `[[Sida]]` på kort för att koppla task till wiki-dokumentation.
- **Uppdateras:** AI-assistenten håller boarden aktuell.



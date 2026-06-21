---
kanban-plugin: board
---

## 🔜 Att göra (To Do)



- [ ] **Fas-101 — MQTT-klientkonfiguration i appen** (settings för host, port, TLS, auth, Sparkplug)
	- [x] Wiki-sida skapad: [[Fas-101-MQTT-Configuration]]
	- [ ] **1. Anslutning** — host, port, client ID
	- [ ] **2. Autentisering** — username, password
	- [ ] **3. TLS/SSL** — certifikat, kryptering
	- [ ] **4. Sparkplug** — Group ID, Node ID, Device ID
	- [ ] **5. Topics** — base, telemetry, command
	- [ ] **6. Beteende** — QoS, retain, keep-alive, reconnect, will message
	- [ ] **7. Verifiering** — testknapp, statusindikator

- [ ] **Fas-100 — MQTT-infrastruktur och nätverkslandskap** (fördjupning)
	- [x] Wiki-sida skapad: [[MQTT-Infrastruktur]]
	- [x] **1. MQTT-protokollet — grunderna**
		- [x] Utbildning: Förstå publish/subscribe, broker, topic, wildcards
		- [x] Konfiguration: Identifiera relevanta protokollinställningar
		- [x] Dokumentation: Sammanfatta MQTT-grunder i egna ord
		- [x] Test: Verifiera förståelse med praktiskt exempel
	- [x] **2. Vår broker: Eclipse Mosquitto**
		- [x] Utbildning: Förstå Mosquitto, docker run-kommandot, konfig
		- [x] Konfiguration: Gå igenom mosquitto.conf, testa ändra parameter
		- [x] Dokumentation: Skapa en "broker quick reference"
		- [x] Test: Starta/stoppa brokern, verifiera med mosquitto_pub/sub
	- [x] **3. Topologi och nätverk**
		- [x] Utbildning: Förstå nätverksflödet app → broker → subscriber
		- [x] Konfiguration: Kartlägga portar, IP, WiFi, brandvägg
		- [x] Dokumentation: Uppdatera nätverksdiagram med korrekta adresser
		- [x] Test: Verifiera anslutning från alla enheter i nätverket
	- [ ] **4. Topics**
		- [ ] Utbildning: Förstå topic-struktur, wildcards, hierarki
		- [ ] Konfiguration: Se över topic-struktur för framtida behov
		- [ ] Dokumentation: Beskriv topic-trädet med exempel
		- [ ] Test: Prenumerera på olika topics, verifiera matchning
	- [ ] **5. Meddelandeformat**
		- [ ] Utbildning: Förstå JSON-payload, Sparkplug B-relation
		- [ ] Konfiguration: Se över om fler fält behövs i payload
		- [ ] Dokumentation: Skapa ett JSON-schema för meddelandena
		- [ ] Test: Publicera och tolka meddelanden med olika verktyg
	- [ ] **6. Kvalitet på leverans (QoS)**
		- [ ] Utbildning: Förstå QoS 0/1/2, cleanSession, konsekvenser
		- [ ] Konfiguration: Utvärdera om QoS 1 är rätt nivå
		- [ ] Dokumentation: Beskriv QoS-beslut och trade-offs
		- [ ] Test: Publicera med olika QoS, mät beteende
	- [ ] **7. Återanslutning och keep-alive**
		- [ ] Utbildning: Förstå keep-alive, reconnect-loop, timeouts
		- [ ] Konfiguration: Se över keepAliveInterval, reconnect-strategi
		- [ ] Dokumentation: Beskriv nuvarande reconnect-beteende
		- [ ] Test: Stäng av broker, mät återanslutningstid
	- [ ] **8. Säkerhet**
		- [ ] Utbildning: Förstå risker med cleartext, anonym auth, TLS
		- [ ] Konfiguration: Utvärdera om TLS ska läggas till
		- [ ] Dokumentation: Dokumentera säkerhetsläge och rekommendationer
		- [ ] Test: Säkerhetsgenomgång av nuvarande setup
	- [ ] **9. Verktyg**
		- [ ] Utbildning: Prova MQTT Explorer, mosquitto_sub, mosquitto_pub
		- [ ] Konfiguration: Notera anslutningsinställningar per verktyg
		- [ ] Dokumentation: Skapa "lathund" för varje verktyg
		- [ ] Test: Använd varje verktyg för att publicera/lyssna
	- [ ] **10. Flöde: från scan till mottaget meddelande**
		- [ ] Utbildning: Följ hela kedjan steg för steg
		- [ ] Konfiguration: Identifiera flaskhalsar eller förbättringar
		- [ ] Dokumentation: Uppdatera dokumentation med korrekt flöde
		- [ ] Test: End-to-end test enligt flödesbeskrivningen
	- [ ] **11. Kända begränsningar och framtida förbättringar**
		- [ ] Utbildning: Förstå vad som saknas och varför
		- [ ] Konfiguration: Prioritera vilka förbättringar som är viktigast
		- [ ] Dokumentation: Uppdatera begränsningar i roadmap
		- [ ] Test: Verifiera att kända begränsningar fortfarande gäller

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
- [x] **6.3** Release build-setup (signing, proguard) — ✅ 2026-06-13
- [x] **6.4** Borttagning av stub-kod — ✅ 2026-06-13
- [x] **6.5** End-to-end test (enligt testplan 5.4) — ✅ redan godkänd
- [x] **6.6** Version 1.0 + GitHub Release — ✅ skapat: https://github.com/JoaBerra/rfid-manager-android/releases/tag/v1.0
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
- [x] **Fas 200 — MQTT Realtidsdashboard** — FastAPI/SSE, Docker Compose, live-flöde + statistik ([[Fas-200-Web-Dashboard]]) — ✅ Godkänt av Kund 2026-06-19
- [x] **Fas-500 — Miljöflytt sixten → falstaff** — Docker, .venv, IP/sökvägsuppdateringar, Android Studio, ADB, MCP-konfig, E2E-verifiering ([[Fas-500-Miljo-flytt-sixten-till-falstaff]]) — ✅ Godkänt av Kund 2026-06-21
- [x] **Fas-400 — Teknikmiljö validering** — Komponentkarta, bootstrap-script, verifieringsrutiner, dokumentation ([[Fas-400-Teknikmiljo-Validering]], [[Kanban]]) — ✅ 2026-06-21

## 📝 Anteckningar

- **Flöde:** Roadmap ([[Produkt-Roadmap]]) → långsiktig plan. Kanban → veckovis taktik. När en fas påbörjas bryter vi ner acceptanskriterierna från roadmap till Kanban-kort.
- **Arbetssätt:** När AI-assistenten påbörjar en punkt flyttas den från Att göra → Pågår. När Kund godkänt (sign-off i [[Kundrelationer-och-Acceptans]]) flyttas den till Klart. Aldrig klarmarkerad före Kund-godkännande.
- **Format (buggar):** `ID | Vad | Testfall | Status`
- **Länkar:** Skriv `[[Sida]]` på kort för att koppla task till wiki-dokumentation.
- **Uppdateras:** AI-assistenten håller boarden aktuell.



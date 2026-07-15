# RFID Manager

**Industriell Android-app för RFID/eskortminne + MQTT/IoT**

| Fält | Värde |
|------|-------|
| **Projektstatus** | **Pausat** (återöppningsbart) — avslutat 2026-07-14 |
| **Senaste release** | [v1.0.1](https://github.com/JoaBerra/rfid-manager/releases/tag/v1.0.1) |
| **Dev-host** | fakir (Arch/Omarchy) |
| **Testmiljö** | ishtar `192.168.50.151` (MQTT + dashboard på hulda/Proxmox) |
| **Organisation** | [Andra Hjärnan](https://github.com/JoaBerra/andra-hjarna) — projektkunskap och uppdrag |

Projektet är **avslutat i nuvarande fas**: appen fungerar mot testlabbet, Fas D är komplett, och dokumentation är synkad. Ingen aktiv utveckling planerad tills någon **återöppnar** projektet (se nedan).

---

## Vad som levererats

- Android-app (Kotlin, Jetpack Compose) — NFC läs/skriv, persistens, MQTT
- Default broker → **ishtar** `192.168.50.151:1883` (Uppdrag 004, v1.0.1)
- Testmiljö Fas D: Proxmox hulda + gäst ishtar, dashboard `:8000`
- Dev-baseline på **fakir**: Android SDK 36, git/gh, MQTT Explorer, wiki
- Uppdrag 001–005 godkända (onboarding → falstaff-avveckling)
- GitHub Release med debug-APK

---

## Struktur (repo)

| Sökväg | Innehåll |
|--------|----------|
| `RFIDManager/` | Android-projekt (Gradle) — **huvudkällkod** |
| `wiki/` | Projektwiki (Obsidian) — arkitektur, test, miljö |
| `test/fas2-mqtt/` | Mosquitto, subscriber, simulatorer (för ishtar) |
| `dashboard/` | Webb-dashboard (Docker) |
| `mcp-server/` | MCP för AI (parkerad — se AH idé) |
| `setup/` | Installationsskript (fakir, ishtar, SSH, m.m.) |
| `manual/` | Användarmanual |

Historiska referenser till `RFIDManager-android/`, `llm-wiki/` och separat GitHub-repo är **inaktuella** — allt ligger i detta monorepo under `RFIDManager/` och `wiki/`.

---

## Snabbstart (återuppta drift)

### 1. Bygg och installera app (fakir)

```bash
export ANDROID_HOME=~/Android/Sdk
cd RFIDManager
./gradlew assembleDebug
./gradlew installDebug    # Note 10 via USB
```

APK: `RFIDManager/app/build/outputs/apk/debug/app-debug.apk`  
Eller ladda från [GitHub Release v1.0.1](https://github.com/JoaBerra/rfid-manager/releases/tag/v1.0.1).

### 2. Testmiljö (ishtar)

```bash
ssh ishtar
# Se wiki/Testmiljo-hulda.md och setup/ishtar-start-mqtt.sh
```

- MQTT: `192.168.50.151:1883`
- Dashboard: `http://192.168.50.151:8000`
- MQTT Explorer på fakir: `mqtt-explorer`

### 3. Dokumentation

| Ämne | Fil |
|------|-----|
| Dev-miljö fakir | `wiki/Utvecklingsmiljö-fakir.md` |
| Testlabb | `wiki/Testmiljo-hulda.md` |
| Kanban / backlog | `wiki/Kanban.md` |
| Release notes | `wiki/Release-Notes.md` |
| UAT | `wiki/UAT-fakir-smoke-test.md` |

Diagnostik GitHub: `bash setup/github-fakir.sh`

---

## Kvar att göra (backlog vid återöppning)

Prioritera från `wiki/Kanban.md`. Sammanfattning:

### Hög prioritet — produkt

| ID | Beskrivning | Referens |
|----|-------------|----------|
| **Fas-101** | Full MQTT-konfig i appen (TLS, auth, Sparkplug, topics, QoS, testknapp) | `wiki/Fas-101-MQTT-Configuration.md` |
| **UAT NFC** | NFC-scan/write inte körd i senaste smoke — verifiera på Note 10 | `wiki/UAT-fakir-smoke-test.md` |
| **Release v1.0.2** | Ev. ny APK efter `network_security_config` (endast `.151`) | `wiki/Release-Notes.md` |

### Medel prioritet

| ID | Beskrivning |
|----|-------------|
| **Fas-100** | MQTT-fördjupning wiki (topics, QoS, säkerhet, E2E) — punkter 4–11 i Kanban |
| **Write-förbättringar** | Hex-checkbox; fullskärms-editor för write-data |
| **Connectivity UI** | Visa broker-IP på Connectivity-skärmen (planerat under Fas-101) |

### Låg prioritet / parkerat

| ID | Beskrivning |
|----|-------------|
| **MCP-server** | Parkerad idé i AH — `mcp-server/`, dashboard + Explorer räcker idag |
| **Room/KSP** | Riktig Room-databas blockerad av KSP/AGP — in-memory + JSON idag |
| **hemmanatverk** | Uppdatera nätverksdiagram (falstaff avvecklad) |

### Teknisk skuld

- `README` var inaktuell före 2026-07-14 (rättad i samband med avslut)
- README i root pekade på icke-existerande `rfid-manager-android`-repo

---

## Återöppna projektet

1. **Läs** denna README och `wiki/Kanban.md`
2. **Verifiera miljö:** `wiki/Utvecklingsmiljö-fakir.md`, `ssh ishtar`, broker svarar
3. **Andra Hjärnan:** `Kunskapsbas/Projekt/rfid-manager.md` + skapa nytt uppdrag (t.ex. Uppdrag 006) med mål och kriterier
4. **Välj backlog-punkt** — rekommenderat start: Fas-101 eller UAT NFC
5. **Uppdatera status** i README (tabellen överst) och AH från *Pausat* till *Pågående*

---

## Länkar

- **GitHub:** https://github.com/JoaBerra/rfid-manager
- **Andra Hjärnan:** https://github.com/JoaBerra/andra-hjarna
- **Domän (AH):** `Kunskapsbas/Amnen/rfid-adc/`

Utvecklat med AI-assistans (Grok, Qwen) på Arch Linux / Omarchy (fakir).
# Fas-500 — Miljöflytt: sixten → falstaff

**Datum:** 2026-06-21  
**Status:** Planerad

## Bakgrund

Utvecklingsmiljön har flyttats från hosten **sixten** till **falstaff**. Projektets tre huvudkataloger har samlats under `~/projects/rfid/`:

| Repo | Gammal sökväg (sixten) | Ny sökväg (falstaff) |
|---|---|---|
| `llm-wiki` | `/home/joakim/projects/rfid/llm-wiki/` | `~/projects/rfid/llm-wiki/` |
| `rfid-manager` | `/home/joakim/projects/rfid/rfid-manager/` | `~/projects/rfid/rfid-manager/` |
| `rfid-manager-android` | `/home/joakim/projects/rfid/rfid-manager-android/` | `~/projects/rfid/rfid-manager-android/` |

---

## 1. Hårdkodade sökvägar som måste uppdateras

### 1.1 Mönster: `~/projects/rfid/rfid-manager/...` → `~/projects/rfid/rfid-manager/...`

Vanligaste mönstret. Förekommer i:

**Aktiva skript (exekverbara):**

| Fil | Rad | Typ |
|---|---|---|
| `test/fas2-mqtt/mqtt/test_subscriber_persist.py` | 1 | Shebang → `#!/home/joakim/...` |
| `test/fas2-mqtt/mqtt/test_subscriber_persist.py` | 40 | `DB_DIR = Path.home() / "rfid-manager" / "data"` |

**Dokumentation (wiki):**
- `wiki/MQTT-Infrastruktur.md` — 8 st
- `wiki/MQTT-Manual.md` — 7 st
- `wiki/MQTT-Explorer.md` — 2 st
- `wiki/Fas-200-Web-Dashboard.md` — 6 st
- `wiki/Hardware-Testenheter.md` — 5 st
- `wiki/log.md` — ~30 st
- `wiki/index.md`, `wiki/App-Architecture.md`, `wiki/Kanban.md`, `wiki/Projektrapport.md`
- `wiki/Rollfordelning-och-Arbetsatt.md`, `wiki/Kundrelationer-och-Acceptans.md`
- `wiki/Figma-Steps-Fas2-Build-Guide.md`, `wiki/Fas3-Implementation-Plan.md`
- `wiki/Produkt-Roadmap.md`, `wiki/Nomenclature-Figma-Android.md`
- `wiki/Fas2-Implementation-Overview.md`, `wiki/Release-Notes.md`, `wiki/Projektrapport.md`
- `wiki/PUSH-TO-GITHUB.md` — 2 st

**Presentation scripts:**
- `create_presentation.py` — 3 st
- `create_time_estimate.py` — 1 st
- `create_workflow_presentation.py` — 1 st

**MCP-server README:**
- `mcp-server/README.md` — 3 st

**llm-wiki kopior (samma mönster):**
- `llm-wiki/wiki/log.md`, `llm-wiki/wiki/App-Architecture.md`, `llm-wiki/wiki/index.md`, etc.
- Samma filer som ovan, speglade i llm-wiki-repot.

**Totalt:** ~70+ instanser i ~25 filer

### 1.2 Mönster: `~/projects/rfid/rfid-manager-android/...` → `~/projects/rfid/rfid-manager-android/`

Förekommer i:
- `wiki/log.md` — 5 st
- `wiki/Fas3-Implementation-Plan.md` — 1 st
- `wiki/Release-Notes.md` — 1 st
- `wiki/Projektrapport.md` — 1 st
- `llm-wiki/wiki/log.md` — 5 st
- `llm-wiki/wiki/Fas3-Implementation-Plan.md` — 1 st

**Totalt:** ~14 instanser i ~5 filer

### 1.3 Mönster: `/home/joakim/projects/rfid/llm-wiki/...` → `~/projects/rfid/llm-wiki/...`

- `wiki/index.md` — rad 12 (schema.md-länk)
- `llm-wiki/wiki/index.md` — rad 12
- `create_presentation.py`, `create_time_estimate.py`, `create_workflow_presentation.py` — output_path

**Totalt:** 5 instanser

### 1.4 Mönster: `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/...` → `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/`

- `wiki/Figma-Steps-Fas2-Build-Guide.md` — 4 st
- `wiki/Fas2-Implementation-Overview.md` — 1 st
- `wiki/log.md` — 4 st
- `wiki/Produkt-Roadmap.md` — 1 st
- `llm-wiki/wiki/` — motsvarande kopior

**Totalt:** ~11 instanser

### 1.5 Mönster: IP-adress `192.168.50.128` → bekräfta ny falstaff-IP

Förekommer i **aktiv källkod** (måste uppdateras för att appen ska fungera):

| Fil | Rad |
|---|---|
| `rfid-manager-android/.../MqttConnectionManager.kt` | 13 |
| `rfid-manager-android/.../MainActivity.kt` | 59 |
| `rfid-manager-android/.../SettingsScreen.kt` | 42 |
| `rfid-manager-android/.../AppSettings.kt` | 31 |
| `rfid-manager-android/.../network_security_config.xml` | 7 |

Förekommer i **dokumentation:**
- `wiki/App-Architecture.md` — 3 st
- `wiki/MQTT-Infrastruktur.md` — ~12 st
- `wiki/MQTT-Explorer.md` — 1 st
- `wiki/Fas-101-MQTT-Configuration.md` — 1 st
- `wiki/Testplan.md` — 3 st
- `wiki/Kundrelationer-och-Acceptans.md` — 3 st
- `wiki/log.md` — 4 st
- `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md` — ~10 st
- `create_presentation.py` — ~12 st

**Totalt:** ~50+ instanser i ~10 filer

---

## 2. Refererade men saknade filer (måste hämtas från sixten)

### 2.1 Figma-mock-bilder (`~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/`)

| Fil | Storlek | Ny plats (förslag) |
|---|---|---|
| `fas2-main-rfid-screen.jpg` | ~227 KB | `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/` *(finns redan!)* |
| `fas2-persisted-readings-list.jpg` | ~264 KB | *(finns redan!)* |
| `fas2-mqtt-sparkplug-status.jpg` | ~265 KB | *(finns redan!)* |

> **Status:** Bilderna finns redan på `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/`. Den gamla sökvägen `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/` är redundant. Dokumentationen måste uppdateras att peka på rätt ställe.

### 2.2 Python virtual environments (.venv)

| Sökväg (sixten) | Status på falstaff | Åtgärd |
|---|---|---|
| `~/projects/rfid/rfid-manager/test/fas2-mqtt/.venv/` | **SAKNAS** | Skapa nytt: `python3 -m venv .venv && source .venv/bin/activate && pip install paho-mqtt` |
| `~/projects/rfid/rfid-manager/dashboard/.venv/` | **SAKNAS** | Skapa nytt: `python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt` |
| `~/projects/rfid/rfid-manager/test/fas2-mqtt/mqtt/requirements.txt` | **SAKNAS** | Skapa med `paho-mqtt>=2.1.0` |

> `.venv` innehåller bara Python-paket som kan ominstalleras. Inget unikt innehåll som måste kopieras.

### 2.3 OpenCode MCP-konfiguration

| Sökväg (sixten) | Status på falstaff |
|---|---|
| `~/.config/opencode/opencode.json` (med MCP-server) | **SAKNAS** — ny `opencode.jsonc` finns utan MCP-konfig |

OpenCode-konfiguration för MCP-server måste återskapas. Se `mcp-server/README.md` och `wiki/Fas-200-Web-Dashboard.md` för MCP-server-inställningar.

### 2.4 Android SDK

| Sökväg (sixten) | Status på falstaff |
|---|---|
| `~/Android/Sdk/` | **SAKNAS** — Android Studio / SDK inte installerat på falstaff |

Android SDK måste installeras på falstaff för att kunna bygga appen.

### 2.5 Gamla releases (tomma kataloger)

| Katalog | Innehåll på falstaff |
|---|---|
| `~/projects/rfid/rfid-manager/releases/2026-06-Fas2/` | **Tom** |
| `~/projects/rfid/rfid-manager/releases/2026-06-Fas4/` | **Tom** |
| `~/projects/rfid/rfid-manager/releases/2026-06-Fas5/` | Endast `manual/manual.pdf` |

Dessa innehöll troligen källkodssnapshots på sixten. Om de behövs, måste de kopieras över.

---

## 3. Förslag på placering av saknade filer

Alla filer placeras under den nya projektroten `~/projects/rfid/`:

| Saknad resurs | Källa (sixten) | Mål (falstaff) |
|---|---|---|
| Figma-mock-bilder | `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/` | `~/projects/rfid/rfid-manager/test/fas2-mqtt/figma-mocks/` *(redan på plats)* |
| Release Fas2 snapshot | `~/projects/rfid/rfid-manager/releases/2026-06-Fas2/` | `~/projects/rfid/rfid-manager/releases/2026-06-Fas2/` |
| Release Fas4 snapshot | `~/projects/rfid/rfid-manager/releases/2026-06-Fas4/` | `~/projects/rfid/rfid-manager/releases/2026-06-Fas4/` |
| Android SDK | `~/Android/Sdk/` | `~/Android/Sdk/` *(bör vara samma sökväg på falstaff)* |
| OpenCode MCP config | `~/.config/opencode/opencode.json` | `~/.config/opencode/opencode.jsonc` *(lägg till MCP-server-sektion)* |

> **Rekommendation:** `.venv`-miljöer återskapas på falstaff med `pip install` snarare än att kopiera. De innehåller bara standard-paket som är enkla att ominstallera.

---

## 4. Arbetsordning (föreslagen)

1. **Återskapa .venv** — `test/fas2-mqtt/.venv` och `dashboard/.venv`
2. **Kopiera release-snapshots** från sixten om de behövs
3. **Installera/konfigurera Android SDK** på falstaff
4. **Uppdatera IP-adress** i källkod + dokumentation
5. **Uppdatera alla sökvägar** i wiki + dokumentation (bulk find-replace)
6. **Återskapa MCP-konfig** i `~/.config/opencode/opencode.jsonc`
7. **Verifiera** att allt bygger och fungerar

---

## 5. Dokument som berörs av sökvägsuppdateringar

### llm-wiki (detta repo)
- `wiki/index.md`
- `wiki/log.md`
- `wiki/MQTT-Infrastruktur.md`
- `wiki/MQTT-Manual.md`
- `wiki/MQTT-Explorer.md`
- `wiki/Fas-200-Web-Dashboard.md`
- `wiki/Hardware-Testenheter.md`
- `wiki/App-Architecture.md`
- `wiki/Kanban.md`
- `wiki/Fas3-Implementation-Plan.md`
- `wiki/Release-Notes.md`
- `wiki/Projektrapport.md`
- `wiki/Rollfordelning-och-Arbetsatt.md`
- `wiki/Kundrelationer-och-Acceptans.md`
- `wiki/Figma-Steps-Fas2-Build-Guide.md`
- `wiki/Produkt-Roadmap.md`
- `wiki/Nomenclature-Figma-Android.md`
- `wiki/Fas2-Implementation-Overview.md`
- `wiki/Fas-101-MQTT-Configuration.md`
- `wiki/Testplan.md`
- `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md`
- `wiki/PUSH-TO-GITHUB.md`

### rfid-manager (wiki + scripts)
- Samma wiki-filer som ovan (spegelkopia)
- `create_presentation.py`
- `create_time_estimate.py`
- `create_workflow_presentation.py`
- `mcp-server/README.md`
- `test/fas2-mqtt/mqtt/test_subscriber_persist.py`
- `README.md`

### rfid-manager-android (källkod)
- `app/src/main/java/.../MqttConnectionManager.kt`
- `app/src/main/java/.../MainActivity.kt`
- `app/src/main/java/.../SettingsScreen.kt`
- `app/src/main/java/.../AppSettings.kt`
- `app/src/main/res/xml/network_security_config.xml`

---

Se även [[Kanban]] för status på Fas-500 uppgifter.

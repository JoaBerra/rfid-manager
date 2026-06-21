# Fas-400 — Teknikmiljö validering

**Status:** Pågår
**Mål:** Gör det möjligt för en kund att klona repot och få igång hela utvecklingsmiljön på en ny Ubuntu 24.04-maskin.

---

## 1. Komponentkarta

### 1.1 Repon

| Komponent | Klon-URL | Lokal sökväg |
|---|---|---|
| RFID Manager (backend/test) | `https://github.com/JoaBerra/rfid-manager` | `~/projects/rfid/rfid-manager/` |
| RFID Manager Android | `https://github.com/JoaBerra/rfid-manager-android` | `~/projects/rfid/rfid-manager-android/` |
| LLM Wiki | `https://github.com/JoaBerra/llm-wiki` | `~/projects/rfid/llm-wiki/` |

**Kloningsordning:** Inbördes oberoende.

### 1.2 Systemberoenden (Ubuntu 24.04)

| Paket | Version (min) | Installation |
|---|---|---|
| Python 3 | ≥ 3.12 | `apt install python3 python3-venv` |
| OpenJDK | ≥ 21 | `apt install openjdk-21-jdk` |
| Docker | ≥ 24 | `apt install docker.io docker-compose-v2` |
| Git | ≥ 2.40 | `apt install git` |
| curl | valfri | `apt install curl` |
| adb | valfri | `apt install android-tools-adb` |

### 1.3 Python-beroenden

| Miljö | Sökväg | Kravfil | Nyckelpaket |
|---|---|---|---|
| Test subscriber | `test/fas2-mqtt/.venv` | — | `paho-mqtt` |
| Dashboard | `dashboard/.venv` | `dashboard/requirements.txt` | `fastapi`, `uvicorn`, `paho-mqtt` |
| MCP-server | (delar dashboardens .venv) | `mcp-server/requirements.txt` | `mcp`, `httpx` |

**Skapa alla:**
```bash
cd ~/projects/rfid/rfid-manager
python3 -m venv test/fas2-mqtt/.venv
test/fas2-mqtt/.venv/bin/pip install paho-mqtt

python3 -m venv dashboard/.venv
dashboard/.venv/bin/pip install -r dashboard/requirements.txt
dashboard/.venv/bin/pip install -r mcp-server/requirements.txt
```

### 1.4 Docker-komponenter

| Komponent | Image | Portar | Volym-mount |
|---|---|---|---|
| MQTT Broker | `eclipse-mosquitto:2` | 1883 (MQTT), 9001 (WS) | `mosquitto.conf` |

**Starta broker:**
```bash
docker run -d --name rfid-mqtt \
  -p 1883:1883 -p 9001:9001 \
  -v ~/projects/rfid/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf \
  eclipse-mosquitto:2
```

**Verifiera:**
```bash
mosquitto_sub -h localhost -p 1883 -t "test" -d
# Ska visa "Client ... connected"
```

### 1.5 Android-beroenden

| Komponent | Sökväg | Not |
|---|---|---|
| Android Studio | snap | `sudo snap install android-studio --classic` |
| Android SDK | `~/Android/Sdk/` | Ladda ner via Studio SDK Manager |
| build-tools | 36+ | SDK Manager |
| platforms | android-36+ | SDK Manager |

**Miljövariabler (i `~/.bashrc`):**
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 1.6 ADB och USB

| Steg | Kommando |
|---|---|
| Installera adb | `sudo apt install android-tools-adb` |
| udev-regler (Samsung) | Se `setup/51-android.rules` |
| Ladda regler | `sudo cp setup/51-android.rules /etc/udev/rules.d/ && sudo udevadm control --reload-rules && sudo udevadm trigger` |

**På telefonen:**
- Utvecklaralternativ → USB debugging = PÅ
- USB → "Överföra filer/Android Auto"

### 1.7 AI-verktyg

| Komponent | Installation |
|---|---|
| OpenCode | Ladda ner från https://opencode.ai och lägg i PATH |
| Qwen2.5 GGUF | Ladda ner från Hugging Face (Qwen/Qwen2.5-Coder-7B-Instruct-GGUF) |

### 1.8 MCP-server

Används av OpenCode för att ge AI-assistenten tillgång till dashboard-data.

**Starta:**
```bash
cd ~/projects/rfid/rfid-manager/mcp-server
../dashboard/.venv/bin/python server.py
```

**Konfiguration i `~/.config/opencode/opencode.jsonc`:**
```json
{
  "mcpServers": {
    "rfid-manager": {
      "command": "/home/joakim/projects/rfid/rfid-manager/dashboard/.venv/bin/python",
      "args": ["/home/joakim/projects/rfid/rfid-manager/mcp-server/server.py"],
      "env": {
        "DASHBOARD_URL": "http://localhost:8001",
        "MQTT_BROKER": "localhost",
        "MQTT_PORT": "1883"
      }
    }
  }
}
```

---

## 2. Bootstrap

Ett automatiserat bootstrap-script finns på `setup/bootstrap.sh`:

```bash
bash ~/projects/rfid/rfid-manager/setup/bootstrap.sh
```

Scriptet installerar systempaket, skapar Python-miljöer, startar MQTT-broker, installerar USB-regler, och ger instruktioner för manuella steg (OpenCode, Qwen2.5, Android Studio).

---

## 3. Arkitekturöversikt

```
┌─────────────────────────────────────────────────────────────┐
│                     Telefon (Samsung Note 10)               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  RFID Manager App (Kotlin/Compose)                     ││
│  │  NFC Reader → MQTT Publish → rfidmanager/<uid>/telemetry││
│  └───────────────────────┬─────────────────────────────────┘│
│                          │ Wi-Fi / USB                       │
└──────────────────────────┼──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     Server (falstaff)                        │
│                                                             │
│  ┌─────────────────────┐   ┌─────────────────────────────┐  │
│  │  Eclipse Mosquitto  │   │  Dashboard (FastAPI + SSE)  │  │
│  │  Docker container   │◄──┤  :8001/api/*               │  │
│  │  :1883 (MQTT)       │   └───────────┬─────────────────┘  │
│  └─────────────────────┘               │                    │
│                                        │                    │
│  ┌─────────────────────┐   ┌───────────▼─────────────────┐  │
│  │  Python Subscriber  │   │  MCP-server                 │  │
│  │  test_subscriber_   │   │  (MCP SDK → OpenCode)      │  │
│  │  persist.py         │   └─────────────────────────────┘  │
│  └─────────────────────┘                                    │
│                                                             │
│  ┌─────────────────────┐                                    │
│  │  OpenCode + Qwen2.5 │◄──── MCP tools                    │
│  └─────────────────────┘                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Verifieringsrutiner

Kör i angiven ordning. Varje steg har ett konkret "PASS"-kriterium.

### 4.1 Repon klonade

```bash
ls -d ~/projects/rfid/*/
```

**PASS:** Alla tre kataloger syns:
- `rfid-manager/`
- `rfid-manager-android/`
- `llm-wiki/`

**FAIL:** `No such file or directory` → kör `git clone` för respektive repo.

### 4.2 Python-miljöer

```bash
cd ~/projects/rfid/rfid-manager

# Test-miljö
test/fas2-mqtt/.venv/bin/python -c "import paho.mqtt.client; print('paho-mqtt OK')"

# Dashboard-miljö
dashboard/.venv/bin/python -c "import fastapi; import uvicorn; print('dashboard OK')"

# MCP-server (delar dashboard-miljö)
dashboard/.venv/bin/python -c "import mcp; import httpx; print('mcp OK')"
```

**PASS:** Alla tre `OK`-meddelanden syns.
**FAIL:** `ModuleNotFoundError` → kör `pip install` för respektive requirements.txt.

### 4.3 Docker tillgängligt

```bash
docker info --format '{{.ServerVersion}}' 2>/dev/null || echo "Docker kräver sudo eller ny grupp"
```

**PASS:** versionsnummer syns (t.ex. `29.1.3`).
**FAIL:** `permission denied` → `sudo usermod -aG docker $USER` + logga ut/in.

### 4.4 MQTT Broker

```bash
# Starta om broker för säkerhets skull
docker stop rfid-mqtt 2>/dev/null; docker rm rfid-mqtt 2>/dev/null
docker run -d --name rfid-mqtt -p 1883:1883 -p 9001:9001 \
  -v ~/projects/rfid/rfid-manager/test/fas2-mqtt/mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf \
  eclipse-mosquitto:2

# Testa anslutning
timeout 3 bash -c 'echo "" | nc -w2 localhost 1883' && echo "Broker OK" || echo "Broker FAIL"
```

**PASS:** `Broker OK`.
**FAIL:** Kontrollera Docker, port 1883 inte blockerad.

### 4.5 Dashboard (FastAPI)

```bash
cd ~/projects/rfid/rfid-manager/dashboard
dashboard/.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8001 &
sleep 2
curl -sf http://localhost:8001/api/stats | python3 -m json.tool
```

**PASS:** JSON-svar med `total`, `unique_uids`, `connected: true`.
**FAIL:** `Connection refused` → port 8001 blockerad eller process kraschad. Kolla `dashboard.log`.

### 4.6 Android-bygge

```bash
cd ~/projects/rfid/rfid-manager-android
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug 2>&1 | tail -5
```

**PASS:** `BUILD SUCCESSFUL`.
**FAIL:** Gradle-fel — kontrollera SDK-sökväg i `local.properties`, JDK-version.

### 4.7 ADB-enhet

```bash
adb devices
```

**PASS:** Telefonens serial-nummer visas med status `device`.
**FAIL:** Se avsnitt 1.6 (udev-regler, USB debugging på telefonen).

### 4.8 MCP-server

```bash
cd ~/projects/rfid/rfid-manager/mcp-server
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | \
  ../dashboard/.venv/bin/python server.py 2>/dev/null | head -1
```

**PASS:** Får ett JSON-svar med `"jsonrpc":"2.0"`.
**FAIL:** `ModuleNotFoundError` → kör `dashboard/.venv/bin/pip install -r requirements.txt`.

### 4.9 OpenCode + Qwen2.5

```bash
# Verifiera OpenCode binär
opencode --version 2>/dev/null && echo "OpenCode OK" || echo "OpenCode FAIL: saknas i PATH"

# Verifiera Qwen2.5 modell
ls -lh ~/Hämtningar/qwen2.5-coder-7b-instruct-q4_k_m.gguf 2>/dev/null && echo "Modell OK" || echo "Modell FAIL"
```

**PASS:** Båda OK.
**FAIL:** Ladda ner från https://opencode.ai och https://huggingface.co/Qwen/Qwen2.5-Coder-7B-Instruct-GGUF.

---

Se även [[Kanban]] för status, [[MQTT-Infrastruktur]] för MQTT-detaljer, [[Fas-200-Web-Dashboard]] för dashboard/MCP.

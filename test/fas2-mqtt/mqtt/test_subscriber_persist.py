#!/home/joakim/projects/rfid/rfid-manager/test/fas2-mqtt/.venv/bin/python3
"""
Enhanced MQTT subscriber with structured, color-coded logging.

Usage:
  ./test_subscriber_persist.py              # listen to all devices
  ./test_subscriber_persist.py --uid E4:9A  # filter by UID substring

For visual inspection of MQTT traffic, use MQTT Explorer instead:
  https://github.com/thomasnordquist/MQTT-Explorer
  See wiki: ../../wiki/MQTT-Explorer.md

Requires: paho-mqtt (available in ../.venv/)
"""
import argparse
import json
import sqlite3
import signal
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import paho.mqtt.client as mqtt

# ── ANSI colors ─────────────────────────────────────────────────────────────
class Color:
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    CYAN = "\033[96m"
    GRAY = "\033[90m"
    BOLD = "\033[1m"
    RESET = "\033[0m"

# ── Configuration ───────────────────────────────────────────────────────────
BROKER = "localhost"
PORT = 1883
TOPIC = "rfidmanager/+/telemetry"
DB_DIR = Path.home() / "projects" / "rfid" / "rfid-manager" / "data"
DB_PATH = DB_DIR / "rfid_readings.db"

stats = {"received": 0, "persisted": 0, "errors": 0}


def init_db():
    DB_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS readings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            type TEXT,
            timestamp TEXT,
            correlation_id TEXT,
            uid TEXT,
            data TEXT,
            source TEXT,
            received_at TEXT
        )
    """)
    conn.commit()
    conn.close()
    log("info", f"Database ready: {DB_PATH}")


def log(level: str, message: str):
    ts = datetime.now(timezone.utc).isoformat()
    prefix = {
        "recv": f"{Color.GREEN}⬡ RECV{Color.RESET}",
        "send": f"{Color.CYAN}◈ SEND{Color.RESET}",
        "info": f"{Color.GRAY}● INFO{Color.RESET}",
        "ok":   f"{Color.GREEN}✔ OK{Color.RESET}",
        "warn": f"{Color.YELLOW}⚠ WARN{Color.RESET}",
        "err":  f"{Color.RED}✘ ERR{Color.RESET}",
        "conn": f"{Color.CYAN}🔗 MQTT{Color.RESET}",
    }.get(level, f"{Color.GRAY}{level}{Color.RESET}")
    print(f"{Color.GRAY}[{ts}]{Color.RESET} {prefix} {message}", flush=True)


def on_connect(client, userdata, flags, rc, properties=None):
    now = datetime.now(timezone.utc).isoformat()
    status = "ok" if rc == 0 else "err"
    label = "Connected" if rc == 0 else f"Connection refused (rc={rc})"
    log(status, label)
    if rc == 0:
        client.subscribe(TOPIC)
        log("info", f"Subscribed to {TOPIC}")


def on_disconnect(client, userdata, rc):
    level = "warn" if rc != 0 else "info"
    log(level, f"Disconnected (rc={rc}) — will auto-reconnect")


def on_message(client, userdata, msg):
    uid_filter = userdata.get("uid_filter", "")
    try:
        payload = json.loads(msg.payload.decode())
        stats["received"] += 1

        msg_type = payload.get("type") or (
            payload.get("metrics", [{}])[0].get("name")
            if payload.get("metrics") else None
        )

        # Extract UID
        p = payload.get("data") or payload.get("payload") or {}
        if isinstance(p, str):
            try:
                p = json.loads(p)
            except Exception:
                p = {}
        uid = (
            payload.get("uid")
            or p.get("uid")
            or (
                msg.topic.split("/")[1]
                if "/" in msg.topic and len(msg.topic.split("/")) > 1
                else None
            )
        )

        # Skip if uid_filter is set and doesn't match
        if uid_filter and uid and uid_filter.lower() not in uid.lower():
            return

        # Color-coded log line
        ts_msg = payload.get("timestamp", "?")
        log("recv", f"{Color.BOLD}{msg_type}{Color.RESET}  uid={uid}  topic={msg.topic}")

        # Persist
        metrics = payload.get("metrics", [{}])
        data_for_db = p.get("dataHex") or json.dumps(p) if isinstance(p, (dict, list)) else str(p)
        src = payload.get("source") or p.get("source", "RFID")
        corr = payload.get("correlationId", f"sparkplug-seq-{payload.get('seq', 0)}")

        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("""
            INSERT INTO readings (type, timestamp, correlation_id, uid, data, source, received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (
            msg_type,
            ts_msg,
            corr,
            uid,
            data_for_db,
            src,
            datetime.now(timezone.utc).isoformat()
        ))
        conn.commit()
        conn.close()
        stats["persisted"] += 1

        log("ok", f"Persisted uid={uid}  type={msg_type}")

    except Exception as e:
        stats["errors"] += 1
        log("err", f"Processing failed: {e}")


def print_stats(signum=None, frame=None):
    """Print statistics and exit."""
    print()
    log("info", f"{Color.BOLD}Session summary:{Color.RESET}")
    log("info", f"  Received:  {stats['received']}")
    log("info", f"  Persisted: {stats['persisted']}")
    log("info", f"  Errors:    {stats['errors']}")
    sys.exit(0)


def main():
    parser = argparse.ArgumentParser(description="MQTT subscriber with structured logging")
    parser.add_argument("--uid", default="", help="Filter by UID substring")
    args = parser.parse_args()

    signal.signal(signal.SIGINT, print_stats)

    init_db()
    client = mqtt.Client(
        callback_api_version=mqtt.CallbackAPIVersion.VERSION2
    )
    client.user_data_set({"uid_filter": args.uid})
    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message

    log("info", f"Connecting to {BROKER}:{PORT}  (Ctrl+C to stop)")
    if args.uid:
        log("info", f"Filter active: uid contains \"{args.uid}\"")
    client.connect(BROKER, PORT, 60)
    client.loop_forever()


if __name__ == "__main__":
    main()

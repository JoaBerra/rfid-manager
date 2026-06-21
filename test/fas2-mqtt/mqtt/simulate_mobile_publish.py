#!/usr/bin/env python3
"""
Simple simulator for 'mobile' publishing a reading over MQTT.
Uses the local test broker.
Run from the venv: ~/rfid-fas2-test/.venv/bin/python simulate_mobile_publish.py
"""
import json
import time
import uuid
from datetime import datetime, timezone

import paho.mqtt.client as mqtt

BROKER = "localhost"
PORT = 1883
TOPIC = "rfidmanager/test-device-001/telemetry"
CLIENT_ID = "mobile-simulator"

def main():
    client = mqtt.Client(client_id=CLIENT_ID)
    client.connect(BROKER, PORT, 60)

    # Simulate a persisted RFID reading using Sparkplug B-inspired structure (per locked decision for industrial)
    # Topic will be under spBv1.0 for full compliance later; simple for now.
    message = {
        "timestamp": int(datetime.now(timezone.utc).timestamp() * 1000),
        "metrics": [
            {
                "name": "ReadEscortMemory",  # Verb + Noun as metric name (Sparkplug style)
                "timestamp": int(datetime.now(timezone.utc).timestamp() * 1000),
                "dataType": "String",
                "value": json.dumps({  # Nested for our data; full Sparkplug uses aliases etc.
                    "uid": "0479981A8A6A80",
                    "page": 12,
                    "dataHex": "74657374",  # "test"
                    "deviceId": "test-device-001",
                    "source": "RFID"
                })
            }
        ],
        "seq": 1
    }

    payload = json.dumps(message, indent=2)
    print("Publishing to", TOPIC)
    print(payload)

    client.publish(TOPIC, payload, qos=1)
    client.disconnect()
    print("Done. Check test subscriber or broker logs.")

if __name__ == "__main__":
    main()
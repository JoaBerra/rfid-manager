import json
import logging
import os
import threading
import time
from collections import deque
from datetime import datetime

import paho.mqtt.client as mqtt

logger = logging.getLogger("dashboard.mqtt")

MAX_MESSAGES = 200

messages = deque(maxlen=MAX_MESSAGES)
_messages_lock = threading.Lock()
total_count = 0
unique_uids = set()
_uid_lock = threading.Lock()
mqtt_connected = False
notification_queue = None
_minute_counts = {}
_last_minute_cleanup = time.time()


def set_notification_queue(q):
    global notification_queue
    notification_queue = q


def on_connect(client, userdata, flags, reasonCode, properties=None):
    global mqtt_connected
    mqtt_connected = reasonCode == 0
    topic = os.getenv("MQTT_TOPIC", "rfidmanager/+/telemetry")
    if mqtt_connected:
        logger.info("Connected to MQTT broker, subscribing to %s", topic)
        client.subscribe(topic)
    else:
        logger.error("MQTT connection failed with code %s", reasonCode)


def on_disconnect(client, userdata, flags, reasonCode, properties=None):
    global mqtt_connected
    mqtt_connected = False
    logger.warning("Disconnected from MQTT broker (rc=%s)", reasonCode)


def on_message(client, userdata, msg):
    global total_count, unique_uids, _minute_counts, _last_minute_cleanup
    try:
        payload = json.loads(msg.payload.decode())
        topic = msg.topic

        uid = payload.get("uid", "")
        if not uid:
            parts = topic.split("/")
            uid = parts[1] if len(parts) > 1 else "unknown"

        entry = {
            "id": f"{time.time_ns()}",
            "topic": topic,
            "uid": uid,
            "type": payload.get("type", "unknown"),
            "timestamp": payload.get("timestamp", int(time.time() * 1000)),
            "source": payload.get("source", ""),
            "sparkplug": payload.get("sparkplug", False),
            "data": payload.get("data", {}),
            "raw": payload,
            "received_at": datetime.now().isoformat(),
        }

        with _messages_lock:
            messages.appendleft(entry)

        total_count += 1
        with _uid_lock:
            unique_uids.add(uid)

        minute_key = int(time.time() / 60)
        _minute_counts[minute_key] = _minute_counts.get(minute_key, 0) + 1
        if time.time() - _last_minute_cleanup > 120:
            _last_minute_cleanup = time.time()
            for k in list(_minute_counts.keys()):
                if k < minute_key - 5:
                    del _minute_counts[k]

        if notification_queue is not None:
            notification_queue.put(entry)

    except json.JSONDecodeError:
        logger.warning("Non-JSON message on %s: %s", msg.topic, msg.payload[:100])
    except Exception as e:
        logger.error("Error processing message: %s", e)


def get_messages(limit=50):
    with _messages_lock:
        return list(messages)[:limit]


def get_stats():
    with _uid_lock:
        uid_count = len(unique_uids)
    current_minute = int(time.time() / 60)
    per_minute = _minute_counts.get(current_minute, 0)
    return {
        "total": total_count,
        "unique_uids": uid_count,
        "connected": mqtt_connected,
        "per_minute": per_minute,
    }


def start_client():
    broker = os.getenv("MQTT_BROKER", "localhost")
    port = int(os.getenv("MQTT_PORT", "1883"))
    client_id = os.getenv("MQTT_CLIENT_ID", "rfid-dashboard")

    client = mqtt.Client(client_id=client_id, callback_api_version=mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message

    try:
        client.connect_async(broker, port, 60)
        client.loop_start()
        logger.info("MQTT client connecting to %s:%s", broker, port)
    except Exception as e:
        logger.error("Failed to start MQTT client: %s", e)

    return client

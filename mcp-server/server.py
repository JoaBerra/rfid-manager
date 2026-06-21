from __future__ import annotations

import json
import logging
import os

import httpx
import mcp.types as types
from mcp.server import Server
from mcp.server.models import InitializationOptions
from mcp.types import ServerCapabilities, ToolsCapability

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("rfid-mcp")

DASHBOARD_URL = os.getenv("DASHBOARD_URL", "http://localhost:8001")
MQTT_BROKER = os.getenv("MQTT_BROKER", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))

app = Server("rfid-manager-mcp")


@app.list_tools()
async def list_tools() -> list[types.Tool]:
    return [
        types.Tool(
            name="get_stats",
            description="Hämta aktuell statistik från RFID-dashboarden (totalt meddelanden, unika UID:n, anslutningsstatus, meddelanden/minut, driftid)",
            inputSchema={
                "type": "object",
                "properties": {},
            },
        ),
        types.Tool(
            name="get_messages",
            description="Hämta senaste N meddelanden från RFID-dashboarden",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {
                        "type": "integer",
                        "description": "Antal meddelanden att hämta (max 200)",
                        "default": 10,
                    }
                },
            },
        ),
        types.Tool(
            name="publish_mqtt",
            description="Publicera ett MQTT-meddelande direkt till brokern",
            inputSchema={
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "MQTT-topic, t.ex. rfidmanager/047B05CA885884/telemetry",
                    },
                    "payload": {
                        "type": "string",
                        "description": "JSON-sträng att publicera som meddelande",
                    },
                },
                "required": ["topic", "payload"],
            },
        ),
        types.Tool(
            name="get_live_events",
            description="Hämta nya realtidshändelser från dashboardens SSE-ström. Returnerar händelser som kommit in under anropet (timeout 5 sekunder).",
            inputSchema={
                "type": "object",
                "properties": {},
            },
        ),
    ]


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[types.TextContent]:
    if name == "get_stats":
        return await _get_stats()
    elif name == "get_messages":
        return await _get_messages(arguments.get("limit", 10))
    elif name == "publish_mqtt":
        return await _publish_mqtt(arguments["topic"], arguments["payload"])
    elif name == "get_live_events":
        return await _get_live_events()
    else:
        raise ValueError(f"Unknown tool: {name}")


async def _get_stats() -> list[types.TextContent]:
    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{DASHBOARD_URL}/api/stats", timeout=10)
        resp.raise_for_status()
        stats = resp.json()
    return [types.TextContent(type="text", text=json.dumps(stats, indent=2))]


async def _get_messages(limit: int) -> list[types.TextContent]:
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{DASHBOARD_URL}/api/messages", params={"limit": limit}, timeout=10
        )
        resp.raise_for_status()
        messages = resp.json()
    if not messages:
        return [types.TextContent(type="text", text="Inga meddelanden i dashboardens buffert.")]
    lines = []
    for m in messages[:limit]:
        ts = m.get("timestamp", "")
        uid = m.get("uid", "")
        typ = m.get("type", "")
        topic = m.get("topic", "")
        sparkplug = "Sparkplug" if m.get("sparkplug") else ""
        source = m.get("source", "")
        lines.append(f"[{ts}] {uid} | {typ} {sparkplug} | {source} | topic={topic}")
    return [types.TextContent(type="text", text="\n".join(lines))]


async def _publish_mqtt(topic: str, payload: str) -> list[types.TextContent]:
    try:
        import paho.mqtt.client as mqtt
    except ImportError:
        return [types.TextContent(type="text", text="FEL: paho-mqtt är inte installerat på MCP-servern.")]
    try:
        json.loads(payload)
    except json.JSONDecodeError:
        return [types.TextContent(type="text", text=f"FEL: payload är inte giltig JSON: {payload}")]

    client = mqtt.Client(client_id="rfid-mcp-publisher")
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 10)
        client.loop_start()
        result = client.publish(topic, payload, qos=1)
        result.wait_for_publish(5)
        client.disconnect()
        client.loop_stop()
    except Exception as e:
        return [types.TextContent(type="text", text=f"FEL vid MQTT-publicering: {e}")]

    return [types.TextContent(type="text", text=f"OK: meddelande publicerat till {topic}")]


async def _get_live_events() -> list[types.TextContent]:
    import paho.mqtt.client as mqtt
    import threading
    import time

    events = []
    ev_lock = threading.Lock()
    connected = threading.Event()

    def on_connect(c, u, f, rc):
        if rc == 0:
            connected.set()
            c.subscribe("rfidmanager/+/telemetry")

    def on_message(c, u, msg):
        try:
            payload = json.loads(msg.payload.decode())
            with ev_lock:
                events.append(payload)
        except json.JSONDecodeError:
            pass

    client = mqtt.Client(client_id="rfid-mcp-listener")
    client.on_connect = on_connect
    client.on_message = on_message

    try:
        client.connect_async(MQTT_BROKER, MQTT_PORT, 10)
        client.loop_start()
        if not connected.wait(timeout=5):
            client.loop_stop()
            return [types.TextContent(type="text", text="Kunde inte ansluta till MQTT-brokern.")]
        time.sleep(5)
    finally:
        client.loop_stop()
        client.disconnect()

    with ev_lock:
        if not events:
            return [types.TextContent(type="text", text="Inga nya MQTT-meddelanden under de senaste 5 sekunderna.")]
        lines = []
        for e in events:
            uid = e.get("uid", "?")
            typ = e.get("type", "?")
            lines.append(f"[{e.get('timestamp', '?')}] {uid} | {typ}")
        return [types.TextContent(type="text", text="\n".join(lines))]


async def main():
    async with __import__("mcp.server.stdio", fromlist=["stdio_server"]).stdio_server() as (read, write):
        await app.run(
            read,
            write,
            InitializationOptions(
                server_name="rfid-manager-mcp",
                server_version="1.0.0",
                capabilities=ServerCapabilities(tools=ToolsCapability(listChanged=True)),
            ),
        )

if __name__ == "__main__":
    __import__("asyncio").run(main())

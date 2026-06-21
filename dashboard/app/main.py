import asyncio
import json
import logging
import os
import queue
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import FileResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles

from . import mqtt_client

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
logger = logging.getLogger("dashboard")


@asynccontextmanager
async def lifespan(app: FastAPI):
    notification_queue = queue.Queue()
    mqtt_client.set_notification_queue(notification_queue)
    client = mqtt_client.start_client()
    app.state.start_time = time.time()
    yield
    if client:
        client.loop_stop()
        client.disconnect()


app = FastAPI(title="RFID Manager Dashboard", version="1.0.0", lifespan=lifespan)

static_dir = os.path.join(os.path.dirname(__file__), "..", "static")
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.get("/")
async def root():
    return FileResponse(os.path.join(static_dir, "index.html"))


@app.get("/api/stats")
async def get_stats():
    stats = mqtt_client.get_stats()
    stats["uptime"] = time.time() - getattr(app.state, "start_time", time.time())
    return stats


@app.get("/api/messages")
async def get_messages(limit: int = 50):
    return mqtt_client.get_messages(limit)


@app.get("/api/events")
async def sse_events(request: Request):
    async def event_generator():
        while True:
            if await request.is_disconnected():
                break
            try:
                msg = mqtt_client.notification_queue.get_nowait()
                yield f"data: {json.dumps(msg)}\n\n"
            except queue.Empty:
                await asyncio.sleep(0.3)
                yield ": keepalive\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=False)

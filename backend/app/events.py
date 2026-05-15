import asyncio
import json
from datetime import datetime, timezone
from typing import Any

from fastapi import WebSocket


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class EventHub:
    def __init__(self) -> None:
        self._clients: set[WebSocket] = set()
        self._lock = asyncio.Lock()

    async def connect(self, websocket: WebSocket) -> None:
        await websocket.accept()
        async with self._lock:
            self._clients.add(websocket)

    async def disconnect(self, websocket: WebSocket) -> None:
        async with self._lock:
            self._clients.discard(websocket)

    async def publish(self, event_type: str, payload: dict[str, Any] | None = None) -> None:
        message = json.dumps(
            {
                "type": event_type,
                "timestamp": utc_now(),
                "payload": payload or {},
            },
            ensure_ascii=False,
        )
        async with self._lock:
            clients = list(self._clients)
        if not clients:
            return
        stale: list[WebSocket] = []
        for client in clients:
            try:
                await client.send_text(message)
            except Exception:
                stale.append(client)
        if stale:
            async with self._lock:
                for client in stale:
                    self._clients.discard(client)

    async def log(self, level: str, message: str, source: str = "system") -> None:
        await self.publish("log", {"level": level, "source": source, "message": message})

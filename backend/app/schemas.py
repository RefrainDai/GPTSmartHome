from typing import Any, Literal

from pydantic import BaseModel, Field


class Device(BaseModel):
    id: str
    name: str
    type: str
    room: str
    is_on: bool = False
    level: int | None = None
    temperature: int | None = None
    mode: str | None = None
    locked: bool | None = None
    updated_at: str


class DeviceActionRequest(BaseModel):
    action: str = Field(..., examples=["on", "off", "toggle", "set_temperature"])
    params: dict[str, Any] = Field(default_factory=dict)
    source: str = "manual"


class TextCommandRequest(BaseModel):
    text: str
    source: str = "manual"


class Intent(BaseModel):
    device_id: str | None = None
    action: str
    params: dict[str, Any] = Field(default_factory=dict)
    confidence: float = 1.0
    reply: str | None = None


class CommandResult(BaseModel):
    ok: bool
    message: str
    intent: Intent | None = None
    devices: list[Device] = Field(default_factory=list)
    audio_url: str | None = None


class ScheduleRequest(BaseModel):
    device_id: str
    action: str
    delay_seconds: float = Field(..., ge=0.1, le=86400)
    params: dict[str, Any] = Field(default_factory=dict)


class ListenerStatus(BaseModel):
    running: bool
    mode: Literal["voice", "gesture"]

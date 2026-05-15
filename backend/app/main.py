from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from .commands import CommandProcessor
from .devices import DeviceStateManager
from .events import EventHub
from .gesture import GestureListener
from .llm import LLMIntentParser
from .scheduler import SchedulerService
from .schemas import DeviceActionRequest, ListenerStatus, ScheduleRequest, TextCommandRequest
from .settings import ASSETS_DIR, get_settings
from .speaker import SpeakerVerifier
from .tts import TTSService
from .voice import VoiceListener


settings = get_settings()
hub = EventHub()
devices = DeviceStateManager()
tts = TTSService(settings)
llm = LLMIntentParser(settings)
processor = CommandProcessor(devices, hub, tts, llm)
speaker = SpeakerVerifier(settings)
voice = VoiceListener(settings, hub, speaker)
gesture = GestureListener(settings, hub)
scheduler = SchedulerService(hub)
voice.bind_processor(processor)
gesture.bind_processor(processor)
scheduler.bind_processor(processor)

app = FastAPI(title="GPTSmartHome", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.frontend_origin] if settings.frontend_origin != "*" else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/assets", StaticFiles(directory=str(ASSETS_DIR)), name="assets")


@app.on_event("startup")
async def startup() -> None:
    await hub.log("info", "GPTSmartHome 后端已启动", "system")


@app.get("/health")
async def health() -> dict:
    return {"ok": True, "voice_running": voice.running, "gesture_running": gesture.running}


@app.get("/api/devices")
async def list_devices() -> dict:
    return {"devices": [device.model_dump() for device in devices.list_devices()]}


@app.post("/api/devices/{device_id}/action")
async def control_device(device_id: str, request: DeviceActionRequest):
    from .schemas import Intent

    intent = Intent(device_id=device_id, action=request.action, params=request.params)
    return await processor.execute_intent(intent, source=request.source)


@app.post("/api/command/text")
async def text_command(request: TextCommandRequest):
    return await processor.handle_text(request.text, source=request.source)


@app.post("/api/listening/start", response_model=ListenerStatus)
async def start_listening() -> ListenerStatus:
    await voice.start()
    return ListenerStatus(running=voice.running, mode="voice")


@app.post("/api/listening/stop", response_model=ListenerStatus)
async def stop_listening() -> ListenerStatus:
    await voice.stop()
    return ListenerStatus(running=voice.running, mode="voice")


@app.post("/api/gesture/start", response_model=ListenerStatus)
async def start_gesture() -> ListenerStatus:
    await gesture.start()
    return ListenerStatus(running=gesture.running, mode="gesture")


@app.post("/api/gesture/stop", response_model=ListenerStatus)
async def stop_gesture() -> ListenerStatus:
    await gesture.stop()
    return ListenerStatus(running=gesture.running, mode="gesture")


@app.post("/api/speaker/enroll")
async def enroll_speaker() -> dict:
    feature = speaker.enroll_from_microphone()
    await hub.log("info", "声纹样本已录制", "speaker")
    return {"ok": True, "feature_length": len(feature)}


@app.post("/api/schedule")
async def schedule_action(request: ScheduleRequest) -> dict:
    job_id = await scheduler.schedule(request)
    return {"ok": True, "job_id": job_id}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket) -> None:
    await hub.connect(websocket)
    await websocket.send_json({"type": "snapshot", "payload": {"devices": [device.model_dump() for device in devices.list_devices()]}})
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        await hub.disconnect(websocket)

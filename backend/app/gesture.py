import asyncio
import time

from .commands import CommandProcessor
from .events import EventHub
from .schemas import Intent
from .settings import Settings


class GestureListener:
    def __init__(self, settings: Settings, hub: EventHub) -> None:
        self.settings = settings
        self.hub = hub
        self.processor: CommandProcessor | None = None
        self._task: asyncio.Task | None = None
        self._running = False
        self._last_action_at = 0.0
        self._last_frame_event_at = 0.0

    def bind_processor(self, processor: CommandProcessor) -> None:
        self.processor = processor

    @property
    def running(self) -> bool:
        return self._running

    async def start(self) -> None:
        if self._running:
            return
        if not self.processor:
            raise RuntimeError("Gesture processor not bound")
        self._running = True
        self._task = asyncio.create_task(self._loop())
        await self.hub.log("info", "手势识别已启动", "gesture")

    async def stop(self) -> None:
        self._running = False
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        await self.hub.log("info", "手势识别已停止", "gesture")

    async def _loop(self) -> None:
        import cv2
        import mediapipe as mp

        cap = cv2.VideoCapture(self.settings.gesture_camera_index)
        if not cap.isOpened():
            self._running = False
            await self.hub.log("error", "摄像头不可用，手势识别无法启动", "gesture")
            return
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, self.settings.gesture_frame_width)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, self.settings.gesture_frame_height)
        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        hands = mp.solutions.hands.Hands(max_num_hands=1, min_detection_confidence=0.65, min_tracking_confidence=0.55)
        try:
            while self._running:
                started_at = time.time()
                ok, frame = await asyncio.to_thread(cap.read)
                if not ok:
                    await asyncio.sleep(0.2)
                    continue
                frame = cv2.resize(frame, (self.settings.gesture_frame_width, self.settings.gesture_frame_height))
                frame = cv2.flip(frame, 1)
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                result = await asyncio.to_thread(hands.process, rgb)
                if result.multi_hand_landmarks:
                    landmarks = result.multi_hand_landmarks[0].landmark
                    gesture = self._classify(landmarks)
                    await self._publish_frame(gesture or "unknown", landmarks)
                    if gesture:
                        await self._handle_gesture(gesture)
                else:
                    await self._publish_empty_frame()
                elapsed = time.time() - started_at
                await asyncio.sleep(max(0.0, self.settings.gesture_process_interval_seconds - elapsed))
        finally:
            cap.release()
            hands.close()

    async def _handle_gesture(self, gesture: str) -> None:
        now = time.time()
        if now - self._last_action_at < self.settings.gesture_cooldown_seconds:
            return
        self._last_action_at = now
        mapping = {
            "palm": Intent(device_id="living_room_light", action="on", reply="识别到手掌，已打开客厅灯。"),
            "fist": Intent(device_id="living_room_light", action="off", reply="识别到拳头，已关闭客厅灯。"),
            "victory": Intent(device_id="television", action="toggle", reply="识别到比耶手势，已切换电视状态。"),
            "point_up": Intent(device_id="curtain", action="toggle", reply="识别到举手，已切换窗帘状态。"),
        }
        intent = mapping.get(gesture)
        if intent and self.processor:
            await self.hub.publish("gesture", {"name": gesture})
            await self.processor.execute_intent(intent, source="gesture")

    async def _publish_frame(self, gesture: str, landmarks) -> None:
        now = time.time()
        if now - self._last_frame_event_at < self.settings.gesture_preview_interval_seconds:
            return
        self._last_frame_event_at = now
        points = [{"x": self._clip(point.x), "y": self._clip(point.y)} for point in landmarks]
        xs = [point["x"] for point in points]
        ys = [point["y"] for point in points]
        padding = 0.04
        bbox = {
            "x": self._clip(min(xs) - padding),
            "y": self._clip(min(ys) - padding),
            "width": self._clip(max(xs) - min(xs) + padding * 2),
            "height": self._clip(max(ys) - min(ys) + padding * 2),
        }
        await self.hub.publish("gesture_frame", {"name": gesture, "bbox": bbox, "landmarks": points})

    async def _publish_empty_frame(self) -> None:
        now = time.time()
        if now - self._last_frame_event_at < 0.5:
            return
        self._last_frame_event_at = now
        await self.hub.publish("gesture_frame", {"name": "none", "bbox": None, "landmarks": []})

    @staticmethod
    def _clip(value: float) -> float:
        return round(max(0.0, min(1.0, float(value))), 4)

    @staticmethod
    def _classify(landmarks) -> str | None:
        # Finger tips above PIP joints usually means extended fingers in an upright hand.
        tips = [8, 12, 16, 20]
        pips = [6, 10, 14, 18]
        extended = [landmarks[tip].y < landmarks[pip].y for tip, pip in zip(tips, pips)]
        count = sum(extended)
        if count >= 4:
            return "palm"
        if count == 0:
            return "fist"
        if extended[0] and extended[1] and not extended[2] and not extended[3]:
            return "victory"
        if extended[0] and count == 1:
            return "point_up"
        return None

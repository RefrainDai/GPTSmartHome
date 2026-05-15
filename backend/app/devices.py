from copy import deepcopy
from typing import Any

from .events import utc_now
from .schemas import Device, Intent


class DeviceStateManager:
    def __init__(self) -> None:
        now = utc_now()
        self._devices: dict[str, Device] = {
            "living_room_light": Device(id="living_room_light", name="客厅主灯", type="light", room="客厅", level=65, updated_at=now),
            "bedroom_light": Device(id="bedroom_light", name="卧室灯", type="light", room="卧室", level=45, updated_at=now),
            "air_conditioner": Device(id="air_conditioner", name="空调", type="climate", room="客厅", temperature=26, mode="cool", updated_at=now),
            "curtain": Device(id="curtain", name="智能窗帘", type="curtain", room="客厅", level=0, updated_at=now),
            "television": Device(id="television", name="电视", type="media", room="客厅", updated_at=now),
            "fan": Device(id="fan", name="风扇", type="fan", room="客厅", level=1, updated_at=now),
            "humidifier": Device(id="humidifier", name="加湿器", type="humidifier", room="卧室", level=40, updated_at=now),
            "door_lock": Device(id="door_lock", name="门锁", type="lock", room="玄关", locked=True, updated_at=now),
            "robot_vacuum": Device(id="robot_vacuum", name="扫地机器人", type="robot", room="全屋", updated_at=now),
        }

    def list_devices(self) -> list[Device]:
        return [deepcopy(device) for device in self._devices.values()]

    def get(self, device_id: str) -> Device:
        if device_id not in self._devices:
            raise KeyError(f"Unknown device: {device_id}")
        return deepcopy(self._devices[device_id])

    def apply_intent(self, intent: Intent) -> list[Device]:
        if intent.action.startswith("scene_"):
            return self.apply_scene(intent.action)
        if not intent.device_id:
            raise ValueError("Intent requires device_id")
        return [self.apply_action(intent.device_id, intent.action, intent.params)]

    def apply_action(self, device_id: str, action: str, params: dict[str, Any] | None = None) -> Device:
        params = params or {}
        if device_id not in self._devices:
            raise KeyError(f"Unknown device: {device_id}")

        device = self._devices[device_id]
        normalized = self._normalize_action(action, device)
        if normalized == "on":
            device.is_on = True
            if device.type == "curtain":
                device.level = 100
            if device.type == "lock":
                device.locked = False
        elif normalized == "off":
            device.is_on = False
            if device.type == "curtain":
                device.level = 0
            if device.type == "lock":
                device.locked = True
        elif normalized == "toggle":
            return self.apply_action(device_id, "off" if device.is_on else "on", params)
        elif normalized == "set_level":
            device.level = int(max(0, min(100, params.get("level", device.level or 0))))
            device.is_on = device.level > 0
        elif normalized == "set_temperature":
            device.temperature = int(max(16, min(30, params.get("temperature", device.temperature or 26))))
            device.is_on = True
        elif normalized == "set_mode":
            device.mode = str(params.get("mode", device.mode or "auto"))
            device.is_on = True
        else:
            raise ValueError(f"Unsupported action: {action}")

        device.updated_at = utc_now()
        return deepcopy(device)

    def apply_scene(self, scene: str) -> list[Device]:
        if scene == "scene_movie":
            operations = [
                ("living_room_light", "set_level", {"level": 15}),
                ("curtain", "off", {}),
                ("television", "on", {}),
                ("fan", "set_level", {"level": 30}),
            ]
        elif scene == "scene_sleep":
            operations = [
                ("living_room_light", "off", {}),
                ("bedroom_light", "set_level", {"level": 10}),
                ("curtain", "off", {}),
                ("television", "off", {}),
                ("air_conditioner", "set_temperature", {"temperature": 26}),
            ]
        elif scene == "scene_home":
            operations = [
                ("door_lock", "on", {}),
                ("living_room_light", "on", {}),
                ("curtain", "on", {}),
                ("air_conditioner", "set_temperature", {"temperature": 24}),
            ]
        elif scene == "scene_away":
            operations = [(device_id, "off", {}) for device_id in self._devices if device_id != "door_lock"]
            operations.append(("door_lock", "off", {}))
        else:
            raise ValueError(f"Unsupported scene: {scene}")

        changed: list[Device] = []
        for device_id, action, params in operations:
            changed.append(self.apply_action(device_id, action, params))
        return changed

    @staticmethod
    def _normalize_action(action: str, device: Device) -> str:
        action = action.lower().strip()
        if device.type == "curtain":
            if action in {"open", "on"}:
                return "on"
            if action in {"close", "off"}:
                return "off"
        if device.type == "lock":
            if action in {"unlock", "open", "on"}:
                return "on"
            if action in {"lock", "close", "off"}:
                return "off"
        aliases = {
            "open": "on",
            "start": "on",
            "close": "off",
            "stop": "off",
            "brightness": "set_level",
            "temperature": "set_temperature",
        }
        return aliases.get(action, action)

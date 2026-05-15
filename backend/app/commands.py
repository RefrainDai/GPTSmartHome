import re

from .devices import DeviceStateManager
from .events import EventHub
from .llm import LLMIntentParser
from .schemas import CommandResult, Intent
from .tts import TTSService


DEVICE_ALIASES: dict[str, list[str]] = {
    "living_room_light": ["客厅灯", "主灯", "灯光", "灯"],
    "bedroom_light": ["卧室灯", "床头灯"],
    "air_conditioner": ["空调", "冷气", "暖气"],
    "curtain": ["窗帘", "帘子"],
    "television": ["电视", "电视机", "屏幕"],
    "fan": ["风扇", "电扇"],
    "humidifier": ["加湿器", "加湿"],
    "door_lock": ["门锁", "锁"],
    "robot_vacuum": ["扫地机器人", "机器人", "扫地"],
}


class CommandProcessor:
    def __init__(self, devices: DeviceStateManager, hub: EventHub, tts: TTSService, llm: LLMIntentParser) -> None:
        self.devices = devices
        self.hub = hub
        self.tts = tts
        self.llm = llm

    async def handle_text(self, text: str, source: str = "manual") -> CommandResult:
        normalized = text.strip()
        await self.hub.log("info", f"收到{source}指令：{normalized}", "command")
        intent = self._parse_local(normalized) or await self.llm.parse(normalized)
        if not intent:
            reply = "我没有理解这条指令，请换一种说法。"
            audio_url = await self.tts.synthesize(reply)
            await self.hub.publish("ai_reply", {"text": reply, "audio_url": audio_url})
            return CommandResult(ok=False, message=reply, audio_url=audio_url)
        return await self.execute_intent(intent, source=source, original_text=normalized)

    async def execute_intent(self, intent: Intent, source: str = "manual", original_text: str | None = None) -> CommandResult:
        try:
            changed = self.devices.apply_intent(intent)
        except Exception as exc:
            reply = f"执行失败：{exc}"
            await self.hub.log("error", reply, "command")
            audio_url = await self.tts.synthesize(reply)
            await self.hub.publish("ai_reply", {"text": reply, "audio_url": audio_url})
            return CommandResult(ok=False, message=reply, intent=intent, audio_url=audio_url)

        reply = intent.reply or self._default_reply(intent, changed)
        audio_url = await self.tts.synthesize(reply)
        for device in changed:
            await self.hub.publish(
                "device_updated",
                {
                    "device": device.model_dump(),
                    "source": source,
                    "action": intent.action,
                    "text": original_text,
                },
            )
        await self.hub.publish("ai_reply", {"text": reply, "audio_url": audio_url})
        await self.hub.log("info", reply, "assistant")
        return CommandResult(ok=True, message=reply, intent=intent, devices=changed, audio_url=audio_url)

    def _parse_local(self, text: str) -> Intent | None:
        compact = re.sub(r"\s+", "", text)
        if any(word in compact for word in ["观影", "看电影", "电影模式", "影院"]):
            return Intent(action="scene_movie", reply="观影模式已开启，灯光已调暗，电视已打开。")
        if any(word in compact for word in ["睡觉", "晚安", "睡眠", "休息"]):
            return Intent(action="scene_sleep", reply="睡眠模式已开启，祝你晚安。")
        if any(word in compact for word in ["回家", "我回来了", "到家"]):
            return Intent(action="scene_home", reply="欢迎回家，灯光和空调已经准备好。")
        if any(word in compact for word in ["离家", "出门", "不在家"]):
            return Intent(action="scene_away", reply="离家模式已开启，已关闭主要设备并锁门。")
        if any(word in compact for word in ["太暗", "好暗", "看不清"]):
            return Intent(device_id="living_room_light", action="on", reply="客厅灯已打开，房间亮起来了。")
        if any(word in compact for word in ["有点热", "太热", "好热"]):
            return Intent(device_id="air_conditioner", action="set_temperature", params={"temperature": 24}, reply="已打开空调，并调到二十四度。")
        if any(word in compact for word in ["有点冷", "太冷", "好冷"]):
            return Intent(device_id="air_conditioner", action="set_temperature", params={"temperature": 27}, reply="已将空调温度调到二十七度。")

        device_id = self._find_device(compact)
        if not device_id:
            return None
        action = self._find_action(compact)
        params: dict[str, int | str] = {}
        if not action:
            return None
        if action == "set_temperature":
            match = re.search(r"(\d{2})度?", compact)
            params["temperature"] = int(match.group(1)) if match else 24
        if action == "set_level":
            match = re.search(r"(\d{1,3})%?", compact)
            params["level"] = int(match.group(1)) if match else 70
        return Intent(device_id=device_id, action=action, params=params)

    @staticmethod
    def _find_action(text: str) -> str | None:
        if any(word in text for word in ["打开", "开启", "启动", "开一下", "解锁"]):
            return "on"
        if any(word in text for word in ["关闭", "关掉", "停止", "关上", "锁门", "上锁"]):
            return "off"
        if any(word in text for word in ["切换", "反转"]):
            return "toggle"
        if any(word in text for word in ["温度", "调到", "调成", "度"]):
            return "set_temperature"
        if any(word in text for word in ["亮度", "档位", "风速", "调亮", "调暗"]):
            return "set_level"
        return None

    @staticmethod
    def _find_device(text: str) -> str | None:
        for device_id, aliases in DEVICE_ALIASES.items():
            if any(alias in text for alias in aliases):
                return device_id
        return None

    @staticmethod
    def _default_reply(intent: Intent, changed: list) -> str:
        if intent.action.startswith("scene_"):
            return "场景模式已执行完成。"
        if not changed:
            return "指令已执行。"
        device = changed[0]
        action_text = {
            "on": "打开",
            "off": "关闭",
            "toggle": "切换",
            "set_level": "调整",
            "set_temperature": "调整温度",
            "set_mode": "切换模式",
        }.get(intent.action, intent.action)
        return f"已{action_text}{device.name}。"

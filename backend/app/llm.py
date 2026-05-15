import json
from typing import Any

from openai import AsyncOpenAI

from .schemas import Intent
from .settings import Settings


SYSTEM_PROMPT = """
你是智能家居意图解析器。只能输出 JSON，不要输出解释。
可用设备: living_room_light, bedroom_light, air_conditioner, curtain, television, fan, humidifier, door_lock, robot_vacuum。
可用动作: on, off, toggle, set_level, set_temperature, set_mode, scene_movie, scene_sleep, scene_home, scene_away。
JSON 格式: {"device_id": string|null, "action": string, "params": object, "confidence": number, "reply": string}。
如果是场景动作，device_id 为 null。
""".strip()


class LLMIntentParser:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.client: AsyncOpenAI | None = None
        if settings.llm_enabled and settings.llm_api_key:
            self.client = AsyncOpenAI(api_key=settings.llm_api_key, base_url=settings.llm_base_url)

    async def parse(self, text: str) -> Intent | None:
        if not self.client:
            return None
        try:
            response = await self.client.chat.completions.create(
                model=self.settings.llm_model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": text},
                ],
                temperature=0.1,
                response_format={"type": "json_object"},
                timeout=12,
            )
            content = response.choices[0].message.content or "{}"
            data: dict[str, Any] = json.loads(content)
            return Intent(**data)
        except Exception:
            return None

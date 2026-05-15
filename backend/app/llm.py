import json
from typing import Any

import httpx

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

    async def parse(self, text: str) -> Intent | None:
        if not self.settings.llm_enabled or not self.settings.llm_api_key:
            return None
        try:
            payload: dict[str, Any] = {
                "model": self.settings.llm_model,
                "messages": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": text},
                ],
                "temperature": 0.1,
                "response_format": {"type": "json_object"},
                "max_completion_tokens": self.settings.llm_max_completion_tokens,
            }
            headers = self._headers()
            async with httpx.AsyncClient(timeout=12) as client:
                response = await client.post(self._chat_completions_url(), headers=headers, json=payload)
                response.raise_for_status()
            content = response.json()["choices"][0]["message"].get("content") or "{}"
            data: dict[str, Any] = json.loads(content)
            return Intent(**data)
        except Exception:
            return None

    def _headers(self) -> dict[str, str]:
        header = self.settings.llm_auth_header.strip().lower()
        if header == "api-key":
            return {"api-key": self.settings.llm_api_key, "Content-Type": "application/json"}
        return {"Authorization": f"Bearer {self.settings.llm_api_key}", "Content-Type": "application/json"}

    def _chat_completions_url(self) -> str:
        return f"{self.settings.llm_base_url.rstrip('/')}/chat/completions"

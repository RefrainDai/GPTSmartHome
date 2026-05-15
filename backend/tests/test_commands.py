import asyncio

from app.commands import CommandProcessor
from app.devices import DeviceStateManager
from app.events import EventHub
from app.llm import LLMIntentParser
from app.settings import get_settings
from app.tts import TTSService


def test_local_command_turns_on_light():
    asyncio.run(_test_local_command_turns_on_light())


async def _test_local_command_turns_on_light():
    settings = get_settings()
    settings.tts_enabled = False
    processor = CommandProcessor(DeviceStateManager(), EventHub(), TTSService(settings), LLMIntentParser(settings))

    result = await processor.handle_text("打开客厅灯")

    assert result.ok
    assert result.devices[0].id == "living_room_light"
    assert result.devices[0].is_on is True


def test_scene_movie_controls_tv():
    asyncio.run(_test_scene_movie_controls_tv())


async def _test_scene_movie_controls_tv():
    settings = get_settings()
    settings.tts_enabled = False
    processor = CommandProcessor(DeviceStateManager(), EventHub(), TTSService(settings), LLMIntentParser(settings))

    result = await processor.handle_text("进入观影模式")

    assert result.ok
    assert any(device.id == "television" and device.is_on for device in result.devices)

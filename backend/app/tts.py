import asyncio
import time
from pathlib import Path

from .settings import Settings, TTS_DIR


class TTSService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        TTS_DIR.mkdir(parents=True, exist_ok=True)

    async def synthesize(self, text: str) -> str | None:
        if not self.settings.tts_enabled or self.settings.tts_provider.lower() == "none":
            return None
        provider = self.settings.tts_provider.lower()
        extension = "wav" if provider == "pyttsx3" else "mp3"
        filename = f"assistant_{int(time.time() * 1000)}.{extension}"
        output = TTS_DIR / filename
        try:
            if provider == "edge":
                await self._edge_tts(text, output)
            elif provider == "pyttsx3":
                await asyncio.to_thread(self._pyttsx3_tts, text, output)
            else:
                return None
        except Exception:
            return None
        if output.exists():
            return f"/assets/tts/{filename}"
        return None

    async def _edge_tts(self, text: str, output: Path) -> None:
        import edge_tts

        communicate = edge_tts.Communicate(text, self.settings.tts_voice)
        await communicate.save(str(output))

    @staticmethod
    def _pyttsx3_tts(text: str, output: Path) -> None:
        import pyttsx3

        engine = pyttsx3.init()
        engine.save_to_file(text, str(output))
        engine.runAndWait()

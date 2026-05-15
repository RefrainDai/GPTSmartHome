import asyncio
import os
import tempfile

import numpy as np
import sounddevice as sd
import soundfile as sf

from .audio import bandpass_filter, rms, spectrum, waveform
from .commands import CommandProcessor
from .events import EventHub
from .settings import Settings
from .speaker import SpeakerVerifier


class VoiceListener:
    def __init__(self, settings: Settings, hub: EventHub, speaker: SpeakerVerifier) -> None:
        self.settings = settings
        self.hub = hub
        self.speaker = speaker
        self.processor: CommandProcessor | None = None
        self._task: asyncio.Task | None = None
        self._running = False

    def bind_processor(self, processor: CommandProcessor) -> None:
        self.processor = processor

    @property
    def running(self) -> bool:
        return self._running

    async def start(self) -> None:
        if self._running:
            return
        if not self.processor:
            raise RuntimeError("Voice processor not bound")
        self._running = True
        self._task = asyncio.create_task(self._loop())
        await self.hub.publish("voice_status", {"running": True, "state": "listening"})
        await self.hub.log("info", "语音监听已启动", "voice")

    async def stop(self) -> None:
        self._running = False
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        await self.hub.publish("voice_status", {"running": False, "state": "stopped"})
        await self.hub.log("info", "语音监听已停止", "voice")

    async def _loop(self) -> None:
        while self._running:
            try:
                samples = await self._record_chunk()
                filtered = bandpass_filter(samples, self.settings.voice_sample_rate)
                volume = rms(filtered)
                if volume < self.settings.voice_rms_threshold:
                    await asyncio.sleep(0.1)
                    continue

                await self.hub.publish("voice_status", {"running": True, "state": "recognizing"})
                allowed, score = self.speaker.verify(filtered, self.settings.voice_sample_rate)
                if not allowed:
                    await self.hub.log("warning", f"声纹校验失败，得分 {score:.2f}", "speaker")
                    continue

                text = await asyncio.to_thread(self._recognize, filtered)
                if text:
                    await self.hub.publish("voice_text", {"text": text})
                    await self.processor.handle_text(text, source="voice")
                await self.hub.publish("voice_status", {"running": True, "state": "listening"})
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                await self.hub.log("error", f"语音监听异常：{exc}", "voice")
                await asyncio.sleep(1.0)

    async def _record_chunk(self) -> np.ndarray:
        sample_rate = self.settings.voice_sample_rate
        total_frames = int(sample_rate * self.settings.voice_chunk_seconds)
        block_frames = max(512, int(sample_rate * self.settings.voice_visual_interval_seconds))
        captured: list[np.ndarray] = []
        frames_read = 0
        with sd.InputStream(samplerate=sample_rate, channels=1, dtype="float32", blocksize=block_frames) as stream:
            while self._running and frames_read < total_frames:
                data, overflowed = await asyncio.to_thread(stream.read, min(block_frames, total_frames - frames_read))
                segment = data.reshape(-1).astype(np.float32)
                captured.append(segment)
                frames_read += segment.size
                filtered = bandpass_filter(segment, sample_rate)
                await self.hub.publish(
                    "voice_frame",
                    {
                        "volume": round(rms(filtered), 5),
                        "threshold": self.settings.voice_rms_threshold,
                        "overflowed": bool(overflowed),
                        "spectrum": spectrum(filtered, sample_rate, self.settings.voice_spectrum_bins),
                        "waveform": waveform(filtered, self.settings.voice_waveform_points),
                    },
                )
        if not captured:
            return np.array([], dtype=np.float32)
        return np.concatenate(captured)

    def _recognize(self, samples: np.ndarray) -> str | None:
        if self.settings.voice_recognition_mode.lower() == "mock":
            return "打开客厅灯"
        import speech_recognition as sr

        recognizer = sr.Recognizer()
        fd, path = tempfile.mkstemp(suffix=".wav")
        os.close(fd)
        try:
            sf.write(path, samples, self.settings.voice_sample_rate)
            with sr.AudioFile(path) as source:
                audio = recognizer.record(source)
            try:
                return recognizer.recognize_google(audio, language=self.settings.voice_language)
            except sr.UnknownValueError:
                return None
            except sr.RequestError as exc:
                raise RuntimeError(f"在线语音识别失败：{exc}") from exc
        finally:
            try:
                os.remove(path)
            except OSError:
                pass

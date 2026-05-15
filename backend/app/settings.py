from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


ROOT_DIR = Path(__file__).resolve().parents[2]
ASSETS_DIR = ROOT_DIR / "assets"
TTS_DIR = ASSETS_DIR / "tts"
DATA_DIR = ROOT_DIR / "backend" / "data"


class Settings(BaseSettings):
    app_host: str = "127.0.0.1"
    app_port: int = 8000
    frontend_origin: str = "*"

    voice_recognition_mode: str = "google"
    voice_language: str = "zh-CN"
    voice_sample_rate: int = 16000
    voice_chunk_seconds: float = 2.8
    voice_rms_threshold: float = 0.015
    voice_visual_interval_seconds: float = 0.16
    voice_spectrum_bins: int = 64
    voice_waveform_points: int = 96

    llm_enabled: bool = False
    llm_api_key: str = ""
    llm_base_url: str = "https://api.openai.com/v1"
    llm_model: str = "gpt-4o-mini"
    llm_auth_header: str = "authorization"
    llm_max_completion_tokens: int = 512

    tts_enabled: bool = True
    tts_provider: str = "edge"
    tts_voice: str = "zh-CN-XiaoxiaoNeural"

    speaker_verify_enabled: bool = False
    speaker_threshold: float = 0.82

    gesture_camera_index: int = 0
    gesture_cooldown_seconds: float = 2.0
    gesture_frame_width: int = 424
    gesture_frame_height: int = 240
    gesture_process_interval_seconds: float = 0.12
    gesture_preview_interval_seconds: float = 0.20
    gesture_use_opencl: bool = True
    gesture_video_preview_enabled: bool = True
    gesture_video_interval_seconds: float = 0.30
    gesture_video_jpeg_quality: int = 55
    gesture_stable_frames: int = 2

    model_config = SettingsConfigDict(env_file=ROOT_DIR / ".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    TTS_DIR.mkdir(parents=True, exist_ok=True)
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    return Settings()

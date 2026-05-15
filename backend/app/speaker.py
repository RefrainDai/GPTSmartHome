import json

import numpy as np
import sounddevice as sd

from .audio import cosine_similarity, voiceprint_feature
from .settings import DATA_DIR, Settings


class SpeakerVerifier:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.profile_path = DATA_DIR / "speaker_profile.json"
        self.template = self._load_template()

    def enabled(self) -> bool:
        return self.settings.speaker_verify_enabled and self.template is not None

    def verify(self, samples: np.ndarray, sample_rate: int) -> tuple[bool, float]:
        if not self.settings.speaker_verify_enabled or not self.template:
            return True, 1.0
        score = cosine_similarity(voiceprint_feature(samples, sample_rate), self.template)
        return score >= self.settings.speaker_threshold, score

    def enroll_from_microphone(self, seconds: float = 4.0) -> list[float]:
        samples = sd.rec(int(seconds * self.settings.voice_sample_rate), samplerate=self.settings.voice_sample_rate, channels=1, dtype="float32")
        sd.wait()
        feature = voiceprint_feature(samples.reshape(-1), self.settings.voice_sample_rate)
        DATA_DIR.mkdir(parents=True, exist_ok=True)
        self.profile_path.write_text(json.dumps({"feature": feature}, ensure_ascii=False, indent=2), encoding="utf-8")
        self.template = feature
        return feature

    def _load_template(self) -> list[float] | None:
        if not self.profile_path.exists():
            return None
        try:
            data = json.loads(self.profile_path.read_text(encoding="utf-8"))
            return list(data.get("feature") or [])
        except Exception:
            return None

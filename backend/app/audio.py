import numpy as np
from scipy import signal


def bandpass_filter(samples: np.ndarray, sample_rate: int) -> np.ndarray:
    if samples.size == 0:
        return samples
    low = 120 / (sample_rate / 2)
    high = min(3600 / (sample_rate / 2), 0.98)
    b, a = signal.butter(4, [low, high], btype="band")
    return signal.lfilter(b, a, samples).astype(np.float32)


def rms(samples: np.ndarray) -> float:
    if samples.size == 0:
        return 0.0
    return float(np.sqrt(np.mean(np.square(samples))))


def spectrum(samples: np.ndarray, sample_rate: int, bins: int = 64) -> list[float]:
    if samples.size == 0:
        return [0.0] * bins
    window = np.hanning(samples.size)
    fft = np.abs(np.fft.rfft(samples * window))
    fft = fft[: min(fft.size, bins)]
    if fft.size < bins:
        fft = np.pad(fft, (0, bins - fft.size))
    max_value = float(np.max(fft)) or 1.0
    return [round(float(value / max_value), 4) for value in fft[:bins]]


def waveform(samples: np.ndarray, points: int = 96) -> list[float]:
    if samples.size == 0:
        return [0.0] * points
    chunks = np.array_split(samples, points)
    values = np.array([float(np.mean(chunk)) if chunk.size else 0.0 for chunk in chunks], dtype=np.float32)
    max_value = float(np.max(np.abs(values))) or 1.0
    return [round(float(value / max_value), 4) for value in values]


def voiceprint_feature(samples: np.ndarray, sample_rate: int) -> list[float]:
    filtered = bandpass_filter(samples, sample_rate)
    frequencies, _, spec = signal.spectrogram(filtered, fs=sample_rate, nperseg=512, noverlap=256)
    if spec.size == 0:
        return [0.0] * 16
    bands = np.array_split(np.log1p(spec), 16, axis=0)
    feature = np.array([float(np.mean(band)) for band in bands], dtype=np.float32)
    norm = np.linalg.norm(feature) or 1.0
    return (feature / norm).round(5).tolist()


def cosine_similarity(a: list[float], b: list[float]) -> float:
    av = np.array(a, dtype=np.float32)
    bv = np.array(b, dtype=np.float32)
    denom = (np.linalg.norm(av) * np.linalg.norm(bv)) or 1.0
    return float(np.dot(av, bv) / denom)

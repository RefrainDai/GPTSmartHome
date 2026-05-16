from typing import Any


def run_diagnostics(max_camera_index: int = 0) -> dict[str, Any]:
    return {
        "cameras": _check_cameras(max_camera_index),
        "audio_inputs": _check_audio_inputs(),
        "packages": _check_packages(),
    }


def _check_cameras(max_camera_index: int) -> list[dict[str, Any]]:
    try:
        import cv2
    except Exception as exc:
        return [{"index": None, "ok": False, "error": f"OpenCV 导入失败：{exc}"}]

    backends = [
        ("dshow", getattr(cv2, "CAP_DSHOW", None)),
        ("default", None),
        ("msmf", getattr(cv2, "CAP_MSMF", None)),
    ]
    results: list[dict[str, Any]] = []
    for index in range(max_camera_index + 1):
        for name, backend in backends:
            if backend is None and name != "default":
                continue
            cap = cv2.VideoCapture(index) if backend is None else cv2.VideoCapture(index, backend)
            opened = bool(cap.isOpened())
            read = False
            shape = None
            if opened:
                read, frame = cap.read()
                if read and frame is not None:
                    shape = list(frame.shape)
            cap.release()
            if opened or read:
                results.append({"index": index, "backend": name, "opened": opened, "read": bool(read), "shape": shape})
            if read:
                break
    return results


def _check_audio_inputs() -> list[dict[str, Any]]:
    try:
        import sounddevice as sd
    except Exception as exc:
        return [{"index": None, "ok": False, "error": f"sounddevice 导入失败：{exc}"}]

    devices = sd.query_devices()
    inputs: list[dict[str, Any]] = []
    for index, device in enumerate(devices):
        if int(device.get("max_input_channels", 0)) > 0:
            inputs.append(
                {
                    "index": index,
                    "name": str(device.get("name", "")),
                    "hostapi": int(device.get("hostapi", -1)),
                    "channels": int(device.get("max_input_channels", 0)),
                    "default_samplerate": float(device.get("default_samplerate", 0)),
                }
            )
    return inputs


def _check_packages() -> dict[str, Any]:
    packages: dict[str, Any] = {}
    try:
        import cv2

        packages["opencv"] = {"version": cv2.__version__, "opencl": bool(cv2.ocl.haveOpenCL())}
    except Exception as exc:
        packages["opencv"] = {"error": str(exc)}
    try:
        import mediapipe as mp

        packages["mediapipe"] = {"version": mp.__version__, "has_solutions": bool(hasattr(mp, "solutions"))}
    except Exception as exc:
        packages["mediapipe"] = {"error": str(exc)}
    return packages

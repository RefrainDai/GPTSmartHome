from types import SimpleNamespace

from app.gesture import GestureListener


def _landmarks(extended: list[bool]):
    points = [SimpleNamespace(x=0.0, y=0.0, z=0.0) for _ in range(21)]
    fingers = [(8, 6), (12, 10), (16, 14), (20, 18)]
    for index, (tip_idx, pip_idx) in enumerate(fingers):
        base_x = 0.1 + index * 0.1
        points[pip_idx] = SimpleNamespace(x=base_x, y=0.2, z=0.0)
        tip_y = 0.42 if extended[index] else 0.12
        points[tip_idx] = SimpleNamespace(x=base_x, y=tip_y, z=0.0)
    return points


def test_classifies_palm():
    assert GestureListener._classify(_landmarks([True, True, True, True])) == "palm"


def test_classifies_fist():
    assert GestureListener._classify(_landmarks([False, False, False, False])) == "fist"


def test_classifies_victory():
    assert GestureListener._classify(_landmarks([True, True, False, False])) == "victory"


def test_classifies_point_up():
    assert GestureListener._classify(_landmarks([True, False, False, False])) == "point_up"

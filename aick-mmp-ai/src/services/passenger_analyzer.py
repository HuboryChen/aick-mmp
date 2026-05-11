from dataclasses import dataclass
from typing import Optional

from src.services.detector import Detection
from src.services.tracker import ByteTrack, TrackedObject


@dataclass
class PassengerCount:
    enter_count: int = 0
    exit_count: int = 0
    inside_count: int = 0
    zone_counts: dict[str, int] = None


class PassengerAnalyzer:
    """Counts people entering/exiting across a virtual line (y = line_y)."""

    def __init__(self, line_y: float = 0.5, person_class_id: int = 0):
        self._line_y = line_y
        self._person_class_id = person_class_id
        self._tracker = ByteTrack()
        self._prev_centers: dict[int, float] = {}  # track_id -> center_y
        self._enter_count = 0
        self._exit_count = 0

    def _get_center(self, obj: TrackedObject) -> tuple[float, float]:
        cx = (obj.x1 + obj.x2) / 2
        cy = (obj.y1 + obj.y2) / 2
        return cx, cy

    def update(self, detections: list[Detection]) -> PassengerCount:
        # Filter persons only
        person_dets = [d for d in detections if d.class_id == self._person_class_id]
        tracks = self._tracker.update(person_dets)

        for track in tracks:
            _, cy = self._get_center(track)
            prev_cy = self._prev_centers.get(track.track_id)
            if prev_cy is not None:
                # Crossed virtual line downward = enter
                if prev_cy < self._line_y and cy >= self._line_y:
                    self._enter_count += 1
                # Crossed upward = exit
                elif prev_cy >= self._line_y and cy < self._line_y:
                    self._exit_count += 1
            self._prev_centers[track.track_id] = cy

        return PassengerCount(
            enter_count=self._enter_count,
            exit_count=self._exit_count,
            inside_count=self._enter_count - self._exit_count,
        )

    def reset(self):
        self._tracker = ByteTrack()
        self._prev_centers.clear()
        self._enter_count = 0
        self._exit_count = 0

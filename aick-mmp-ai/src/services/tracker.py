from dataclasses import dataclass
from typing import Optional


@dataclass
class TrackedObject:
    track_id: int
    class_id: int
    x1: float
    y1: float
    x2: float
    y2: float
    lost: int = 0  # frames since last match


class ByteTrack:
    """Simplified ByteTrack: associates detections across frames via IoU."""

    def __init__(self, iou_threshold: float = 0.3, max_lost: int = 30):
        self._iou_threshold = iou_threshold
        self._max_lost = max_lost
        self._tracks: dict[int, TrackedObject] = {}
        self._next_id = 1

    def _iou(self, a: TrackedObject, b: TrackedObject) -> float:
        xi1 = max(a.x1, b.x1)
        yi1 = max(a.y1, b.y1)
        xi2 = min(a.x2, b.x2)
        yi2 = min(a.y2, b.y2)
        inter = max(0, xi2 - xi1) * max(0, yi2 - yi1)
        a_area = (a.x2 - a.x1) * (a.y2 - a.y1)
        b_area = (b.x2 - b.x1) * (b.y2 - b.y1)
        union = a_area + b_area - inter
        return inter / union if union > 0 else 0.0

    def update(self, detections: list) -> list[TrackedObject]:
        """Match detections to existing tracks, return current tracked objects."""
        matched = set()
        for det in detections:
            best_iou = self._iou_threshold
            best_id = None
            for tid, track in self._tracks.items():
                if tid in matched:
                    continue
                if track.class_id != det.class_id:
                    continue
                track_box = TrackedObject(track.track_id, track.class_id,
                                           track.x1, track.y1, track.x2, track.y2)
                iou = self._iou(track_box, det)
                if iou > best_iou:
                    best_iou = iou
                    best_id = tid
            if best_id is not None:
                self._tracks[best_id].x1 = det.x1
                self._tracks[best_id].y1 = det.y1
                self._tracks[best_id].x2 = det.x2
                self._tracks[best_id].y2 = det.y2
                self._tracks[best_id].lost = 0
                matched.add(best_id)
            else:
                tid = self._next_id
                self._next_id += 1
                self._tracks[tid] = TrackedObject(tid, det.class_id,
                                                   det.x1, det.y1, det.x2, det.y2)
                matched.add(tid)

        # Increment lost for unmatched tracks
        for tid, track in self._tracks.items():
            if tid not in matched:
                track.lost += 1

        # Remove stale tracks
        self._tracks = {tid: t for tid, t in self._tracks.items()
                        if t.lost < self._max_lost}

        return list(self._tracks.values())

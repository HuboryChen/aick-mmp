from dataclasses import dataclass
from src.services.tracker import ByteTrack


@dataclass
class _Detection:
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float
    class_id: int


def test_bytetrack_new_object():
    tracker = ByteTrack(iou_threshold=0.3)
    dets = [_Detection(0, 0, 10, 20, 0.9, 0)]
    tracks = tracker.update(dets)
    assert len(tracks) == 1
    assert tracks[0].track_id == 1
    assert tracks[0].lost == 0


def test_bytetrack_reuse_track():
    tracker = ByteTrack(iou_threshold=0.3)
    dets1 = [_Detection(0, 0, 10, 20, 0.9, 0)]
    tracks1 = tracker.update(dets1)
    tid = tracks1[0].track_id

    dets2 = [_Detection(1, 1, 11, 21, 0.9, 0)]
    tracks2 = tracker.update(dets2)
    assert tracks2[0].track_id == tid  # reused


def test_bytetrack_stale_removed():
    tracker = ByteTrack(iou_threshold=0.3, max_lost=2)
    tracker.update([_Detection(0, 0, 10, 20, 0.9, 0)])
    tracker.update([])
    tracker.update([])
    tracks = tracker.update([])
    assert len(tracks) == 0  # lost > max_lost

import time
from src.services.behavior_engine import BehaviorEngine, EventType, EventLevel
from src.services.tracker import TrackedObject


def test_loitering():
    engine = BehaviorEngine()
    engine._loitering_threshold = 0
    now = time.time()
    tracks = [TrackedObject(track_id=1, class_id=0, x1=0, y1=0, x2=10, y2=20)]
    engine.update(tracks, now)  # register appearance
    alerts = engine.update(tracks, now + 31)
    loitering = [a for a in alerts if a.event_type == EventType.LOITERING]
    assert len(loitering) == 1


def test_intrusion():
    engine = BehaviorEngine()
    engine.set_restricted_zones([(5, 5, 15, 15)])
    tracks = [TrackedObject(track_id=1, class_id=0, x1=6, y1=6, x2=14, y2=14)]
    alerts = engine.update(tracks, time.time())
    intrusion = [a for a in alerts if a.event_type == EventType.INTRUSION]
    assert len(intrusion) == 1


def test_gathering():
    engine = BehaviorEngine()
    engine._gathering_threshold = 3
    tracks = [
        TrackedObject(track_id=i, class_id=0, x1=0, y1=0, x2=1, y2=1)
        for i in range(4)
    ]
    alerts = engine.update(tracks, time.time())
    gathering = [a for a in alerts if a.event_type == EventType.GATHERING]
    assert len(gathering) == 1


def test_fall():
    engine = BehaviorEngine()
    tracks = [TrackedObject(track_id=1, class_id=0, x1=0, y1=0, x2=2, y2=0.2)]
    alerts = engine.update(tracks, time.time())
    fall = [a for a in alerts if a.event_type == EventType.FALL]
    assert len(fall) == 1

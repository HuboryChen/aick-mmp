import time
from collections import defaultdict
from dataclasses import dataclass
from enum import Enum
from typing import Optional


class EventType(str, Enum):
    LOITERING = "LOITERING"
    INTRUSION = "INTRUSION"
    GATHERING = "GATHERING"
    FALL = "FALL"


class EventLevel(str, Enum):
    INFO = "INFO"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"


@dataclass
class BehaviorAlert:
    event_type: EventType
    level: EventLevel
    position_x: float
    position_y: float
    width: float
    height: float
    description: str


class BehaviorEngine:
    def __init__(self):
        self._track_appearances: dict[int, float] = {}
        self._restricted_zones: list[tuple[float, float, float, float]] = []
        self._gathering_threshold = 5
        self._loitering_threshold = 30

    def set_restricted_zones(self, zones: list[tuple[float, float, float, float]]):
        self._restricted_zones = zones

    def update(self, tracks: list, current_time: float) -> list[BehaviorAlert]:
        alerts = []

        for track in tracks:
            cx = (track.x1 + track.x2) / 2
            cy = (track.y1 + track.y2) / 2
            box_w = track.x2 - track.x1
            box_h = track.y2 - track.y1

            # 1. Loitering
            if track.track_id not in self._track_appearances:
                self._track_appearances[track.track_id] = current_time
            else:
                elapsed = current_time - self._track_appearances[track.track_id]
                if elapsed >= self._loitering_threshold:
                    alerts.append(BehaviorAlert(
                        event_type=EventType.LOITERING,
                        level=EventLevel.WARNING,
                        position_x=cx, position_y=cy,
                        width=box_w, height=box_h,
                        description=f"Person {track.track_id} loitering for {elapsed:.0f}s",
                    ))

            # 2. Intrusion (in restricted zone)
            for zx1, zy1, zx2, zy2 in self._restricted_zones:
                if zx1 <= cx <= zx2 and zy1 <= cy <= zy2:
                    alerts.append(BehaviorAlert(
                        event_type=EventType.INTRUSION,
                        level=EventLevel.CRITICAL,
                        position_x=cx, position_y=cy,
                        width=box_w, height=box_h,
                        description=f"Person {track.track_id} entered restricted zone",
                    ))

            # 3. Fall detection (person bounding box is much wider than tall)
            if box_h > 0 and box_w / box_h > 1.5 and box_h < 0.3:
                alerts.append(BehaviorAlert(
                    event_type=EventType.FALL,
                    level=EventLevel.CRITICAL,
                    position_x=cx, position_y=cy,
                    width=box_w, height=box_h,
                    description=f"Person {track.track_id} may have fallen",
                ))

        # 4. Gathering
        if len(tracks) >= self._gathering_threshold:
            avg_x = sum((t.x1 + t.x2) / 2 for t in tracks) / len(tracks)
            avg_y = sum((t.y1 + t.y2) / 2 for t in tracks) / len(tracks)
            alerts.append(BehaviorAlert(
                event_type=EventType.GATHERING,
                level=EventLevel.WARNING,
                position_x=avg_x, position_y=avg_y,
                width=0, height=0,
                description=f"{len(tracks)} persons gathered",
            ))

        seen = set()
        unique = []
        for a in alerts:
            key = (a.event_type, int(a.position_x), int(a.position_y))
            if key not in seen:
                seen.add(key)
                unique.append(a)
        return unique

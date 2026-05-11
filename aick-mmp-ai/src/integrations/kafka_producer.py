import json
import logging
from typing import Optional

from aiokafka import AIOKafkaProducer

from src.core.config import settings


logger = logging.getLogger(__name__)


class ResultPublisher:
    def __init__(self):
        self._producer: Optional[AIOKafkaProducer] = None

    async def start(self):
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode(),
        )
        await self._producer.start()
        logger.info("Kafka producer started")

    async def stop(self):
        if self._producer:
            await self._producer.stop()

    async def publish_passenger(self, camera_id: str, stats) -> None:
        await self._producer.send_and_wait(
            settings.kafka_topic_passenger,
            key=camera_id.encode(),
            value={
                "camera_id": camera_id,
                "enter_count": stats.enter_count,
                "exit_count": stats.exit_count,
                "inside_count": stats.inside_count,
                "zone_stats": [
                    {"zone_name": z.zone_name, "person_count": z.person_count}
                    for z in stats.zone_stats
                ],
            },
        )

    async def publish_behavior(self, camera_id: str, event) -> None:
        await self._producer.send_and_wait(
            settings.kafka_topic_behavior,
            key=camera_id.encode(),
            value={
                "camera_id": camera_id,
                "event_type": event.event_type,
                "level": event.level,
                "position_x": event.position_x,
                "position_y": event.position_y,
                "width": event.width,
                "height": event.height,
                "snapshot_path": event.snapshot_path,
                "description": event.description,
            },
        )

    async def publish_vehicle(self, camera_id: str, record) -> None:
        await self._producer.send_and_wait(
            settings.kafka_topic_vehicle,
            key=record.plate_number.encode(),
            value={
                "camera_id": camera_id,
                "plate_number": record.plate_number,
                "plate_color": record.plate_color,
                "confidence": record.confidence,
                "snapshot_path": record.snapshot_path,
            },
        )

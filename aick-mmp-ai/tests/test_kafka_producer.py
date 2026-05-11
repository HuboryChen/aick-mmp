import pytest
from unittest.mock import AsyncMock

from src.integrations.kafka_producer import ResultPublisher
from src.integrations.frame_pb2 import PassengerStats, ZoneStats


@pytest.mark.asyncio
async def test_publish_passenger():
    publisher = ResultPublisher()
    publisher._producer = AsyncMock()
    publisher._producer.send_and_wait = AsyncMock()

    stats = PassengerStats(
        enter_count=5, exit_count=3, inside_count=10,
        zone_stats=[ZoneStats(zone_name="zone1", person_count=4)],
    )
    await publisher.publish_passenger("cam-001", stats)

    publisher._producer.send_and_wait.assert_called_once()
    call_args = publisher._producer.send_and_wait.call_args
    assert call_args[0][0] == "ai-passenger-stats"
    assert call_args[1]["key"] == b"cam-001"
    assert call_args[1]["value"]["enter_count"] == 5

import pytest
from src.integrations.grpc_server import GrpcServer


@pytest.mark.asyncio
async def test_grpc_server_start_stop():
    async def dummy_handler(camera_id, frame_data, timestamp, types):
        return None

    server = GrpcServer(dummy_handler)
    await server.start()
    await server.stop()

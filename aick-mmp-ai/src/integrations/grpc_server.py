import asyncio
import logging
from concurrent import futures
from typing import AsyncGenerator, Callable, Optional, Awaitable

import grpc

from src.core.config import settings
from src.integrations.frame_pb2 import AnalysisResult
from src.integrations.frame_pb2_grpc import (
    FrameAnalysisServicer,
    add_FrameAnalysisServicer_to_server,
)
from src.integrations.frame_pb2 import FrameRequest

logger = logging.getLogger(__name__)

FrameHandler = Callable[[str, bytes, float, list[str]], Awaitable[Optional[AnalysisResult]]]


class FrameAnalysisServicerImpl(FrameAnalysisServicer):
    def __init__(self, frame_handler: FrameHandler):
        self._frame_handler = frame_handler

    async def AnalyzeFrame(
        self, request_iterator: AsyncGenerator[FrameRequest, None],
    ) -> AsyncGenerator[AnalysisResult, None]:
        async for req in request_iterator:
            try:
                result = await self._frame_handler(
                    req.camera_id, req.frame_data, req.timestamp, list(req.analysis_types),
                )
                if result is not None:
                    yield result
            except Exception as e:
                logger.error("Frame analysis failed for %s: %s", req.camera_id, e)


class GrpcServer:
    def __init__(self, frame_handler: FrameHandler):
        self._server = grpc.aio.server(futures.ThreadPoolExecutor(max_workers=4))
        servicer = FrameAnalysisServicerImpl(frame_handler)
        add_FrameAnalysisServicer_to_server(servicer, self._server)

    async def start(self):
        address = f"0.0.0.0:{settings.grpc_port}"
        self._server.add_insecure_port(address)
        await self._server.start()
        logger.info("gRPC server listening on %s", address)

    async def stop(self):
        await self._server.stop(5)

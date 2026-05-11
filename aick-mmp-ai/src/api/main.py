import asyncio
import logging
import time
from contextlib import asynccontextmanager
from typing import AsyncGenerator, Optional

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException
from prometheus_client import Histogram, Counter, generate_latest
from starlette.responses import Response

from src.core.config import settings
from src.integrations.frame_pb2 import AnalysisResult, PassengerStats, BehaviorEvent, VehicleRecord, ZoneStats
from src.integrations.grpc_server import GrpcServer
from src.integrations.kafka_producer import ResultPublisher
from src.services.detector import YOLODetector
from src.services.passenger_analyzer import PassengerAnalyzer
from src.services.pose_estimator import PoseEstimator
from src.services.behavior_engine import BehaviorEngine
from src.services.plate_recognizer import PlateRecognizer

logger = logging.getLogger(__name__)

# Prometheus metrics
INFERENCE_DURATION = Histogram("ai_inference_seconds", "Inference time per frame", ["analysis_type"])
FRAMES_PROCESSED = Counter("ai_frames_total", "Total processed frames", ["analysis_type"])

# Global service instances
detector = YOLODetector()
passenger_analyzer = PassengerAnalyzer()
pose_estimator = PoseEstimator()
behavior_engine = BehaviorEngine()
plate_recognizer = PlateRecognizer()
publisher = ResultPublisher()

# State
camera_analyzers: dict[str, PassengerAnalyzer] = {}


async def handle_frame(
    camera_id: str, frame_data: bytes, timestamp: float, analysis_types: list[str],
) -> Optional[AnalysisResult]:
    """Orchestrates all requested analysis types on a single frame."""
    result = AnalysisResult(camera_id=camera_id, timestamp=int(timestamp * 1000))

    for atype in analysis_types:
        if atype == "passenger":
            with INFERENCE_DURATION.labels("passenger").time():
                detections = detector.detect(frame_data)
            FRAMES_PROCESSED.labels("passenger").inc()

            if camera_id not in camera_analyzers:
                camera_analyzers[camera_id] = PassengerAnalyzer()
            analyzer = camera_analyzers[camera_id]
            counts = analyzer.update(detections)
            stats = PassengerStats(
                enter_count=counts.enter_count,
                exit_count=counts.exit_count,
                inside_count=counts.inside_count,
            )
            result.passenger.CopyFrom(stats)
            await publisher.publish_passenger(camera_id, stats)

        elif atype == "behavior":
            with INFERENCE_DURATION.labels("behavior").time():
                detections = detector.detect(frame_data)
            FRAMES_PROCESSED.labels("behavior").inc()

            from src.services.tracker import ByteTrack
            tracker = ByteTrack()
            tracks = tracker.update(detections)
            alerts = behavior_engine.update(tracks, time.time())
            for alert in alerts:
                event = BehaviorEvent(
                    event_type=alert.event_type.value,
                    level=alert.level.value,
                    position_x=alert.position_x,
                    position_y=alert.position_y,
                    width=alert.width,
                    height=alert.height,
                    description=alert.description,
                )
                await publisher.publish_behavior(camera_id, event)
                if result.behavior.event_type == "":
                    result.behavior.CopyFrom(event)

        elif atype == "plate":
            with INFERENCE_DURATION.labels("plate").time():
                plates = plate_recognizer.recognize(frame_data)
            FRAMES_PROCESSED.labels("plate").inc()

            for plate in plates:
                record = VehicleRecord(
                    plate_number=plate.plate_number,
                    plate_color=plate.plate_color,
                    confidence=plate.confidence,
                )
                await publisher.publish_vehicle(camera_id, record)
                if result.vehicle.plate_number == "":
                    result.vehicle.CopyFrom(record)

    if result.HasField("passenger") or result.HasField("behavior") or result.HasField("vehicle"):
        return result
    return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    grpc_server = GrpcServer(handle_frame)
    await grpc_server.start()
    await publisher.start()
    logger.info("AI service started on gRPC:%d HTTP:%d", settings.grpc_port, settings.http_port)
    yield
    await publisher.stop()
    await grpc_server.stop()


app = FastAPI(title="AI Analysis Service", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "ok", "service": settings.service_name}


@app.get("/metrics")
async def metrics():
    return Response(content=generate_latest(), media_type="text/plain")


@app.get("/models")
async def list_models():
    import os
    models = []
    if os.path.exists(settings.model_dir):
        for f in os.listdir(settings.model_dir):
            if f.endswith(".onnx"):
                models.append(f)
    return {"models": models}

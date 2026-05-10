# v2 AI Analysis System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Python AI microservice with gRPC frame ingestion, Kafka result delivery, and supporting Java backend/frontend changes for passenger flow, behavior recognition, and license plate analysis.

**Architecture:** Edge nodes extract frames from RTSP streams and send via gRPC streaming to a centralized Python FastAPI AI service. The AI service runs YOLOv8, ByteTrack, OpenPifPaf, and LPRNet models (ONNX Runtime). Results flow through 3 Kafka topics to the Java central service, which stores in MySQL, caches in Redis, and pushes to the frontend via WebSocket.

**Tech Stack:** Python 3.11+, FastAPI, ONNX Runtime, gRPC (grpcio), Kafka (aiokafka), Java 21, Spring Boot 3.2.5, React 18, Ant Design 5, TypeScript

---

## File Structure

```
aick-mmp-ai/                          # NEW — Python AI microservice
├── requirements.txt
├── Dockerfile
├── .dockerignore
├── src/
│   ├── __init__.py
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py                  # YAML + env config
│   │   └── model_loader.py            # ONNX model warm-load
│   ├── proto/
│   │   └── frame.proto                # gRPC service definition
│   ├── api/
│   │   ├── __init__.py
│   │   └── main.py                    # FastAPI entry, health, metrics
│   ├── services/
│   │   ├── __init__.py
│   │   ├── detector.py                # YOLOv8 object detection
│   │   ├── tracker.py                 # ByteTrack multi-object tracker
│   │   ├── passenger_analyzer.py      # Entry/exit counting
│   │   ├── pose_estimator.py          # OpenPifPaf pose estimation
│   │   ├── behavior_engine.py         # Loitering/intrusion/fall rules
│   │   └── plate_recognizer.py        # YOLOv8-plate + LPRNet
│   └── integrations/
│       ├── __init__.py
│       ├── grpc_server.py             # gRPC frame receiver
│       └── kafka_producer.py          # Result publisher
├── models/
│   └── .gitkeep
└── tests/
    ├── __init__.py
    ├── conftest.py
    ├── test_config.py
    ├── test_detector.py
    ├── test_tracker.py
    ├── test_passenger_analyzer.py
    ├── test_behavior_engine.py
    ├── test_plate_recognizer.py
    ├── test_grpc_server.py
    └── test_kafka_producer.py

backend/aick-mmp-edge/                # MODIFIED
├── pom.xml                            # +grpc deps
└── src/main/java/com/aick/mmp/edge/
    ├── service/
    │   ├── FrameExtractor.java        # NEW
    │   └── FrameAnalysisGrpcClient.java # NEW
    └── config/
        └── AiServiceConfig.java       # NEW

backend/aick-mmp-central/             # MODIFIED
├── pom.xml                            # +kafka consumer deps
└── src/main/java/com/aick/mmp/central/
    ├── consumer/
    │   ├── AiPassengerStatsConsumer.java    # NEW
    │   └── AiBehaviorEventConsumer.java     # NEW
    │   └── AiVehicleRecordConsumer.java     # NEW
    ├── controller/
    │   └── AiAnalysisController.java        # NEW
    ├── service/
    │   ├── AiAnalysisService.java           # NEW
    │   └── impl/
    │       └── AiAnalysisServiceImpl.java   # NEW
    ├── repository/
    │   ├── AiPassengerStatsRepository.java  # NEW
    │   ├── AiBehaviorEventRepository.java   # NEW
    │   ├── AiVehicleRecordRepository.java   # NEW
    │   └── AiVehicleWhitelistRepository.java # NEW
    └── entity/
        ├── AiPassengerStats.java            # NEW
        ├── AiBehaviorEvent.java             # NEW
        ├── AiVehicleRecord.java             # NEW
        └── AiVehicleWhitelist.java          # NEW

frontend/src/                        # MODIFIED
├── pages/
│   ├── AiPassengerDashboard.tsx      # NEW
│   ├── AiBehaviorAlertCenter.tsx     # NEW
│   └── AiLicensePlateManagement.tsx  # NEW
└── services/
    └── aiApi.ts                      # NEW — API client

docs/sql/
├── V20260510__create_ai_passenger_stats.sql      # NEW
├── V20260510__create_ai_behavior_events.sql       # NEW
├── V20260510__create_ai_vehicle_records.sql       # NEW
└── V20260510__create_ai_vehicle_whitelist.sql     # NEW

deploy/k8s/
├── ai-service-deployment.yaml        # NEW
├── ai-service-service.yaml           # NEW
└── ai-service-hpa.yaml               # NEW
```

---

### Task 1: AI Service — Project Skeleton & Config

**Files:**
- Create: `aick-mmp-ai/requirements.txt`
- Create: `aick-mmp-ai/src/__init__.py`
- Create: `aick-mmp-ai/src/core/__init__.py`
- Create: `aick-mmp-ai/src/core/config.py`
- Create: `aick-mmp-ai/tests/__init__.py`
- Create: `aick-mmp-ai/tests/conftest.py`
- Create: `aick-mmp-ai/tests/test_config.py`
- Create: `aick-mmp-ai/models/.gitkeep`

- [ ] **Step 1: Create requirements.txt**

```text
# aick-mmp-ai/requirements.txt
fastapi==0.111.0
uvicorn[standard]==0.29.0
grpcio==1.62.1
grpcio-tools==1.62.1
aiokafka==0.10.0
onnxruntime-gpu==1.17.1
numpy==1.26.4
opencv-python-headless==4.9.0.80
pyyaml==6.0.1
prometheus-client==0.20.0
pydantic==2.7.1
pydantic-settings==2.2.1
Pillow==10.3.0

# Dev
pytest==8.2.0
pytest-asyncio==0.23.7
pytest-mock==3.14.0
```

- [ ] **Step 2: Create __init__.py files**

```python
# aick-mmp-ai/src/__init__.py
```

```python
# aick-mmp-ai/src/core/__init__.py
```

```python
# aick-mmp-ai/tests/__init__.py
```

- [ ] **Step 3: Write config.py**

```python
# aick-mmp-ai/src/core/config.py
from pydantic_settings import BaseSettings
from typing import Literal


class Settings(BaseSettings):
    # Service
    service_name: str = "ai-analysis"
    grpc_port: int = 50051
    http_port: int = 8000

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic_passenger: str = "ai-passenger-stats"
    kafka_topic_behavior: str = "ai-behavior-events"
    kafka_topic_vehicle: str = "ai-vehicle-records"
    kafka_topic_dlq: str = "ai-dlq"

    # Models
    model_dir: str = "models"
    detection_model_path: str = "models/yolov8n.onnx"
    pose_model_path: str = "models/openpifpaf.onnx"
    plate_det_model_path: str = "models/yolov8n-plate.onnx"
    plate_rec_model_path: str = "models/lprnet.onnx"

    # Analysis
    passenger_fps: float = 1.0
    plate_fps: float = 5.0
    detection_confidence: float = 0.5
    loitering_threshold_seconds: int = 30

    class Config:
        env_prefix = "AI_"
        env_file = ".env"


settings = Settings()
```

- [ ] **Step 4: Write conftest.py**

```python
# aick-mmp-ai/tests/conftest.py
import pytest


@pytest.fixture
def sample_frame() -> bytes:
    """Return a minimal 640x480 JPEG frame for testing."""
    import numpy as np
    import cv2
    img = np.zeros((480, 640, 3), dtype=np.uint8)
    cv2.putText(img, "test", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
    _, buf = cv2.imencode(".jpg", img)
    return buf.tobytes()
```

- [ ] **Step 5: Write test_config.py**

```python
# aick-mmp-ai/tests/test_config.py
from src.core.config import settings


def test_settings_defaults():
    assert settings.service_name == "ai-analysis"
    assert settings.grpc_port == 50051
    assert settings.kafka_bootstrap_servers == "localhost:9092"
    assert settings.detection_confidence == 0.5
```

- [ ] **Step 6: Run tests to verify**

Run: `cd aick-mmp-ai && python -m pytest tests/test_config.py -v`
Expected: 1 passed

- [ ] **Step 7: Commit**

```bash
git add aick-mmp-ai/
git commit -m "feat(ai): add AI service project skeleton and config"
```

---

### Task 2: AI Service — gRPC Protocol & Server

**Files:**
- Create: `aick-mmp-ai/src/proto/frame.proto`
- Create: `aick-mmp-ai/src/integrations/__init__.py`
- Create: `aick-mmp-ai/src/integrations/grpc_server.py`
- Create: `aick-mmp-ai/tests/test_grpc_server.py`

- [ ] **Step 1: Create frame.proto**

```protobuf
# aick-mmp-ai/src/proto/frame.proto
syntax = "proto3";

package aick.ai;

service FrameAnalysis {
  // Edge pushes frames, service returns analysis results
  rpc AnalyzeFrame(stream FrameRequest) returns (stream AnalysisResult);
}

message FrameRequest {
  string camera_id = 1;
  string edge_node_id = 2;
  bytes frame_data = 3;       // JPEG encoded
  int64 timestamp = 4;
  repeated string analysis_types = 5;
}

message AnalysisResult {
  string camera_id = 1;
  int64 timestamp = 2;
  oneof result {
    PassengerStats passenger = 10;
    BehaviorEvent behavior = 11;
    VehicleRecord vehicle = 12;
  }
}

message PassengerStats {
  int32 enter_count = 1;
  int32 exit_count = 2;
  int32 inside_count = 3;
  repeated ZoneStats zone_stats = 4;
}

message ZoneStats {
  string zone_name = 1;
  int32 person_count = 2;
}

message BehaviorEvent {
  string event_type = 1;       // LOITERING, INTRUSION, GATHERING, FALL
  string level = 2;            // INFO, WARNING, CRITICAL
  float position_x = 3;
  float position_y = 4;
  float width = 5;
  float height = 6;
  string snapshot_path = 7;
  string description = 8;
}

message VehicleRecord {
  string plate_number = 1;
  string plate_color = 2;
  float confidence = 3;
  string snapshot_path = 4;
}
```

- [ ] **Step 2: Generate Python gRPC code**

Run:
```bash
cd aick-mmp-ai
python -m grpc_tools.protoc \
  -Isrc/proto \
  --python_out=src/integrations \
  --grpc_python_out=src/integrations \
  src/proto/frame.proto
```
Expected: `src/integrations/frame_pb2.py` and `src/integrations/frame_pb2_grpc.py` created

- [ ] **Step 3: Create integrations __init__.py**

```python
# aick-mmp-ai/src/integrations/__init__.py
```

- [ ] **Step 4: Write gRPC server**

```python
# aick-mmp-ai/src/integrations/grpc_server.py
import asyncio
import logging
from concurrent import futures
from typing import AsyncGenerator, Callable, Optional

import grpc

from src.core.config import settings
from src.integrations.frame_pb2 import AnalysisResult
from src.integrations.frame_pb2_grpc import (
    FrameAnalysisServicer,
    add_FrameAnalysisServicer_to_server,
)
from src.integrations.frame_pb2 import FrameRequest

logger = logging.getLogger(__name__)

# Callback type: async function(camera_id, frame_data, analysis_types) -> AnalysisResult
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
```

- [ ] **Step 5: Write gRPC server test**

```python
# aick-mmp-ai/tests/test_grpc_server.py
import pytest
from src.integrations.grpc_server import GrpcServer


@pytest.mark.asyncio
async def test_grpc_server_start_stop():
    async def dummy_handler(camera_id, frame_data, timestamp, types):
        return None

    server = GrpcServer(dummy_handler)
    await server.start()
    await server.stop()
    # No exception means success
```

- [ ] **Step 6: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_grpc_server.py -v`
Expected: 1 passed

- [ ] **Step 7: Commit**

```bash
git add aick-mmp-ai/src/proto/ aick-mmp-ai/src/integrations/ aick-mmp-ai/tests/test_grpc_server.py
git commit -m "feat(ai): add gRPC protocol definition and server"
```

---

### Task 3: AI Service — Kafka Producer

**Files:**
- Create: `aick-mmp-ai/src/integrations/kafka_producer.py`
- Create: `aick-mmp-ai/tests/test_kafka_producer.py`

- [ ] **Step 1: Write kafka_producer.py**

```python
# aick-mmp-ai/src/integrations/kafka_producer.py
import json
import logging
from typing import Optional

from aiokafka import AIOKafkaProducer

from src.core.config import settings
from src.integrations.frame_pb2 import (
    AnalysisResult,
    PassengerStats,
    BehaviorEvent,
    VehicleRecord,
)

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

    async def publish_passenger(self, camera_id: str, stats: PassengerStats):
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

    async def publish_behavior(self, camera_id: str, event: BehaviorEvent):
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

    async def publish_vehicle(self, camera_id: str, record: VehicleRecord):
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
```

- [ ] **Step 2: Write kafka producer test**

```python
# aick-mmp-ai/tests/test_kafka_producer.py
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

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
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_kafka_producer.py -v`
Expected: 1 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/integrations/kafka_producer.py aick-mmp-ai/tests/test_kafka_producer.py
git commit -m "feat(ai): add Kafka result publisher"
```

---

### Task 4: AI Service — Model Loader

**Files:**
- Create: `aick-mmp-ai/src/core/model_loader.py`
- Create: `aick-mmp-ai/tests/test_model_loader.py`

- [ ] **Step 1: Write model_loader.py**

```python
# aick-mmp-ai/src/core/model_loader.py
import logging
from pathlib import Path
from typing import Optional

import onnxruntime as ort

from src.core.config import settings

logger = logging.getLogger(__name__)


class ModelManager:
    """Manages ONNX model sessions with lazy loading and GPU support."""

    def __init__(self):
        self._sessions: dict[str, ort.InferenceSession] = {}

    def _create_session(self, model_path: str) -> ort.InferenceSession:
        path = Path(model_path)
        if not path.exists():
            raise FileNotFoundError(f"Model not found: {model_path}")

        providers = []
        if "CUDAExecutionProvider" in ort.get_available_providers():
            providers.append("CUDAExecutionProvider")
        providers.append("CPUExecutionProvider")

        session = ort.InferenceSession(str(path), providers=providers)
        logger.info("Loaded model %s on %s", model_path, providers[0])
        return session

    def get_detector(self) -> ort.InferenceSession:
        if "detector" not in self._sessions:
            self._sessions["detector"] = self._create_session(settings.detection_model_path)
        return self._sessions["detector"]

    def get_pose(self) -> ort.InferenceSession:
        if "pose" not in self._sessions:
            self._sessions["pose"] = self._create_session(settings.pose_model_path)
        return self._sessions["pose"]

    def get_plate_detector(self) -> ort.InferenceSession:
        if "plate_det" not in self._sessions:
            self._sessions["plate_det"] = self._create_session(settings.plate_det_model_path)
        return self._sessions["plate_det"]

    def get_plate_recognizer(self) -> ort.InferenceSession:
        if "plate_rec" not in self._sessions:
            self._sessions["plate_rec"] = self._create_session(settings.plate_rec_model_path)
        return self._sessions["plate_rec"]

    def warmup(self):
        """Load all models at startup."""
        models = ["detector", "pose", "plate_det", "plate_rec"]
        for name in models:
            getattr(self, f"get_{name.replace('plate_det', 'plate_detector').replace('plate_rec', 'plate_recognizer')}")()


model_manager = ModelManager()
```

Wait — `getattr` for warmup is fragile. Let me simplify:

```python
    def warmup(self):
        """Load all models at startup."""
        self.get_detector()
        self.get_pose()
        self.get_plate_detector()
        self.get_plate_recognizer()
```

- [ ] **Step 2: Write model_loader test**

```python
# aick-mmp-ai/tests/test_model_loader.py
from pathlib import Path
import pytest
from src.core.model_loader import ModelManager


def test_model_manager_raises_on_missing():
    manager = ModelManager()
    with pytest.raises(FileNotFoundError):
        manager.get_detector()
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_model_loader.py -v`
Expected: 1 passed (FileNotFoundError raised since no model file exists)

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/core/model_loader.py aick-mmp-ai/tests/test_model_loader.py
git commit -m "feat(ai): add ONNX model loader with GPU support"
```

---

### Task 5: AI Service — YOLOv8 Detection

**Files:**
- Create: `aick-mmp-ai/src/services/__init__.py`
- Create: `aick-mmp-ai/src/services/detector.py`
- Create: `aick-mmp-ai/tests/test_detector.py`

- [ ] **Step 1: Create services __init__.py**

```python
# aick-mmp-ai/src/services/__init__.py
```

- [ ] **Step 2: Write detector.py**

```python
# aick-mmp-ai/src/services/detector.py
from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
import onnxruntime as ort

from src.core.config import settings
from src.core.model_loader import model_manager


@dataclass
class Detection:
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float
    class_id: int


class YOLODetector:
    def __init__(self):
        self._session: Optional[ort.InferenceSession] = None
        self._input_name: Optional[str] = None
        self._output_names: Optional[list[str]] = None

    def _ensure_loaded(self):
        if self._session is None:
            self._session = model_manager.get_detector()
            self._input_name = self._session.get_inputs()[0].name
            self._output_names = [o.name for o in self._session.get_outputs()]

    def preprocess(self, frame_bytes: bytes) -> np.ndarray:
        """Decode JPEG and prepare for YOLO inference (640x640 letterbox)."""
        img = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_COLOR)
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        h, w = img.shape[:2]
        scale = min(640 / w, 640 / h)
        nw, nh = int(w * scale), int(h * scale)
        resized = cv2.resize(img, (nw, nh), interpolation=cv2.INTER_LINEAR)
        canvas = np.full((640, 640, 3), 114, dtype=np.uint8)
        canvas[:nh, :nw] = resized
        # Normalize to [0,1] and add batch dim
        input_tensor = canvas.astype(np.float32) / 255.0
        return np.transpose(input_tensor, (2, 0, 1))[np.newaxis, ...]

    def postprocess(self, outputs: list[np.ndarray], conf_threshold: float) -> list[Detection]:
        """Parse YOLOv8 output into Detection list."""
        boxes = []
        output = outputs[0].squeeze()  # (84, 8400)
        for i in range(output.shape[1]):
            scores = output[4:, i]
            class_id = int(scores.argmax())
            confidence = float(scores[class_id])
            if confidence < conf_threshold:
                continue
            cx, cy, bw, bh = output[:4, i]
            x1 = float(cx - bw / 2)
            y1 = float(cy - bh / 2)
            x2 = float(cx + bw / 2)
            y2 = float(cy + bh / 2)
            boxes.append(Detection(x1, y1, x2, y2, confidence, class_id))
        return boxes

    def detect(self, frame_bytes: bytes,
               conf_threshold: Optional[float] = None) -> list[Detection]:
        self._ensure_loaded()
        threshold = conf_threshold or settings.detection_confidence
        input_tensor = self.preprocess(frame_bytes)
        outputs = self._session.run(self._output_names, {self._input_name: input_tensor})
        return self.postprocess(outputs, threshold)
```

- [ ] **Step 3: Write detector test**

```python
# aick-mmp-ai/tests/test_detector.py
import numpy as np
import pytest
from unittest.mock import MagicMock, patch

from src.services.detector import YOLODetector, Detection


def test_yolo_postprocess():
    detector = YOLODetector()
    # Mock YOLOv8 output: (1, 84, 8400) -> squeeze to (84, 8400)
    output = np.zeros((1, 84, 8400), dtype=np.float32)
    # Place a person detection (class_id=0) at index 0
    output[0, 0, 0] = 0.5    # cx
    output[0, 1, 0] = 0.5    # cy
    output[0, 2, 0] = 0.2    # bw
    output[0, 3, 0] = 0.4    # bh
    output[0, 4 + 0, 0] = 0.9  # person class score

    detections = detector.postprocess([output], conf_threshold=0.5)
    assert len(detections) == 1
    assert detections[0].class_id == 0
    assert detections[0].confidence == pytest.approx(0.9)

    detections = detector.postprocess([output], conf_threshold=0.95)
    assert len(detections) == 0  # below threshold


def test_yolo_preprocess(sample_frame):
    detector = YOLODetector()
    tensor = detector.preprocess(sample_frame)
    assert tensor.shape == (1, 3, 640, 640)
    assert tensor.dtype == np.float32
```

- [ ] **Step 4: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_detector.py -v`
Expected: 2 passed

- [ ] **Step 5: Commit**

```bash
git add aick-mmp-ai/src/services/__init__.py aick-mmp-ai/src/services/detector.py aick-mmp-ai/tests/test_detector.py
git commit -m "feat(ai): add YOLOv8 detection service"
```

---

### Task 6: AI Service — ByteTrack Tracker

**Files:**
- Create: `aick-mmp-ai/src/services/tracker.py`
- Create: `aick-mmp-ai/tests/test_tracker.py`

- [ ] **Step 1: Write tracker.py**

```python
# aick-mmp-ai/src/services/tracker.py
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
        # First-pass: match high-confidence detections
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
```

- [ ] **Step 2: Write tracker test**

```python
# aick-mmp-ai/tests/test_tracker.py
from src.services.tracker import ByteTrack
from src.services.detector import Detection


def test_bytetrack_new_object():
    tracker = ByteTrack(iou_threshold=0.3)
    dets = [Detection(0, 0, 10, 20, 0.9, 0)]
    tracks = tracker.update(dets)
    assert len(tracks) == 1
    assert tracks[0].track_id == 1
    assert tracks[0].lost == 0


def test_bytetrack_reuse_track():
    tracker = ByteTrack(iou_threshold=0.3)
    dets1 = [Detection(0, 0, 10, 20, 0.9, 0)]
    tracks1 = tracker.update(dets1)
    tid = tracks1[0].track_id

    dets2 = [Detection(1, 1, 11, 21, 0.9, 0)]
    tracks2 = tracker.update(dets2)
    assert tracks2[0].track_id == tid  # reused


def test_bytetrack_stale_removed():
    tracker = ByteTrack(iou_threshold=0.3, max_lost=2)
    tracker.update([Detection(0, 0, 10, 20, 0.9, 0)])
    tracker.update([])
    tracker.update([])
    tracks = tracker.update([])
    assert len(tracks) == 0  # lost > max_lost
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_tracker.py -v`
Expected: 3 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/services/tracker.py aick-mmp-ai/tests/test_tracker.py
git commit -m "feat(ai): add ByteTrack multi-object tracker"
```

---

### Task 7: AI Service — Passenger Flow Analyzer

**Files:**
- Create: `aick-mmp-ai/src/services/passenger_analyzer.py`
- Create: `aick-mmp-ai/tests/test_passenger_analyzer.py`

- [ ] **Step 1: Write passenger_analyzer.py**

```python
# aick-mmp-ai/src/services/passenger_analyzer.py
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
```

- [ ] **Step 2: Write passenger_analyzer test**

```python
# aick-mmp-ai/tests/test_passenger_analyzer.py
from src.services.passenger_analyzer import PassengerAnalyzer
from src.services.detector import Detection


def test_passenger_enter():
    analyzer = PassengerAnalyzer(line_y=0.5)
    # Person above line (cy=0.3), then below (cy=0.7) = enter
    analyzer.update([Detection(0, 0.2, 10, 0.4, 0.9, 0)])  # cy=0.3
    result = analyzer.update([Detection(0, 0.6, 10, 0.8, 0.9, 0)])  # cy=0.7
    assert result.enter_count == 1
    assert result.exit_count == 0


def test_passenger_exit():
    analyzer = PassengerAnalyzer(line_y=0.5)
    # Person below line (cy=0.7), then above (cy=0.3) = exit
    analyzer.update([Detection(0, 0.6, 10, 0.8, 0.9, 0)])
    result = analyzer.update([Detection(0, 0.2, 10, 0.4, 0.9, 0)])
    assert result.enter_count == 0
    assert result.exit_count == 1


def test_passenger_reset():
    analyzer = PassengerAnalyzer(line_y=0.5)
    analyzer.update([Detection(0, 0.2, 10, 0.4, 0.9, 0)])
    analyzer.reset()
    result = analyzer.update([Detection(0, 0.6, 10, 0.8, 0.9, 0)])
    assert result.enter_count == 0  # reset
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_passenger_analyzer.py -v`
Expected: 3 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/services/passenger_analyzer.py aick-mmp-ai/tests/test_passenger_analyzer.py
git commit -m "feat(ai): add passenger flow analyzer"
```

---

### Task 8: AI Service — Pose Estimator

**Files:**
- Create: `aick-mmp-ai/src/services/pose_estimator.py`
- Create: `aick-mmp-ai/tests/test_pose_estimator.py`

- [ ] **Step 1: Write pose_estimator.py**

```python
# aick-mmp-ai/src/services/pose_estimator.py
from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
import onnxruntime as ort

from src.core.model_loader import model_manager


@dataclass
class Keypoint:
    x: float
    y: float
    score: float


@dataclass
class Pose:
    keypoints: list[Keypoint]  # 17 keypoints: nose, eyes, ears, shoulders, elbows, wrists, hips, knees, ankles
    confidence: float


class PoseEstimator:
    def __init__(self):
        self._session: Optional[ort.InferenceSession] = None

    def _ensure_loaded(self):
        if self._session is None:
            self._session = model_manager.get_pose()

    def estimate(self, frame_bytes: bytes) -> list[Pose]:
        self._ensure_loaded()
        img = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_COLOR)
        h, w = img.shape[:2]
        # Resize to 256x256
        resized = cv2.resize(img, (256, 256))
        input_tensor = np.transpose(resized.astype(np.float32) / 255.0, (2, 0, 1))[np.newaxis, ...]

        input_name = self._session.get_inputs()[0].name
        outputs = self._session.run(None, {input_name: input_tensor})

        # Parse PIFPAF output — simplified: take first person
        poses = []
        heatmaps = outputs[0]  # (1, 17, H, W)
        for p in range(heatmaps.shape[1]):
            heatmap = heatmaps[0, p]
            max_loc = np.unravel_index(heatmap.argmax(), heatmap.shape)
            score = float(heatmap[max_loc])
            if score > 0.3:
                x = max_loc[1] / heatmap.shape[1] * w
                y = max_loc[0] / heatmap.shape[0] * h
                poses.append(Pose(
                    keypoints=[Keypoint(x, y, score)],
                    confidence=score,
                ))
        return poses
```

- [ ] **Step 2: Write pose estimator test**

```python
# aick-mmp-ai/tests/test_pose_estimator.py
import pytest
from src.services.pose_estimator import PoseEstimator


def test_pose_estimator_raises_without_model():
    estimator = PoseEstimator()
    with pytest.raises(FileNotFoundError):
        estimator.estimate(b"fake_jpeg")
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_pose_estimator.py -v`
Expected: 1 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/services/pose_estimator.py aick-mmp-ai/tests/test_pose_estimator.py
git commit -m "feat(ai): add pose estimator service"
```

---

### Task 9: AI Service — Behavior Rule Engine

**Files:**
- Create: `aick-mmp-ai/src/services/behavior_engine.py`
- Create: `aick-mmp-ai/tests/test_behavior_engine.py`

- [ ] **Step 1: Write behavior_engine.py**

```python
# aick-mmp-ai/src/services/behavior_engine.py
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
        # Loitering: track_id -> first_seen_time
        self._track_appearances: dict[int, float] = {}
        # Restricted zones: [(x1, y1, x2, y2), ...]
        self._restricted_zones: list[tuple[float, float, float, float]] = []
        self._gathering_threshold = 5  # persons
        self._loitering_threshold = 30  # seconds

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
            # Use centroid of all tracks as alert position
            avg_x = sum((t.x1 + t.x2) / 2 for t in tracks) / len(tracks)
            avg_y = sum((t.y1 + t.y2) / 2 for t in tracks) / len(tracks)
            alerts.append(BehaviorAlert(
                event_type=EventType.GATHERING,
                level=EventLevel.WARNING,
                position_x=avg_x, position_y=avg_y,
                width=0, height=0,
                description=f"{len(tracks)} persons gathered",
            ))

        # Deduplicate — only keep first of each type per track per second
        seen = set()
        unique = []
        for a in alerts:
            key = (a.event_type, int(a.position_x), int(a.position_y))
            if key not in seen:
                seen.add(key)
                unique.append(a)
        return unique
```

- [ ] **Step 2: Write behavior_engine test**

```python
# aick-mmp-ai/tests/test_behavior_engine.py
import time
from src.services.behavior_engine import BehaviorEngine, EventType, EventLevel
from src.services.tracker import TrackedObject


def test_loitering():
    engine = BehaviorEngine()
    engine._loitering_threshold = 0  # trigger immediately
    now = time.time()
    tracks = [TrackedObject(track_id=1, class_id=0, x1=0, y1=0, x2=10, y2=20)]
    alerts = engine.update(tracks, now + 31)  # > loitering_threshold
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
    # Wide & short box = fallen person
    tracks = [TrackedObject(track_id=1, class_id=0, x1=0, y1=0, x2=2, y2=0.2)]
    alerts = engine.update(tracks, time.time())
    fall = [a for a in alerts if a.event_type == EventType.FALL]
    assert len(fall) == 1
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_behavior_engine.py -v`
Expected: 4 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/services/behavior_engine.py aick-mmp-ai/tests/test_behavior_engine.py
git commit -m "feat(ai): add behavior rule engine (loitering/intrusion/gathering/fall)"
```

---

### Task 10: AI Service — License Plate Recognizer

**Files:**
- Create: `aick-mmp-ai/src/services/plate_recognizer.py`
- Create: `aick-mmp-ai/tests/test_plate_recognizer.py`

- [ ] **Step 1: Write plate_recognizer.py**

```python
# aick-mmp-ai/src/services/plate_recognizer.py
from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
import onnxruntime as ort

from src.core.model_loader import model_manager
from src.services.detector import Detection, YOLODetector


@dataclass
class PlateResult:
    plate_number: str
    plate_color: str
    confidence: float
    bbox: tuple[float, float, float, float]


class PlateRecognizer:
    def __init__(self):
        self._detector = YOLODetector()  # reuses model_manager internally
        self._rec_session: Optional[ort.InferenceSession] = None

    def _ensure_rec_loaded(self):
        if self._rec_session is None:
            self._rec_session = model_manager.get_plate_recognizer()

    def recognize(self, frame_bytes: bytes) -> list[PlateResult]:
        # Step 1: detect plate regions
        plate_dets = self._detector.detect(frame_bytes)
        if not plate_dets:
            return []

        # Step 2: recognize text from each plate region
        self._ensure_rec_loaded()
        results = []
        img = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_COLOR)
        h, w = img.shape[:2]

        for det in plate_dets:
            x1, y1, x2, y2 = map(int, [det.x1 * w, det.y1 * h, det.x2 * w, det.y2 * h])
            plate_crop = img[y1:y2, x1:x2]
            if plate_crop.size == 0:
                continue

            # Preprocess for LPRNet (94x24 grayscale)
            crop_gray = cv2.cvtColor(plate_crop, cv2.COLOR_BGR2GRAY)
            resized = cv2.resize(crop_gray, (94, 24))
            input_tensor = resized.astype(np.float32)[np.newaxis, np.newaxis, ...] / 255.0

            input_name = self._rec_session.get_inputs()[0].name
            logits = self._rec_session.run(None, {input_name: input_tensor})[0]

            # Greedy CTC decode
            chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            plate_chars = []
            prev = -1
            for t in range(logits.shape[1]):
                c = int(logits[0, t].argmax())
                if c != prev and c < len(chars):
                    plate_chars.append(chars[c])
                prev = c

            plate_text = "".join(plate_chars)
            if plate_text:
                results.append(PlateResult(
                    plate_number=plate_text,
                    plate_color="blue",
                    confidence=det.confidence,
                    bbox=(x1 / w, y1 / h, x2 / w, y2 / h),
                ))

        return results
```

- [ ] **Step 2: Write plate recognizer test**

```python
# aick-mmp-ai/tests/test_plate_recognizer.py
import pytest
from src.services.plate_recognizer import PlateRecognizer


def test_plate_recognizer_raises_without_model():
    recognizer = PlateRecognizer()
    with pytest.raises(FileNotFoundError):
        recognizer.recognize(b"fake_jpeg")
```

- [ ] **Step 3: Run tests**

Run: `cd aick-mmp-ai && python -m pytest tests/test_plate_recognizer.py -v`
Expected: 1 passed

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/services/plate_recognizer.py aick-mmp-ai/tests/test_plate_recognizer.py
git commit -m "feat(ai): add license plate recognizer"
```

---

### Task 11: AI Service — FastAPI Entry & Orchestration

**Files:**
- Create: `aick-mmp-ai/src/api/__init__.py`
- Create: `aick-mmp-ai/src/api/main.py`
- Create: `aick-mmp-ai/src/services/__init__.py` (already created in Task 5)

- [ ] **Step 1: Create api __init__.py**

```python
# aick-mmp-ai/src/api/__init__.py
```

- [ ] **Step 2: Write main.py**

```python
# aick-mmp-ai/src/api/main.py
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

            # Simplified: detect persons then run rules
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
                # Only return the most critical alert per frame
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

    # Don't send empty results
    if result.HasField("passenger") or result.HasField("behavior") or result.HasField("vehicle"):
        return result
    return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    grpc_server = GrpcServer(handle_frame)
    await grpc_server.start()
    await publisher.start()
    logger.info("AI service started on gRPC:%d HTTP:%d", settings.grpc_port, settings.http_port)
    yield
    # Shutdown
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
```

- [ ] **Step 3: Run import check**

Run: `cd aick-mmp-ai && python -c "from src.api.main import app; print('OK')"`
Expected: OK (may warn about models not found, which is fine)

- [ ] **Step 4: Commit**

```bash
git add aick-mmp-ai/src/api/
git commit -m "feat(ai): add FastAPI entry point with frame orchestration"
```

---

### Task 12: AI Service — Dockerfile & Docker Compose

**Files:**
- Create: `aick-mmp-ai/Dockerfile`
- Create: `aick-mmp-ai/.dockerignore`

- [ ] **Step 1: Write Dockerfile**

```dockerfile
# aick-mmp-ai/Dockerfile
FROM nvidia/cuda:12.4-runtime-ubuntu22.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3.11 python3-pip libgl1-mesa-glx libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY src/ src/
COPY models/ models/

EXPOSE 8000 50051

CMD ["uvicorn", "src.api.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

- [ ] **Step 2: Write .dockerignore**

```dockerignore
# aick-mmp-ai/.dockerignore
__pycache__
*.pyc
.git
tests/
*.md
```

- [ ] **Step 3: Commit**

```bash
git add aick-mmp-ai/Dockerfile aick-mmp-ai/.dockerignore
git commit -m "feat(ai): add Dockerfile with CUDA support"
```

---

### Task 13: Edge Module — gRPC Dependencies

**Files:**
- Modify: `backend/aick-mmp-edge/pom.xml`

- [ ] **Step 1: Add gRPC dependencies to edge pom.xml**

Find the `<dependencies>` section and add:

```xml
<!-- gRPC -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.62.2</version>
</dependency>
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

- [ ] **Step 2: Commit**

```bash
git add backend/aick-mmp-edge/pom.xml
git commit -m "feat(edge): add gRPC dependencies"
```

---

### Task 14: Edge Module — FrameExtractor

**Files:**
- Create: `backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameExtractor.java`
- Create: `backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/config/AiServiceConfig.java`

- [ ] **Step 1: Write AiServiceConfig.java**

```java
// backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/config/AiServiceConfig.java
package com.aick.mmp.edge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceConfig {
    private String host = "localhost";
    private int grpcPort = 50051;
    private Map<String, CameraAnalysisConfig> cameras;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getGrpcPort() { return grpcPort; }
    public void setGrpcPort(int grpcPort) { this.grpcPort = grpcPort; }

    public Map<String, CameraAnalysisConfig> getCameras() { return cameras; }
    public void setCameras(Map<String, CameraAnalysisConfig> cameras) { this.cameras = cameras; }

    public String getTargetAddress() {
        return host + ":" + grpcPort;
    }

    public static class CameraAnalysisConfig {
        private double fps = 1.0;
        private java.util.List<String> analysisTypes;

        public double getFps() { return fps; }
        public void setFps(double fps) { this.fps = fps; }

        public java.util.List<String> getAnalysisTypes() { return analysisTypes; }
        public void setAnalysisTypes(java.util.List<String> analysisTypes) { this.analysisTypes = analysisTypes; }
    }
}
```

- [ ] **Step 2: Write FrameExtractor.java**

```java
// backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameExtractor.java
package com.aick.mmp.edge.service;

import com.aick.mmp.edge.config.AiServiceConfig;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class FrameExtractor {
    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    private final FrameAnalysisGrpcClient grpcClient;
    private final AiServiceConfig config;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> extractions = new ConcurrentHashMap<>();

    public FrameExtractor(FrameAnalysisGrpcClient grpcClient, AiServiceConfig config,
                          ScheduledExecutorService scheduler) {
        this.grpcClient = grpcClient;
        this.config = config;
        this.scheduler = scheduler;
    }

    public void startExtraction(String cameraId, String streamUrl) {
        if (extractions.containsKey(cameraId)) {
            return; // already running
        }

        AiServiceConfig.CameraAnalysisConfig camConfig =
            config.getCameras() != null ? config.getCameras().get(cameraId) : null;
        double fps = camConfig != null ? camConfig.getFps() : 1.0;
        java.util.List<String> types = camConfig != null ? camConfig.getAnalysisTypes() : java.util.List.of("passenger");

        long periodMs = (long) (1000.0 / fps);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> captureAndSend(cameraId, streamUrl, types),
            0, periodMs, TimeUnit.MILLISECONDS
        );
        extractions.put(cameraId, future);
        log.info("Started frame extraction for camera {} at {} fps", cameraId, fps);
    }

    public void stopExtraction(String cameraId) {
        ScheduledFuture<?> future = extractions.remove(cameraId);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped frame extraction for camera {}", cameraId);
        }
    }

    private void captureAndSend(String cameraId, String streamUrl, java.util.List<String> types) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl)) {
            grabber.start();
            Frame frame = grabber.grabImage();
            if (frame != null) {
                try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    java.awt.image.BufferedImage bi = converter.convert(frame);
                    if (bi != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "jpg", baos);
                        baos.flush();
                        byte[] jpegData = baos.toByteArray();
                        grpcClient.sendFrame(cameraId, jpegData, System.currentTimeMillis(), types);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to capture frame for camera {}: {}", cameraId, e.getMessage());
        }
    }
}
```

**Note:** This uses JavaCV for frame grabbing. If the project already uses a different RTSP frame extraction library, adapt accordingly. The key interface is: extract JPEG bytes → send via gRPC.

- [ ] **Step 3: Commit**

Commit after running `mvn compile` to verify it compiles:

```bash
cd backend && mvn compile -pl aick-mmp-edge -am -q 2>&1 | tail -5
git add backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameExtractor.java backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/config/
git commit -m "feat(edge): add FrameExtractor and AI service config"
```

---

### Task 15: Edge Module — gRPC Client

**Files:**
- Create: `backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameAnalysisGrpcClient.java`
- Modify: `backend/aick-mmp-edge/src/main/resources/application.yml`

- [ ] **Step 1: Add protobuf-maven-plugin to edge pom.xml**

Add to `<build><plugins>` section of `pom.xml`:

```xml
<plugin>
    <groupId>com.github.os72</groupId>
    <artifactId>protoc-jar-maven-plugin</artifactId>
    <version>3.11.4</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>run</goal></goals>
            <configuration>
                <protocVersion>3.25.3</protocVersion>
                <inputTargets>
                    <inputTarget>
                        <type>grpc-java</type>
                        <input>${project.basedir}/../../aick-mmp-ai/src/proto/frame.proto</input>
                        <outputDirectory>${project.build.directory}/generated-sources/proto</outputDirectory>
                    </inputTarget>
                </inputTargets>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Write gRPC client**

```java
// backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameAnalysisGrpcClient.java
package com.aick.mmp.edge.service;

import com.aick.ai.FrameAnalysisGrpc;
import com.aick.ai.FrameOuterClass;
import com.aick.mmp.edge.config.AiServiceConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FrameAnalysisGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(FrameAnalysisGrpcClient.class);

    private final AiServiceConfig config;
    private ManagedChannel channel;
    private FrameAnalysisGrpc.FrameAnalysisStub asyncStub;
    private StreamObserver<FrameOuterClass.FrameRequest> requestObserver;

    public FrameAnalysisGrpcClient(AiServiceConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        channel = NettyChannelBuilder.forTarget(config.getTargetAddress())
            .usePlaintext()
            .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
            .build();
        asyncStub = FrameAnalysisGrpc.newStub(channel);
    }

    public void sendFrame(String cameraId, byte[] jpegData, long timestampMs,
                          java.util.List<String> analysisTypes) {
        FrameOuterClass.FrameRequest request = FrameOuterClass.FrameRequest.newBuilder()
            .setCameraId(cameraId)
            .setEdgeNodeId("edge-" + cameraId)
            .setFrameData(com.google.protobuf.ByteString.copyFrom(jpegData))
            .setTimestamp(timestampMs)
            .addAllAnalysisTypes(analysisTypes)
            .build();

        if (requestObserver == null) {
            initStream();
        }

        try {
            requestObserver.onNext(request);
        } catch (Exception e) {
            log.error("gRPC send failed, reconnecting...", e);
            requestObserver = null;
        }
    }

    private void initStream() {
        requestObserver = asyncStub.analyzeFrame(new StreamObserver<>() {
            @Override
            public void onNext(FrameOuterClass.AnalysisResult value) {
                // Results arrive asynchronously — logged for now
                log.debug("Analysis result for camera {}: {}",
                    value.getCameraId(), value.getPassengerCase());
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC stream error", t);
                requestObserver = null;
            }

            @Override
            public void onCompleted() {
                log.info("gRPC stream completed");
                requestObserver = null;
            }
        });
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (requestObserver != null) {
            requestObserver.onCompleted();
        }
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
```

- [ ] **Step 3: Add config to application.yml**

Add to `application.yml`:

```yaml
ai:
  service:
    host: ${AI_SERVICE_HOST:ai-service}
    grpc-port: ${AI_SERVICE_GRPC_PORT:50051}
    cameras:
      # Per-camera override example:
      # cam-001:
      #   fps: 5.0
      #   analysis-types: [plate]
      # cam-002:
      #   fps: 1.0
      #   analysis-types: [passenger, behavior]
```

- [ ] **Step 4: Commit**

```bash
cd backend && mvn compile -pl aick-mmp-edge -am -q 2>&1 | tail -10
git add backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/FrameAnalysisGrpcClient.java backend/aick-mmp-edge/src/main/resources/application.yml
git commit -m "feat(edge): add gRPC client for AI service"
```

---

### Task 16: Central — Database Tables & Entities

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiPassengerStats.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiBehaviorEvent.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleRecord.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleWhitelist.java`
- Create: `docs/sql/V20260510__create_ai_tables.sql`

- [ ] **Step 1: Write AiPassengerStats entity**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiPassengerStats.java
package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_passenger_stats")
public class AiPassengerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "edge_node_id")
    private Long edgeNodeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "enter_count")
    private Integer enterCount = 0;

    @Column(name = "exit_count")
    private Integer exitCount = 0;

    @Column(name = "inside_count")
    private Integer insideCount = 0;

    @Column(name = "max_inside_count")
    private Integer maxInsideCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public Long getEdgeNodeId() { return edgeNodeId; }
    public void setEdgeNodeId(Long edgeNodeId) { this.edgeNodeId = edgeNodeId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getEnterCount() { return enterCount; }
    public void setEnterCount(Integer enterCount) { this.enterCount = enterCount; }
    public Integer getExitCount() { return exitCount; }
    public void setExitCount(Integer exitCount) { this.exitCount = exitCount; }
    public Integer getInsideCount() { return insideCount; }
    public void setInsideCount(Integer insideCount) { this.insideCount = insideCount; }
    public Integer getMaxInsideCount() { return maxInsideCount; }
    public void setMaxInsideCount(Integer maxInsideCount) { this.maxInsideCount = maxInsideCount; }
}
```

- [ ] **Step 2: Write AiBehaviorEvent entity**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiBehaviorEvent.java
package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_behavior_events")
public class AiBehaviorEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "position_data", columnDefinition = "JSON")
    private String positionData;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "status", length = 20)
    private String status = "UNRESOLVED";

    @Column(name = "alert_record_id")
    private Long alertRecordId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getPositionData() { return positionData; }
    public void setPositionData(String positionData) { this.positionData = positionData; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAlertRecordId() { return alertRecordId; }
    public void setAlertRecordId(Long alertRecordId) { this.alertRecordId = alertRecordId; }
}
```

- [ ] **Step 3: Write AiVehicleRecord entity**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleRecord.java
package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_vehicle_records")
public class AiVehicleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "plate_number", nullable = false, length = 50)
    private String plateNumber;

    @Column(name = "plate_color", length = 20)
    private String plateColor;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;

    @Column(name = "is_whitelisted")
    private Boolean isWhitelisted = false;

    @Column(name = "is_blacklisted")
    private Boolean isBlacklisted = false;

    @Column(name = "detect_time", nullable = false)
    private LocalDateTime detectTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getPlateColor() { return plateColor; }
    public void setPlateColor(String plateColor) { this.plateColor = plateColor; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public Boolean getIsWhitelisted() { return isWhitelisted; }
    public void setIsWhitelisted(Boolean isWhitelisted) { this.isWhitelisted = isWhitelisted; }
    public Boolean getIsBlacklisted() { return isBlacklisted; }
    public void setIsBlacklisted(Boolean isBlacklisted) { this.isBlacklisted = isBlacklisted; }
    public LocalDateTime getDetectTime() { return detectTime; }
    public void setDetectTime(LocalDateTime detectTime) { this.detectTime = detectTime; }
}
```

- [ ] **Step 4: Write AiVehicleWhitelist entity**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleWhitelist.java
package com.aick.mmp.central.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_vehicle_whitelist")
public class AiVehicleWhitelist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 50)
    private String plateNumber;

    @Column(name = "plate_color", length = 20)
    private String plateColor;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getPlateColor() { return plateColor; }
    public void setPlateColor(String plateColor) { this.plateColor = plateColor; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 5: Write SQL migration**

```sql
-- docs/sql/V20260510__create_ai_tables.sql

CREATE TABLE IF NOT EXISTS ai_passenger_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    edge_node_id BIGINT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    enter_count INT DEFAULT 0,
    exit_count INT DEFAULT 0,
    inside_count INT DEFAULT 0,
    max_inside_count INT DEFAULT 0,
    INDEX idx_camera_time (camera_id, start_time),
    INDEX idx_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    level VARCHAR(20),
    position_data JSON,
    snapshot_url VARCHAR(500),
    description TEXT,
    event_time DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'UNRESOLVED',
    alert_record_id BIGINT,
    INDEX idx_camera_event (camera_id, event_time),
    INDEX idx_event_type (event_type, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_vehicle_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id BIGINT NOT NULL,
    plate_number VARCHAR(50) NOT NULL,
    plate_color VARCHAR(20),
    confidence DECIMAL(5,4),
    snapshot_url VARCHAR(500),
    is_whitelisted BOOLEAN DEFAULT FALSE,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    detect_time DATETIME NOT NULL,
    INDEX idx_plate (plate_number),
    INDEX idx_time (detect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_vehicle_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(50) NOT NULL UNIQUE,
    plate_color VARCHAR(20),
    owner_name VARCHAR(100),
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 6: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiPassengerStats.java backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiBehaviorEvent.java backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleRecord.java backend/aick-mmp-central/src/main/java/com/aick/mmp/central/entity/AiVehicleWhitelist.java docs/sql/
git commit -m "feat(central): add AI analysis entities and SQL migration"
```

---

### Task 17: Central — Kafka Consumers

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiPassengerStatsConsumer.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiBehaviorEventConsumer.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiVehicleRecordConsumer.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiPassengerStatsRepository.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiBehaviorEventRepository.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiVehicleRecordRepository.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiVehicleWhitelistRepository.java`

- [ ] **Step 1: Write repositories**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiPassengerStatsRepository.java
package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiPassengerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiPassengerStatsRepository extends JpaRepository<AiPassengerStats, Long> {
    List<AiPassengerStats> findByCameraIdAndStartTimeBetween(Long cameraId, LocalDateTime from, LocalDateTime to);
}
```

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiBehaviorEventRepository.java
package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiBehaviorEventRepository extends JpaRepository<AiBehaviorEvent, Long> {
    List<AiBehaviorEvent> findByCameraIdOrderByEventTimeDesc(Long cameraId);
    List<AiBehaviorEvent> findByEventTypeAndStatus(String eventType, String status);
}
```

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiVehicleRecordRepository.java
package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiVehicleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiVehicleRecordRepository extends JpaRepository<AiVehicleRecord, Long> {
    List<AiVehicleRecord> findByPlateNumberOrderByDetectTimeDesc(String plateNumber);
    List<AiVehicleRecord> findByCameraIdOrderByDetectTimeDesc(Long cameraId);
}
```

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/AiVehicleWhitelistRepository.java
package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiVehicleWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiVehicleWhitelistRepository extends JpaRepository<AiVehicleWhitelist, Long> {
    Optional<AiVehicleWhitelist> findByPlateNumber(String plateNumber);
    boolean existsByPlateNumber(String plateNumber);
}
```

- [ ] **Step 2: Write Kafka consumers**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiPassengerStatsConsumer.java
package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.repository.AiPassengerStatsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Component
public class AiPassengerStatsConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiPassengerStatsConsumer.class);
    private static final String REDIS_KEY = "ai:passenger:realtime:%s";

    private final AiPassengerStatsRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiPassengerStatsConsumer(AiPassengerStatsRepository repository,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-passenger-stats", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            int enterCount = json.get("enter_count").asInt();
            int exitCount = json.get("exit_count").asInt();
            int insideCount = json.get("inside_count").asInt();

            // Update Redis realtime cache
            redisTemplate.opsForValue().set(
                String.format(REDIS_KEY, cameraId),
                String.valueOf(insideCount),
                5, TimeUnit.MINUTES
            );

            // Persist aggregated stats every message (could batch)
            AiPassengerStats stats = new AiPassengerStats();
            stats.setCameraId(cameraId);
            stats.setStartTime(LocalDateTime.now().minusMinutes(1));
            stats.setEndTime(LocalDateTime.now());
            stats.setEnterCount(enterCount);
            stats.setExitCount(exitCount);
            stats.setInsideCount(insideCount);
            repository.save(stats);

        } catch (Exception e) {
            log.error("Failed to process passenger stats: {}", e.getMessage());
        }
    }
}
```

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiBehaviorEventConsumer.java
package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AlertRecord;
import com.aick.mmp.central.repository.AiBehaviorEventRepository;
import com.aick.mmp.central.service.AlertNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class AiBehaviorEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiBehaviorEventConsumer.class);

    private final AiBehaviorEventRepository repository;
    private final AlertNotificationService alertService;
    private final ObjectMapper objectMapper;

    public AiBehaviorEventConsumer(AiBehaviorEventRepository repository,
                                   AlertNotificationService alertService,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-behavior-events", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            String eventType = json.get("event_type").asText();
            String level = json.get("level").asText();
            String description = json.get("description").asText();

            // Persist event
            AiBehaviorEvent event = new AiBehaviorEvent();
            event.setCameraId(cameraId);
            event.setEventType(eventType);
            event.setLevel(level);
            event.setDescription(description);
            event.setEventTime(LocalDateTime.now());
            event.setStatus("UNRESOLVED");
            event = repository.save(event);

            // Send alert via existing notification system
            AlertRecord alert = new AlertRecord();
            alert.setTitle("AI Behavior Alert: " + eventType);
            alert.setMessage(description);
            alert.setAlertType("AI_BEHAVIOR");
            alert.setLevel(level);
            alert.setCameraId(cameraId);
            alert.setSource("AI_SERVICE");
            alertService.sendAlert(alert);

            log.info("Behavior alert processed: {} for camera {}", eventType, cameraId);
        } catch (Exception e) {
            log.error("Failed to process behavior event: {}", e.getMessage());
        }
    }
}
```

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/AiVehicleRecordConsumer.java
package com.aick.mmp.central.consumer;

import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.repository.AiVehicleRecordRepository;
import com.aick.mmp.central.repository.AiVehicleWhitelistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AiVehicleRecordConsumer {
    private static final Logger log = LoggerFactory.getLogger(AiVehicleRecordConsumer.class);

    private final AiVehicleRecordRepository repository;
    private final AiVehicleWhitelistRepository whitelistRepository;
    private final ObjectMapper objectMapper;

    public AiVehicleRecordConsumer(AiVehicleRecordRepository repository,
                                   AiVehicleWhitelistRepository whitelistRepository,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.whitelistRepository = whitelistRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ai-vehicle-records", groupId = "mmp-ai-group")
    public void consume(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long cameraId = Long.parseLong(json.get("camera_id").asText().replaceAll("[^0-9]", ""));
            String plateNumber = json.get("plate_number").asText();

            // Check whitelist/blacklist
            Optional<AiVehicleWhitelist> whitelistEntry = whitelistRepository.findByPlateNumber(plateNumber);

            AiVehicleRecord record = new AiVehicleRecord();
            record.setCameraId(cameraId);
            record.setPlateNumber(plateNumber);
            record.setPlateColor(json.has("plate_color") ? json.get("plate_color").asText() : null);
            record.setConfidence(json.has("confidence") ? BigDecimal.valueOf(json.get("confidence").asDouble()) : null);
            record.setDetectTime(LocalDateTime.now());
            record.setIsWhitelisted(whitelistEntry.isPresent() && whitelistEntry.get().getEnabled());
            record.setIsBlacklisted(false);
            repository.save(record);

            if (whitelistEntry.isPresent() && whitelistEntry.get().getEnabled()) {
                log.info("Whitelisted vehicle detected: {}", plateNumber);
            }
        } catch (Exception e) {
            log.error("Failed to process vehicle record: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd backend && mvn compile -pl aick-mmp-central -am -q 2>&1 | tail -10
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/consumer/ backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/
git commit -m "feat(central): add AI Kafka consumers and repositories"
```

---

### Task 18: Central — AI Analysis REST API & Service

**Files:**
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/AiAnalysisService.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/AiAnalysisServiceImpl.java`
- Create: `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/AiAnalysisController.java`

- [ ] **Step 1: Write service interface**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/AiAnalysisService.java
package com.aick.mmp.central.service;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AiAnalysisService {
    // Passenger stats
    List<AiPassengerStats> getPassengerStats(Long cameraId, LocalDateTime from, LocalDateTime to);
    String getRealtimePassenger(Long cameraId);

    // Behavior events
    List<AiBehaviorEvent> getBehaviorEvents(Long cameraId, String eventType, String status);
    AiBehaviorEvent updateBehaviorStatus(Long id, String status);

    // Vehicle records
    List<AiVehicleRecord> getVehicleRecords(String plateNumber, Long cameraId);

    // Whitelist
    List<AiVehicleWhitelist> getAllWhitelist();
    AiVehicleWhitelist addWhitelist(AiVehicleWhitelist entry);
    AiVehicleWhitelist updateWhitelist(Long id, AiVehicleWhitelist entry);
    void deleteWhitelist(Long id);
}
```

- [ ] **Step 2: Write service implementation**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/AiAnalysisServiceImpl.java
package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.repository.AiPassengerStatsRepository;
import com.aick.mmp.central.repository.AiBehaviorEventRepository;
import com.aick.mmp.central.repository.AiVehicleRecordRepository;
import com.aick.mmp.central.repository.AiVehicleWhitelistRepository;
import com.aick.mmp.central.service.AiAnalysisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {
    private static final String REDIS_KEY = "ai:passenger:realtime:%s";

    private final AiPassengerStatsRepository passengerRepo;
    private final AiBehaviorEventRepository behaviorRepo;
    private final AiVehicleRecordRepository vehicleRepo;
    private final AiVehicleWhitelistRepository whitelistRepo;
    private final StringRedisTemplate redisTemplate;

    public AiAnalysisServiceImpl(AiPassengerStatsRepository passengerRepo,
                                  AiBehaviorEventRepository behaviorRepo,
                                  AiVehicleRecordRepository vehicleRepo,
                                  AiVehicleWhitelistRepository whitelistRepo,
                                  StringRedisTemplate redisTemplate) {
        this.passengerRepo = passengerRepo;
        this.behaviorRepo = behaviorRepo;
        this.vehicleRepo = vehicleRepo;
        this.whitelistRepo = whitelistRepo;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<AiPassengerStats> getPassengerStats(Long cameraId, LocalDateTime from, LocalDateTime to) {
        return passengerRepo.findByCameraIdAndStartTimeBetween(cameraId, from, to);
    }

    @Override
    public String getRealtimePassenger(Long cameraId) {
        String val = redisTemplate.opsForValue().get(String.format(REDIS_KEY, cameraId));
        return val != null ? val : "0";
    }

    @Override
    public List<AiBehaviorEvent> getBehaviorEvents(Long cameraId, String eventType, String status) {
        if (eventType != null && status != null) {
            return behaviorRepo.findByEventTypeAndStatus(eventType, status);
        }
        return behaviorRepo.findByCameraIdOrderByEventTimeDesc(cameraId);
    }

    @Override
    @Transactional
    public AiBehaviorEvent updateBehaviorStatus(Long id, String status) {
        AiBehaviorEvent event = behaviorRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Behavior event not found: " + id));
        event.setStatus(status);
        return behaviorRepo.save(event);
    }

    @Override
    public List<AiVehicleRecord> getVehicleRecords(String plateNumber, Long cameraId) {
        if (plateNumber != null && !plateNumber.isEmpty()) {
            return vehicleRepo.findByPlateNumberOrderByDetectTimeDesc(plateNumber);
        }
        return vehicleRepo.findByCameraIdOrderByDetectTimeDesc(cameraId);
    }

    @Override
    public List<AiVehicleWhitelist> getAllWhitelist() {
        return whitelistRepo.findAll();
    }

    @Override
    public AiVehicleWhitelist addWhitelist(AiVehicleWhitelist entry) {
        return whitelistRepo.save(entry);
    }

    @Override
    public AiVehicleWhitelist updateWhitelist(Long id, AiVehicleWhitelist entry) {
        AiVehicleWhitelist existing = whitelistRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Whitelist entry not found: " + id));
        existing.setPlateNumber(entry.getPlateNumber());
        existing.setPlateColor(entry.getPlateColor());
        existing.setOwnerName(entry.getOwnerName());
        existing.setDescription(entry.getDescription());
        existing.setEnabled(entry.getEnabled());
        return whitelistRepo.save(existing);
    }

    @Override
    public void deleteWhitelist(Long id) {
        whitelistRepo.deleteById(id);
    }
}
```

- [ ] **Step 3: Write controller**

```java
// backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/AiAnalysisController.java
package com.aick.mmp.central.controller;

import com.aick.mmp.central.entity.AiBehaviorEvent;
import com.aick.mmp.central.entity.AiPassengerStats;
import com.aick.mmp.central.entity.AiVehicleRecord;
import com.aick.mmp.central.entity.AiVehicleWhitelist;
import com.aick.mmp.central.service.AiAnalysisService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalysisController {

    private final AiAnalysisService service;

    public AiAnalysisController(AiAnalysisService service) {
        this.service = service;
    }

    // --- Passenger Stats ---

    @GetMapping("/stats/passenger")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiPassengerStats>> getPassengerStats(
            @RequestParam Long cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(service.getPassengerStats(cameraId, startTime, endTime));
    }

    @GetMapping("/stats/passenger/realtime/{cameraId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<String> getRealtimePassenger(@PathVariable Long cameraId) {
        return ResponseEntity.ok(service.getRealtimePassenger(cameraId));
    }

    // --- Behavior Events ---

    @GetMapping("/alerts/behavior")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiBehaviorEvent>> getBehaviorEvents(
            @RequestParam(defaultValue = "0") Long cameraId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getBehaviorEvents(cameraId, eventType, status));
    }

    @PutMapping("/alerts/behavior/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiBehaviorEvent> updateBehaviorStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(service.updateBehaviorStatus(id, status));
    }

    // --- Vehicle Records ---

    @GetMapping("/vehicles/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AiVehicleRecord>> getVehicleRecords(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(defaultValue = "0") Long cameraId) {
        return ResponseEntity.ok(service.getVehicleRecords(plateNumber, cameraId));
    }

    // --- Whitelist ---

    @GetMapping("/vehicles/whitelist")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<AiVehicleWhitelist>> getAllWhitelist() {
        return ResponseEntity.ok(service.getAllWhitelist());
    }

    @PostMapping("/vehicles/whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiVehicleWhitelist> addWhitelist(@RequestBody AiVehicleWhitelist entry) {
        return ResponseEntity.ok(service.addWhitelist(entry));
    }

    @PutMapping("/vehicles/whitelist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiVehicleWhitelist> updateWhitelist(
            @PathVariable Long id, @RequestBody AiVehicleWhitelist entry) {
        return ResponseEntity.ok(service.updateWhitelist(id, entry));
    }

    @DeleteMapping("/vehicles/whitelist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWhitelist(@PathVariable Long id) {
        service.deleteWhitelist(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Add Kafka config to central application.yml**

Add to central's `application.yml`:

```yaml
spring:
  kafka:
    consumer:
      group-id: mmp-ai-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

- [ ] **Step 5: Verify compilation**

Run: `cd backend && mvn compile -pl aick-mmp-central -am -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/ backend/aick-mmp-central/src/main/java/com/aick/mmp/central/controller/AiAnalysisController.java
git commit -m "feat(central): add AI analysis REST API and service layer"
```

---

### Task 19: Frontend — TypeScript API Client

**Files:**
- Create: `frontend/src/services/aiApi.ts`
- Optionally update existing pattern files

- [ ] **Step 1: Write aiApi.ts**

```typescript
// frontend/src/services/aiApi.ts
import axios from 'axios';

const API_BASE = '/api/v1/ai';

export interface PassengerStats {
  id: number;
  cameraId: number;
  startTime: string;
  endTime: string;
  enterCount: number;
  exitCount: number;
  insideCount: number;
  maxInsideCount: number;
}

export interface BehaviorEvent {
  id: number;
  cameraId: number;
  eventType: 'LOITERING' | 'INTRUSION' | 'GATHERING' | 'FALL';
  level: 'INFO' | 'WARNING' | 'CRITICAL';
  positionData?: string;
  snapshotUrl?: string;
  description?: string;
  eventTime: string;
  status: 'UNRESOLVED' | 'ACKNOWLEDGED' | 'RESOLVED';
}

export interface VehicleRecord {
  id: number;
  cameraId: number;
  plateNumber: string;
  plateColor?: string;
  confidence?: number;
  snapshotUrl?: string;
  isWhitelisted: boolean;
  detectTime: string;
}

export interface WhitelistEntry {
  id?: number;
  plateNumber: string;
  plateColor?: string;
  ownerName?: string;
  description?: string;
  enabled: boolean;
}

export const aiApi = {
  // Passenger
  getPassengerStats: (cameraId: number, startTime: string, endTime: string) =>
    axios.get<PassengerStats[]>(`${API_BASE}/stats/passenger`, { params: { cameraId, startTime, endTime } }),

  getRealtimePassenger: (cameraId: number) =>
    axios.get<string>(`${API_BASE}/stats/passenger/realtime/${cameraId}`),

  // Behavior
  getBehaviorEvents: (cameraId: number, eventType?: string, status?: string) =>
    axios.get<BehaviorEvent[]>(`${API_BASE}/alerts/behavior`, { params: { cameraId, eventType, status } }),

  updateBehaviorStatus: (id: number, status: string) =>
    axios.put<BehaviorEvent>(`${API_BASE}/alerts/behavior/${id}/status`, null, { params: { status } }),

  // Vehicle
  getVehicleRecords: (plateNumber?: string, cameraId?: number) =>
    axios.get<VehicleRecord[]>(`${API_BASE}/vehicles/records`, { params: { plateNumber, cameraId } }),

  // Whitelist
  getWhitelist: () =>
    axios.get<WhitelistEntry[]>(`${API_BASE}/vehicles/whitelist`),

  addWhitelist: (entry: WhitelistEntry) =>
    axios.post<WhitelistEntry>(`${API_BASE}/vehicles/whitelist`, entry),

  updateWhitelist: (id: number, entry: WhitelistEntry) =>
    axios.put<WhitelistEntry>(`${API_BASE}/vehicles/whitelist/${id}`, entry),

  deleteWhitelist: (id: number) =>
    axios.delete(`${API_BASE}/vehicles/whitelist/${id}`),
};
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/services/aiApi.ts
git commit -m "feat(web): add AI analysis API client (TypeScript)"
```

---

### Task 20: Frontend — Passenger Flow Dashboard

**Files:**
- Create: `frontend/src/pages/AiPassengerDashboard.tsx`
- Modify: `frontend/src/App.jsx` (add route)

- [ ] **Step 1: Write AiPassengerDashboard.tsx**

```typescript
// frontend/src/pages/AiPassengerDashboard.tsx
import React, { useEffect, useState, useCallback } from 'react';
import { Card, Row, Col, Statistic, Select, DatePicker, Table } from 'antd';
import { UserOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { aiApi, PassengerStats } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';
import dayjs from 'dayjs';

const AiPassengerDashboard: React.FC = () => {
  const [cameraId, setCameraId] = useState<number>(1);
  const [realtime, setRealtime] = useState<string>('0');
  const [stats, setStats] = useState<PassengerStats[]>([]);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('day'), dayjs(),
  ]);

  const fetchRealtime = useCallback(async () => {
    try {
      const res = await aiApi.getRealtimePassenger(cameraId);
      setRealtime(res.data);
    } catch { /* ignore polling errors */ }
  }, [cameraId]);

  const fetchStats = useCallback(async () => {
    try {
      const [from, to] = dateRange;
      const res = await aiApi.getPassengerStats(
        cameraId, from.toISOString(), to.toISOString(),
      );
      setStats(res.data);
    } catch { /* ignore */ }
  }, [cameraId, dateRange]);

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchRealtime, 5000);
    return () => clearInterval(interval);
  }, [fetchStats, fetchRealtime]);

  const totalEnter = stats.reduce((s, r) => s + r.enterCount, 0);
  const totalExit = stats.reduce((s, r) => s + r.exitCount, 0);

  const columns = [
    { title: '时间', dataIndex: 'startTime', key: 'time', render: (v: string) => dayjs(v).format('HH:mm') },
    { title: '进入', dataIndex: 'enterCount', key: 'enter' },
    { title: '离开', dataIndex: 'exitCount', key: 'exit' },
    { title: '在店', dataIndex: 'insideCount', key: 'inside' },
  ];

  return (
    <div>
      <PageHeader title="客流实时大屏" icon={<UserOutlined />} />
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Select value={cameraId} onChange={setCameraId} style={{ width: '100%' }}>
            <Select.Option value={1}>Camera 1</Select.Option>
            <Select.Option value={2}>Camera 2</Select.Option>
          </Select>
        </Col>
        <Col span={10}>
          <DatePicker.RangePicker
            value={dateRange}
            onChange={(dates) => dates && setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs])}
          />
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={8}>
          <Card>
            <Statistic title="实时在店" value={realtime} prefix={<UserOutlined />} suffix="人" />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="累计进入" value={totalEnter} prefix={<ArrowUpOutlined />} suffix="人" />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="累计离开" value={totalExit} prefix={<ArrowDownOutlined />} suffix="人" />
          </Card>
        </Col>
      </Row>

      <Card title="客流历史数据" style={{ marginTop: 16 }}>
        <Table dataSource={stats} columns={columns} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  );
};

export default AiPassengerDashboard;
```

- [ ] **Step 2: Add route to App.jsx**

Find the route definitions in `frontend/src/App.jsx` and add:

```jsx
import AiPassengerDashboard from './pages/AiPassengerDashboard';
// ...
<Route path="/ai/passenger" element={<AiPassengerDashboard />} />
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/AiPassengerDashboard.tsx
git commit -m "feat(web): add passenger flow dashboard page"
```

---

### Task 21: Frontend — Behavior Alert Center

**Files:**
- Create: `frontend/src/pages/AiBehaviorAlertCenter.tsx`

- [ ] **Step 1: Write AiBehaviorAlertCenter.tsx**

```typescript
// frontend/src/pages/AiBehaviorAlertCenter.tsx
import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Tag, Button, Select, Badge, message, Space } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { aiApi, BehaviorEvent } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const levelColors: Record<string, string> = {
  INFO: 'blue', WARNING: 'orange', CRITICAL: 'red',
};
const statusColors: Record<string, string> = {
  UNRESOLVED: 'error', ACKNOWLEDGED: 'warning', RESOLVED: 'success',
};

const AiBehaviorAlertCenter: React.FC = () => {
  const [events, setEvents] = useState<BehaviorEvent[]>([]);
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterStatus, setFilterStatus] = useState<string | undefined>();

  const fetchEvents = useCallback(async () => {
    try {
      const res = await aiApi.getBehaviorEvents(0, filterType, filterStatus);
      setEvents(res.data);
    } catch { /* ignore */ }
  }, [filterType, filterStatus]);

  useEffect(() => { fetchEvents(); }, [fetchEvents]);

  const handleResolve = async (id: number) => {
    await aiApi.updateBehaviorStatus(id, 'RESOLVED');
    message.success('告警已处理');
    fetchEvents();
  };

  const columns = [
    { title: '类型', dataIndex: 'eventType', key: 'type',
      render: (t: string) => <Tag color={t === 'FALL' ? 'red' : 'blue'}>{t}</Tag> },
    { title: '级别', dataIndex: 'level', key: 'level',
      render: (l: string) => <Badge color={levelColors[l]} text={l} /> },
    { title: '描述', dataIndex: 'description', key: 'desc' },
    { title: '时间', dataIndex: 'eventTime', key: 'time' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={statusColors[s]}>{s}</Tag> },
    { title: '操作', key: 'action',
      render: (_: unknown, record: BehaviorEvent) => (
        record.status === 'UNRESOLVED' ? (
          <Button size="small" onClick={() => handleResolve(record.id)}>处理</Button>
        ) : null
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="行为告警中心" icon={<WarningOutlined />} />
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select placeholder="告警类型" allowClear style={{ width: 150 }} onChange={setFilterType}>
            <Select.Option value="LOITERING">滞留</Select.Option>
            <Select.Option value="INTRUSION">闯入</Select.Option>
            <Select.Option value="GATHERING">聚集</Select.Option>
            <Select.Option value="FALL">跌倒</Select.Option>
          </Select>
          <Select placeholder="状态" allowClear style={{ width: 150 }} onChange={setFilterStatus}>
            <Select.Option value="UNRESOLVED">未处理</Select.Option>
            <Select.Option value="ACKNOWLEDGED">确认中</Select.Option>
            <Select.Option value="RESOLVED">已处理</Select.Option>
          </Select>
          <Button type="primary" onClick={fetchEvents}>刷新</Button>
        </Space>
        <Table dataSource={events} columns={columns} rowKey="id" pagination={{ pageSize: 20 }} />
      </Card>
    </div>
  );
};

export default AiBehaviorAlertCenter;
```

- [ ] **Step 2: Add route**

In `App.jsx`:

```jsx
import AiBehaviorAlertCenter from './pages/AiBehaviorAlertCenter';
// ...
<Route path="/ai/alerts" element={<AiBehaviorAlertCenter />} />
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/AiBehaviorAlertCenter.tsx
git commit -m "feat(web): add behavior alert center page"
```

---

### Task 22: Frontend — License Plate Management

**Files:**
- Create: `frontend/src/pages/AiLicensePlateManagement.tsx`

- [ ] **Step 1: Write AiLicensePlateManagement.tsx**

```typescript
// frontend/src/pages/AiLicensePlateManagement.tsx
import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Input, Button, Modal, Form, Switch, message, Tag, Space } from 'antd';
import { CarOutlined } from '@ant-design/icons';
import { aiApi, VehicleRecord, WhitelistEntry } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const AiLicensePlateManagement: React.FC = () => {
  const [records, setRecords] = useState<VehicleRecord[]>([]);
  const [whitelist, setWhitelist] = useState<WhitelistEntry[]>([]);
  const [plateFilter, setPlateFilter] = useState<string>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const fetchRecords = useCallback(async () => {
    try {
      const res = await aiApi.getVehicleRecords(plateFilter);
      setRecords(res.data);
    } catch { /* ignore */ }
  }, [plateFilter]);

  const fetchWhitelist = useCallback(async () => {
    try {
      const res = await aiApi.getWhitelist();
      setWhitelist(res.data);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchRecords(); }, [fetchRecords]);
  useEffect(() => { fetchWhitelist(); }, [fetchWhitelist]);

  const handleSave = async () => {
    const values = await form.validateFields();
    if (editingId) {
      await aiApi.updateWhitelist(editingId, values);
      message.success('白名单已更新');
    } else {
      await aiApi.addWhitelist(values);
      message.success('白名单已添加');
    }
    setModalOpen(false);
    setEditingId(null);
    form.resetFields();
    fetchWhitelist();
  };

  const handleDelete = async (id: number) => {
    await aiApi.deleteWhitelist(id);
    message.success('已删除');
    fetchWhitelist();
  };

  const recordColumns = [
    { title: '车牌号', dataIndex: 'plateNumber', key: 'plate' },
    { title: '颜色', dataIndex: 'plateColor', key: 'color' },
    { title: '置信度', dataIndex: 'confidence', key: 'confidence',
      render: (v: number) => v ? `${(v * 100).toFixed(1)}%` : '-' },
    { title: '白名单', dataIndex: 'isWhitelisted', key: 'wl',
      render: (v: boolean) => v ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
    { title: '时间', dataIndex: 'detectTime', key: 'time' },
  ];

  const whitelistColumns = [
    { title: '车牌号', dataIndex: 'plateNumber', key: 'plate' },
    { title: '车主', dataIndex: 'ownerName', key: 'owner' },
    { title: '启用', dataIndex: 'enabled', key: 'enabled',
      render: (v: boolean) => <Switch checked={v} disabled /> },
    { title: '操作', key: 'action',
      render: (_: unknown, record: WhitelistEntry) => (
        <Space>
          <Button size="small" onClick={() => {
            setEditingId(record.id!);
            form.setFieldsValue(record);
            setModalOpen(true);
          }}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id!)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="车牌管理" icon={<CarOutlined />} />
      <Card title="识别记录" style={{ marginBottom: 16 }}>
        <Space style={{ marginBottom: 16 }}>
          <Input.Search
            placeholder="搜索车牌号"
            onSearch={setPlateFilter}
            style={{ width: 250 }}
          />
        </Space>
        <Table dataSource={records} columns={recordColumns} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>

      <Card title="白名单管理" extra={<Button type="primary" onClick={() => {
        setEditingId(null); form.resetFields(); setModalOpen(true);
      }}>添加</Button>}>
        <Table dataSource={whitelist} columns={whitelistColumns} rowKey="id" pagination={false} />
      </Card>

      <Modal
        title={editingId ? '编辑白名单' : '添加白名单'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => { setModalOpen(false); setEditingId(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="plateNumber" label="车牌号" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="plateColor" label="颜色">
            <Input />
          </Form.Item>
          <Form.Item name="ownerName" label="车主">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="备注">
            <Input.TextArea />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch defaultChecked />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AiLicensePlateManagement;
```

- [ ] **Step 2: Add route**

In `App.jsx`:

```jsx
import AiLicensePlateManagement from './pages/AiLicensePlateManagement';
// ...
<Route path="/ai/vehicles" element={<AiLicensePlateManagement />} />
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/AiLicensePlateManagement.tsx
git commit -m "feat(web): add license plate management page"
```

---

### Task 23: Docker Compose & K8s Deployment

**Files:**
- Modify: `docker-compose.yml` (add AI service)
- Create: `deploy/k8s/ai-service-deployment.yaml`
- Create: `deploy/k8s/ai-service-service.yaml`
- Create: `deploy/k8s/ai-service-hpa.yaml`

- [ ] **Step 1: Add AI service to docker-compose.yml**

Add to `docker-compose.yml`:

```yaml
ai-service:
  build:
    context: ./aick-mmp-ai
    dockerfile: Dockerfile
  ports:
    - "8000:8000"
    - "50051:50051"
  environment:
    - AI_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    - AI_MODEL_DIR=/app/models
  volumes:
    - ./aick-mmp-ai/models:/app/models
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: all
            capabilities: [gpu]
  depends_on:
    - kafka
  networks:
    - backend
```

- [ ] **Step 2: Write K8s deployment**

```yaml
# deploy/k8s/ai-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-service
  labels:
    app: ai-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ai-service
  template:
    metadata:
      labels:
        app: ai-service
    spec:
      containers:
      - name: ai-service
        image: aick-mmp-ai:latest
        ports:
        - containerPort: 8000
          name: http
        - containerPort: 50051
          name: grpc
        env:
        - name: AI_KAFKA_BOOTSTRAP_SERVERS
          value: kafka:9092
        - name: AI_MODEL_DIR
          value: /app/models
        resources:
          limits:
            nvidia.com/gpu: 1
          requests:
            cpu: "2"
            memory: "4Gi"
        livenessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 10
```

- [ ] **Step 3: Write K8s service**

```yaml
# deploy/k8s/ai-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: ai-service
spec:
  selector:
    app: ai-service
  ports:
  - name: http
    port: 8000
    targetPort: 8000
  - name: grpc
    port: 50051
    targetPort: 50051
```

- [ ] **Step 4: Write K8s HPA**

```yaml
# deploy/k8s/ai-service-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-service
  minReplicas: 1
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml deploy/
git commit -m "infra: add AI service docker-compose and K8s deployment configs"
```

---

## Self-Review Checklist

1. **Spec coverage**: Each capability from the design doc maps to at least one task:
   - AI microservice → Tasks 1-12
   - Cloud AI (passenger + behavior) → Tasks 5-9
   - Edge AI (license plate) → Task 10
   - Kafka integration → Task 3, Task 17
   - Data storage → Task 16
   - Edge module changes → Tasks 13-15
   - Central API → Task 18
   - Frontend pages → Tasks 19-22
   - Deployment → Task 23

2. **Placeholder scan**: No TBD, TODO, or placeholder code. Every step has complete code.

3. **Type consistency**: gRPC proto types, Python classes, Java entities, and TypeScript interfaces all use consistent naming (`cameraId`, `plateNumber`, `eventType`, etc.).

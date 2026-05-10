# v2 AI Analysis System Design

## Overview

将当前系统的示例级帧差分运动检测升级为真正的 AI 驱动视频分析能力，通过独立的 Python AI 微服务实现客流统计、行为识别和车牌识别。

## Architecture

```
Camera ──RTSP──► Edge Node (Java)
                │
                ├── FrameExtractor (定时截帧)
                │
                └──gRPC streaming──► AI Microservice (Python FastAPI)
                                     │
                                     ├── Passenger Flow (YOLOv8 + ByteTrack)
                                     ├── Behavior Recognition (OpenPifPaf + Rules)
                                     └── License Plate (YOLOv8-plate + LPRNet)
                                          │
                                     Kafka ──► Central Service (Java)
                                     (4 topics) │
                                                ├── WebSocket ──► Frontend
                                                ├── MySQL Storage
                                                └── Redis Cache
```

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Frame transport | gRPC streaming | Edge→AI direct, low latency, native streaming support |
| Edge AI deployment | Centralized | All AI on GPU-equipped server; edge stays lightweight Java |
| Implementation scope | All at once | Complete 3 analysis capabilities in single project |
| AI framework | FastAPI + PyTorch(dev) / ONNX Runtime(prod) | Async perf, auto OpenAPI, cross-platform |
| Object detection | YOLOv8 | High accuracy, rich pretrained models |
| Pose estimation | OpenPifPaf | Lightweight, multi-person, open source |
| License plate | YOLOv8-plate + LPRNet | End-to-end, high accuracy, open source |
| Message bus | Kafka (4 topics) | Reuse existing Kafka infra, high throughput |
| Database | MySQL 8.0 | Project uses MySQL, not PostgreSQL |
| Frontend | TypeScript | New AI pages in TypeScript, existing JS pages preserved |

## AI Microservice (`aick-mmp-ai/`)

### Project Structure

```
aick-mmp-ai/
├── src/
│   ├── api/                    # FastAPI routes
│   │   └── main.py             # Entry, health, metrics
│   ├── core/
│   │   ├── config.py           # Config loader (YAML + env)
│   │   └── model_loader.py     # ONNX model warm-load
│   ├── services/
│   │   ├── detector.py         # YOLOv8 detection
│   │   ├── tracker.py          # ByteTrack multi-object tracker
│   │   ├── passenger_analyzer.py  # Entry/exit counting
│   │   ├── pose_estimator.py   # OpenPifPaf pose estimation
│   │   ├── behavior_engine.py  # Loitering/intrusion/gathering/fall rules
│   │   └── plate_recognizer.py # YOLOv8-plate + LPRNet
│   ├── integrations/
│   │   ├── grpc_server.py      # gRPC frame receiver
│   │   └── kafka_producer.py   # Result publisher
│   └── proto/
│       └── frame.proto         # gRPC protobuf definition
├── models/                     # ONNX model files
├── Dockerfile                  # CUDA-enabled
└── requirements.txt
```

### gRPC Interface

```protobuf
service FrameAnalysis {
  rpc AnalyzeFrame(stream FrameRequest) returns (stream AnalysisResult);
}

message FrameRequest {
  string camera_id = 1;
  string edge_node_id = 2;
  bytes frame_data = 3;       // JPEG encoded
  int64 timestamp = 4;
  repeated string analysis_types = 5;  // ["passenger", "behavior", "plate"]
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
```

### Analysis Services

**Passenger Flow:**
- YOLOv8 detects persons per frame
- ByteTrack assigns tracking IDs across frames
- Direction logic: entry/exit determined by y-coordinate trajectory across virtual line
- Zone counting for area occupancy

**Behavior Recognition:**
- OpenPifPaf extracts keypoints (17-point skeleton)
- Rule engine detects:
  - Loitering: same person in frame > threshold time
  - Intrusion: person in restricted zone
  - Gathering: person count in zone > threshold
  - Fall: keypoint geometry indicating horizontal pose

**License Plate:**
- YOLOv8-plate detects plate region
- LPRNet recognizes characters from cropped region
- Whitelist/blacklist matching against central service data

## Kafka Integration

### Topics (4 total)

| Topic | Key | Content | Producer | Consumer |
|-------|-----|---------|----------|----------|
| `ai-passenger-stats` | cameraId | Periodic passenger counts | AI Service | Central Service |
| `ai-behavior-events` | cameraId | Behavior alert events | AI Service | Central Service |
| `ai-vehicle-records` | plateNumber | License plate recognition | AI Service | Central Service |
| `ai-dlq` | originalKey | Failed messages | Central Service | Central Service |

### Delivery Guarantees
- At-least-once delivery for analysis results
- Idempotent consumers (Redis-based dedup)
- Retry + DLQ for failed messages

## Edge Module Changes (`aick-mmp-edge`)

### New Components
- **FrameExtractor**: Scheduled frame capture from RTSP streams (configurable FPS: 1fps for passenger, 5fps for plate)
- **FrameAnalysisGrpcClient**: Maintains gRPC stream per camera, sends frames, receives results
- **Config**: Target AI service address, per-camera analysis type config, frame quality settings

### Removed
- `MotionDetectionServiceImpl` HTTP reporting logic (replaced by AI service)

## Central Service Changes (`aick-mmp-central`)

### New Kafka Consumers
- `AiPassengerStatsConsumer` → `ai_passenger_stats` table + Redis realtime cache
- `AiBehaviorEventConsumer` → `ai_behavior_events` table + alert notification
- `AiVehicleRecordConsumer` → `ai_vehicle_records` table + whitelist matching

### New REST API (`/api/v1/ai`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stats/passenger` | Historical passenger flow with time range |
| GET | `/stats/passenger/realtime/{cameraId}` | Real-time occupancy |
| GET | `/alerts/behavior` | Behavior event query (type, level, status) |
| PUT | `/alerts/behavior/{id}/status` | Acknowledge/resolve alert |
| GET | `/vehicles/records` | License plate record query |
| GET/POST/PUT/DELETE | `/vehicles/whitelist` | Whitelist CRUD |

### Alert Integration
- Behavior events reuse existing `AlertNotificationService`
- Pushed via existing WebSocket (`/topic/alerts`)
- Alert record created in `ai_behavior_events` with optional link to `AlertRecord`

## Data Storage

### MySQL Tables

**`ai_passenger_stats`** — 5-minute aggregated passenger counts per camera
**`ai_behavior_events`** — Individual behavior events with position data and snapshot reference
**`ai_vehicle_records`** — License plate recognition records
**`ai_vehicle_whitelist`** — Whitelist for authorized vehicles

### Redis Cache Keys
- `ai:passenger:realtime:{cameraId}` — current occupancy, TTL 5min
- `ai:passenger:daily:{cameraId}:{yyyyMMdd}` — daily cumulative, TTL 48h
- `ai:behavior:recent:{cameraId}` — last 20 events, TTL 10min

### Object Storage (MinIO)
- Bucket: `ai-snapshots`
- Path: `{analysisType}/{cameraId}/{date}/{timestamp}.jpg`
- Retention: 7 days (configurable)

## Frontend Pages

Three new TypeScript pages built with React + Ant Design 5.x:

1. **Passenger Flow Dashboard** — real-time occupancy gauge, hourly/daily charts, camera selection, animated counters
2. **Behavior Alert Center** — event list with filters (type/level/status), snapshot viewer, one-click acknowledge/resolve
3. **License Plate Management** — plate record search, whitelist CRUD, blacklist alerts, entry/exit logs

All pages use TypeScript and reuse existing WebSocket connection for real-time updates.

## Implementation Plan

### Phase 1: Infrastructure (Week 1-2)
- Create `aick-mmp-ai/` project skeleton
- Implement config, model loader, gRPC server, Kafka producer
- Dockerfile with CUDA, docker-compose integration
- Edge: FrameExtractor + gRPC client

### Phase 2: Core Analysis (Week 3-6)
- YOLOv8 detection service + ByteTrack tracker
- Passenger flow counting engine
- Behavior recognition (pose estimation + rule engine)
- License plate detection + recognition

### Phase 3: Backend Integration (Week 7-8)
- 3 Kafka consumers in central service
- AI management REST API
- Alert integration with existing notification system
- Database tables + migrations

### Phase 4: Frontend (Week 9-11)
- Passenger flow dashboard page
- Behavior alert center page
- License plate management page
- WebSocket real-time updates

### Phase 5: Testing & Deployment (Week 12-14)
- Unit tests for AI services
- Integration tests (gRPC, Kafka, DB)
- Performance benchmarking + GPU optimization
- K8s deployment configs (Deployment, HPA, GPU scheduling)
- Prometheus + Grafana monitoring
- Production rollout with blue-green deployment

## Rollback Strategy
- Kafka consumers support version-tolerant message formats
- Database schema backward-compatible (no destructive migrations)
- Blue-green deployment for AI service
- Edge nodes can fall back to no-op if AI service unreachable

## Existing Proposal Corrections
1. **Database**: PostgreSQL references corrected to MySQL 8.0
2. **Kafka topics**: Reduced from 6+1 to 4 (frame transport removed from Kafka)
3. **Edge→Kafka**: Removed — edge uses gRPC directly to AI service
5. **Edge AI**: LPR centralized, not deployed on edge nodes

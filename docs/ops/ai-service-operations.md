# AI Analysis Service Operations Manual

## Architecture Overview

```
Edge Node → gRPC stream → AI Service (FastAPI + ONNX Runtime)
                              ↓
                    Kafka 3 Topics → Central Service (Java/Spring)
                                          ↓
                                   Frontend (React, 5s polling)
```

### Key Components
- **AI Service**: Python FastAPI with ONNX Runtime, GPU-accelerated inference
- **gRPC Transport**: Bidirectional streaming for frame transport (Edge → AI)
- **Kafka Integration**: 3 topics + DLQ for analysis results
- **MinIO Storage**: Snapshot storage for behavior alerts and vehicle records

## Service Configuration

### AI Service Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `AI_MODEL_DIR` | `/app/models` | ONNX model storage path |
| `AI_GRPC_PORT` | `50051` | gRPC server port |
| `AI_HTTP_PORT` | `8000` | HTTP API port |
| `AI_LOG_LEVEL` | `INFO` | Logging level |
| `AI_PROMETHEUS_ENABLED` | `true` | Enable Prometheus metrics |

### Central Service Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin123` | MinIO secret key |
| `MINIO_BUCKET_NAME` | `ai-snapshots` | MinIO bucket name |

## Health Checks

### AI Service
```bash
# HTTP health
curl http://ai-service:8000/health

# gRPC health (requires grpcurl)
grpcurl -plaintext ai-service:50051 grpc.health.v1.Health/Check

# Prometheus metrics
curl http://ai-service:8000/metrics

# Model list
curl http://ai-service:8000/models
```

### Central Service
```bash
curl http://central-1:8080/api/actuator/health
curl http://central-1:8080/api/actuator/prometheus
```

## Monitoring & Alerts

### Prometheus
Prometheus is configured to scrape:
- AI Service: `/metrics` on port 8000
- Central Service: `/api/actuator/prometheus` on port 8080
- Edge Nodes: `/api/actuator/prometheus` on port 8080

**Alert Rules** (see `deploy/prometheus/rules/`):
- `AIServiceDown`: AI Service offline > 1 minute → **CRITICAL**
- `HighGPULoad`: GPU > 80% for 5 minutes → **WARNING**
- `KafkaConsumerLag`: Consumer backlog > 1000 → **WARNING**
- `HighFrameProcessingLatency`: P95 > 500ms → **WARNING**

### Grafana
Pre-built dashboard at `deploy/grafana/dashboards/ai-service-dashboard.json`

Key panels:
- GPU Utilization gauge
- Frame Processing Latency (P95) timeseries
- Active Video Streams counter
- Kafka Producer Rate
- Detection Rate
- Memory Usage
- Behavior Events by Type pie chart
- Kafka Consumer Lag

## Troubleshooting

### High GPU Memory
```bash
# Check GPU utilization
nvidia-smi

# Check AI Service logs
docker logs aick-mmp-ai-service

# Restart service
docker-compose restart ai-service
```

### Kafka Consumer Lag
```bash
# Check consumer group status
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group ai-analysis-group --describe

# Check DLQ for failed messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic ai-dlq --from-beginning
```

### gRPC Connection Issues
```bash
# Verify gRPC port is listening
netstat -an | grep 50051

# Test with grpcurl
grpcurl -plaintext ai-service:50051 list

# Check Edge node logs for gRPC errors
docker logs aick-mmp-edge-node-1 | grep -i grpc
```

### MinIO Storage
```bash
# Check bucket exists
mc ls local/ai-snapshots

# List all snapshots
mc find local/ai-snapshots --older-than 7d
```

## Performance Tuning

### Frame Rate Configuration
Per-camera analysis frame rates (configured via AI Config Management page):
| Analysis Type | Default FPS | Range |
|--------------|-------------|-------|
| Passenger | 1 | 0.5 - 5 |
| Behavior | 2 | 1 - 10 |
| Plate | 5 | 1 - 15 |

### GPU Resource Estimation
| Concurrent Streams | GPU Required | VRAM |
|-------------------|--------------|------|
| 50 | RTX 3060 | 12GB |
| 100 | RTX 3090 | 24GB |
| 200 | RTX 4090 | 24GB |
| 500 | A100 | 80GB |

## Scaling

### Horizontal Scaling
AI Service supports horizontal scaling via Kubernetes HPA:
```bash
kubectl scale deployment ai-service --replicas=3
```

HPA triggers:
- GPU utilization > 70% → scale up
- CPU > 70% → scale up
- Cooldown: 60s scale up / 300s scale down

### Kafka Partition Scaling
Each AI analysis topic has 3 partitions by default:
```
ai-passenger-stats: 3 partitions
ai-behavior-events: 3 partitions  
ai-vehicle-records: 3 partitions
ai-dlq: 1 partition
```

## Backup & Recovery

### MinIO Snapshots
Snapshots are stored in MinIO bucket `ai-snapshots`:
```bash
# Backup all snapshots
mc mirror local/ai-snapshots backup/minio/ai-snapshots-$(date +%Y%m%d)

# Retention: 30 days (configure via MinIO lifecycle policy)
```

### Database
AI analysis data is persisted in MySQL:
- `ai_passenger_stats`: Passenger flow data (retention: 90 days)
- `ai_behavior_events`: Behavior alerts (retention: 30 days)
- `ai_vehicle_records`: License plate records (retention: 90 days)
- `ai_vehicle_whitelist`: Whitelist (permanent)
- `ai_vehicle_blacklist`: Blacklist (permanent)
- `ai_analysis_config`: Per-camera config (permanent)

## Deployment

### Docker Compose (Development)
```bash
docker-compose up -d minio ai-service
```

### Kubernetes (Production)
```bash
kubectl apply -f deploy/k8s/ai-service-deployment.yaml
kubectl apply -f deploy/k8s/ai-service-service.yaml
kubectl apply -f deploy/k8s/ai-service-hpa.yaml
```

### gRPC TLS (Production)
For production deployments, enable gRPC TLS:
1. Generate certificates:
```bash
openssl req -x509 -newkey rsa:4096 -keyout server.key -out server.crt -days 365 -nodes
```
2. Mount certificates to AI Service container
3. Set `use_tls: true` in `AiServiceConfig.java`
4. Update Edge node truststore with CA certificate

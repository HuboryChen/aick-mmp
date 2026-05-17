# AI Service Performance Benchmark

## Test Environment Requirements

- GPU: NVIDIA RTX 3090 or equivalent (24GB VRAM)
- CPU: 8+ cores
- Memory: 16GB+
- Network: 1Gbps between Edge nodes and AI Service

## Benchmark Scenarios

### 1. Single Stream Latency

Test: Frame processing latency for a single video stream.

```bash
# Using grpcurl to send test frames
grpcurl -plaintext -d '{
  "camera_id": 1,
  "frame_data": "<base64-encoded-test-frame>",
  "width": 640,
  "height": 480,
  "analysis_types": ["passenger", "behavior", "plate"]
}' ai-service:50051 ai.AnalysisService/AnalyzeFrame
```

**Expected**: P95 latency < 500ms

### 2. Concurrent Stream Scaling

Test: Gradually increase concurrent streams and measure throughput.

```bash
# Use the built-in benchmark endpoint
curl -X POST http://ai-service:8000/benchmark/start \
  -H "Content-Type: application/json" \
  -d '{"concurrent_streams": 10, "duration_seconds": 60}'
```

| Concurrent Streams | Expected FPS | Expected GPU Util |
|-------------------|-------------|-------------------|
| 10 | 50+ | 20-30% |
| 25 | 100+ | 40-60% |
| 50 | 150+ | 70-85% |
| 100 | 200+ | 90-95% |

### 3. Kafka Throughput

Test: Kafka producer throughput under load.

```bash
# Check producer metrics
curl http://ai-service:8000/metrics | grep ai_kafka_messages_produced_total
```

**Expected**: > 1000 messages/second sustained

### 4. gRPC Connection Stability

Test: Maintain gRPC connection for extended period.

| Duration | Expected Result |
|----------|----------------|
| 1 hour | 0 disconnects |
| 8 hours | < 3 disconnects |
| 24 hours | < 10 disconnects |

## Performance Test Script

```python
#!/usr/bin/env python3
"""AI Service Performance Benchmark"""
import time
import grpc
import psutil
import subprocess
from concurrent.futures import ThreadPoolExecutor

# Configuration
AI_SERVICE_HOST = "localhost"
GRPC_PORT = 50051
NUM_STREAMS = [1, 5, 10, 25, 50]
DURATION_SECONDS = 30

def measure_single_stream():
    """Measure single stream latency"""
    import ai_pb2
    import ai_pb2_grpc

    channel = grpc.insecure_channel(f"{AI_SERVICE_HOST}:{GRPC_PORT}")
    stub = ai_pb2_grpc.AnalysisServiceStub(channel)

    latencies = []
    for i in range(100):
        start = time.time()
        # Send test frame
        response = stub.AnalyzeFrame(ai_pb2.FrameRequest(
            camera_id=1,
            frame_data=b"test_frame_data",
            width=640, height=480
        ))
        latency = (time.time() - start) * 1000
        latencies.append(latency)

    latencies.sort()
    n = len(latencies)
    return {
        "p50": latencies[int(n * 0.5)],
        "p95": latencies[int(n * 0.95)],
        "p99": latencies[int(n * 0.99)],
        "avg": sum(latencies) / n,
        "min": min(latencies),
        "max": max(latencies),
    }

def measure_concurrent_streams(num_streams):
    """Measure concurrent stream throughput"""
    def stream_worker(stream_id):
        # Simulate frame sending for DURATION_SECONDS
        frames_sent = 0
        start = time.time()
        while time.time() - start < DURATION_SECONDS:
            # Send frame
            frames_sent += 1
            time.sleep(0.033)  # ~30fps
        return frames_sent

    with ThreadPoolExecutor(max_workers=num_streams) as executor:
        results = list(executor.map(stream_worker, range(num_streams)))

    total_frames = sum(results)
    return {
        "streams": num_streams,
        "total_frames": total_frames,
        "fps": total_frames / DURATION_SECONDS,
        "frames_per_stream": total_frames / num_streams / DURATION_SECONDS,
    }

if __name__ == "__main__":
    print("=== AI Service Performance Benchmark ===\n")

    print("1. Single Stream Latency (ms):")
    latency_results = measure_single_stream()
    for k, v in latency_results.items():
        print(f"   {k}: {v:.2f}ms")

    print("\n2. Concurrent Stream Scaling:")
    for ns in NUM_STREAMS:
        result = measure_concurrent_streams(ns)
        print(f"   {result['streams']} streams: {result['fps']:.1f} FPS "
              f"({result['frames_per_stream']:.1f} FPS/stream)")
```

## Stress Test Plan

### Phase 1: Baseline
- 1 stream for 30 minutes
- Record all metrics (latency, CPU, GPU, memory)

### Phase 2: Load Test
- Ramp up from 1 to 50 streams over 10 minutes
- Hold at 50 streams for 30 minutes
- Observe GPU utilization and latency degradation

### Phase 3: Stress Test
- Push to 100+ streams
- Identify breaking point (latency > 2000ms or OOM)
- Monitor Kafka consumer lag

### Phase 4: Recovery
- Drop to 10 streams
- Measure recovery time
- Verify no data loss

## Success Criteria

| Metric | Target |
|--------|--------|
| Single-stream P95 latency | < 500ms |
| 50-stream throughput | > 150 FPS |
| GPU utilization (100 streams) | < 95% |
| Kafka consumer lag (normal) | < 100 |
| gRPC connection stability | 0 drops/hour |
| Memory leak | < 5% over 24h |

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

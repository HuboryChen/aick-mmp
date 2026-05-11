# aick-mmp-ai/tests/test_config.py
from src.core.config import settings


def test_settings_defaults():
    assert settings.service_name == "ai-analysis"
    assert settings.grpc_port == 50051
    assert settings.kafka_bootstrap_servers == "localhost:9092"
    assert settings.detection_confidence == 0.5

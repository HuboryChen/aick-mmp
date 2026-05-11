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
        self.get_detector()
        self.get_pose()
        self.get_plate_detector()
        self.get_plate_recognizer()


model_manager = ModelManager()

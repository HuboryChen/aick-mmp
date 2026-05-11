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

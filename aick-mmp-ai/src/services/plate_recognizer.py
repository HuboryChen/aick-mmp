from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
import onnxruntime as ort

from src.core.model_loader import model_manager
from src.services.detector import YOLODetector


@dataclass
class PlateResult:
    plate_number: str
    plate_color: str
    confidence: float
    bbox: tuple[float, float, float, float]


class PlateRecognizer:
    def __init__(self):
        self._detector = YOLODetector()
        self._rec_session: Optional[ort.InferenceSession] = None

    def _ensure_rec_loaded(self):
        if self._rec_session is None:
            self._rec_session = model_manager.get_plate_recognizer()

    def recognize(self, frame_bytes: bytes) -> list[PlateResult]:
        plate_dets = self._detector.detect(frame_bytes)
        if not plate_dets:
            return []

        self._ensure_rec_loaded()
        results = []
        img = cv2.imdecode(np.frombuffer(frame_bytes, np.uint8), cv2.IMREAD_COLOR)
        h, w = img.shape[:2]

        for det in plate_dets:
            x1, y1, x2, y2 = map(int, [det.x1 * w, det.y1 * h, det.x2 * w, det.y2 * h])
            plate_crop = img[y1:y2, x1:x2]
            if plate_crop.size == 0:
                continue

            crop_gray = cv2.cvtColor(plate_crop, cv2.COLOR_BGR2GRAY)
            resized = cv2.resize(crop_gray, (94, 24))
            input_tensor = resized.astype(np.float32)[np.newaxis, np.newaxis, ...] / 255.0

            input_name = self._rec_session.get_inputs()[0].name
            logits = self._rec_session.run(None, {input_name: input_tensor})[0]

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

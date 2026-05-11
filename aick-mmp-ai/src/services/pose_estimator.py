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
    keypoints: list[Keypoint]
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
        resized = cv2.resize(img, (256, 256))
        input_tensor = np.transpose(resized.astype(np.float32) / 255.0, (2, 0, 1))[np.newaxis, ...]

        input_name = self._session.get_inputs()[0].name
        outputs = self._session.run(None, {input_name: input_tensor})

        poses = []
        heatmaps = outputs[0]
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

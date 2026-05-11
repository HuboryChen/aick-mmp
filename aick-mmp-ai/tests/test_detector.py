import numpy as np
import pytest

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

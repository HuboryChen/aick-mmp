import sys
from unittest.mock import MagicMock

sys.modules["onnxruntime"] = MagicMock()

import pytest
from src.services.pose_estimator import PoseEstimator


def test_pose_estimator_raises_without_model():
    estimator = PoseEstimator()
    with pytest.raises(FileNotFoundError):
        estimator.estimate(b"fake_jpeg")

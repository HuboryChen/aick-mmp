import sys
from unittest.mock import MagicMock

sys.modules["onnxruntime"] = MagicMock()

import pytest
from src.services.plate_recognizer import PlateRecognizer


def test_plate_recognizer_raises_without_model():
    recognizer = PlateRecognizer()
    with pytest.raises(FileNotFoundError):
        recognizer.recognize(b"fake_jpeg")

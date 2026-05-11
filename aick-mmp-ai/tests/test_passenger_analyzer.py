import sys
from unittest.mock import MagicMock
from dataclasses import dataclass

# Mock onnxruntime to bypass detector.py import issue with Python 3.14
sys.modules['onnxruntime'] = MagicMock()

from src.services.passenger_analyzer import PassengerAnalyzer


@dataclass
class _Detection:
    x1: float
    y1: float
    x2: float
    y2: float
    confidence: float
    class_id: int


def test_passenger_enter():
    analyzer = PassengerAnalyzer(line_y=0.5)
    # Boxes must overlap enough for IoU tracker match (>0.3)
    # Box 1 above line (cy=0.4), Box 2 below line (cy=0.6), IoU=0.6
    analyzer.update([_Detection(0, 0, 10, 0.8, 0.9, 0)])  # cy=0.4
    result = analyzer.update([_Detection(0, 0.2, 10, 1.0, 0.9, 0)])  # cy=0.6
    assert result.enter_count == 1
    assert result.exit_count == 0


def test_passenger_exit():
    analyzer = PassengerAnalyzer(line_y=0.5)
    analyzer.update([_Detection(0, 0.2, 10, 1.0, 0.9, 0)])  # cy=0.6
    result = analyzer.update([_Detection(0, 0, 10, 0.8, 0.9, 0)])  # cy=0.4
    assert result.enter_count == 0
    assert result.exit_count == 1


def test_passenger_reset():
    analyzer = PassengerAnalyzer(line_y=0.5)
    analyzer.update([_Detection(0, 0, 10, 0.8, 0.9, 0)])
    analyzer.reset()
    result = analyzer.update([_Detection(0, 0.2, 10, 1.0, 0.9, 0)])
    assert result.enter_count == 0  # reset

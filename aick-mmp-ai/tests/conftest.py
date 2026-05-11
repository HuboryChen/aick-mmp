# aick-mmp-ai/tests/conftest.py
import pytest


@pytest.fixture
def sample_frame() -> bytes:
    """Return a minimal 640x480 JPEG frame for testing."""
    import numpy as np
    import cv2
    img = np.zeros((480, 640, 3), dtype=np.uint8)
    cv2.putText(img, "test", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
    _, buf = cv2.imencode(".jpg", img)
    return buf.tobytes()

from pathlib import Path
import pytest
from src.core.model_loader import ModelManager


def test_model_manager_raises_on_missing():
    manager = ModelManager()
    with pytest.raises(FileNotFoundError):
        manager.get_detector()

from dataclasses import dataclass
from enum import Enum
from typing import Optional


class AppState(str, Enum):
    BOOTSTRAP = "bootstrap"
    SPLASH = "splash"
    GUIDE = "guide"
    MAP = "map"
    PLAY = "play"
    PLAY_DIALOG = "play_dialog"
    OUT_OF_APP = "out_of_app"
    UNKNOWN = "unknown"


@dataclass(frozen=True)
class LevelSolution:
    level_id: int
    puzzle: str
    solution: str


@dataclass(frozen=True)
class DetectionResult:
    state: AppState
    package_name: Optional[str] = None
    activity_name: Optional[str] = None
    confidence: float = 0.0
    detail: str = ""

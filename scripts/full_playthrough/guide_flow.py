import time
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional


@dataclass(frozen=True)
class GuideStep:
    name: str
    detector: str
    action: str
    retry_limit: int = 2


GUIDE_STEPS = [
    GuideStep(name="intro_wait", detector="guide", action="wait"),
    GuideStep(name="intro_tap", detector="guide", action="tap_highlight"),
    GuideStep(name="demo_board", detector="guide", action="guide_board_action"),
]


def pick_guide_action(step: GuideStep) -> str:
    return step.action


def execute_basic_guide_progression(device, reporter, max_steps: int = 10,
                                    settle_seconds: float = 0.8) -> int:
    """Execute guide progression by tapping screen center repeatedly.

    The GuideActivity binds goNext() to the root layout, board, timer, and
    keypad area. Tapping center reliably advances through all guide pages
    until the map page is reached.

    After each tap the current activity is checked - if we've left the guide
    (map or play activity), we stop early.
    """
    completed_steps = 0
    for index in range(max_steps):
        # Check if we're still in the guide
        pkg, activity = device.current_package_and_activity()
        activity_lower = (activity or "").lower()
        if "map" in activity_lower or "play" in activity_lower:
            reporter.append_log("actions.log",
                                "guide_early_exit step=%d activity=%s" % (index, activity))
            break

        device.tap_screen_center()
        completed_steps += 1
        reporter.append_log("actions.log",
                            "guide_step=%d action=tap_screen_center" % (index + 1))
        time.sleep(settle_seconds)

    return completed_steps


def wait_for_map_state(device, detector, reporter,
                       max_wait: float = 10.0,
                       interval: float = 1.0) -> bool:
    """Poll until the app reaches the MAP state.

    Returns True if MAP was reached, False on timeout."""
    deadline = time.time() + max_wait
    while time.time() < deadline:
        pkg, activity = device.current_package_and_activity()
        if "map" in (activity or "").lower():
            reporter.append_log("states.log", "guide_done reached_map")
            return True
        time.sleep(interval)
        device.tap_screen_center()
    reporter.append_log("states.log", "guide_timeout")
    return False

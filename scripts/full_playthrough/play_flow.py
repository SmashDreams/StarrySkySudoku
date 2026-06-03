import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from .geometry import BoardRect, calc_board_rect, calc_keypad_centers, cell_center
from .models import AppState


def build_fill_plan(puzzle: str, solution: str) -> List[Tuple[int, int, int]]:
    steps = []
    for idx, cell in enumerate(puzzle):
        if cell == "0":
            row, col = divmod(idx, 9)
            steps.append((row, col, int(solution[idx])))
    return steps


def execute_fill_step(device, cell_centers, keypad_map,
                      row: int, col: int, digit: int,
                      tap_delay: float = 0.12,
                      key_delay: float = 0.08) -> None:
    """Tap a cell then tap the digit key."""
    cx, cy = cell_centers[row][col]
    device.tap(cx, cy)
    time.sleep(tap_delay)
    key_x, key_y = keypad_map[digit]
    device.tap(key_x, key_y)
    time.sleep(key_delay)


def should_restart_level(retry_count: int, retry_limit: int) -> bool:
    return retry_count >= retry_limit


def solve_single_level(
    device,
    detector,
    reporter,
    store,
    level_id: int,
    screenshot_dir: Path,
    config,
) -> dict:
    """Play through a single Sudoku level.

    Returns a level result dict with keys: level_id, status, reason, fill_count, total_fills.
    """
    level = store.get_level(level_id)
    fill_steps = build_fill_plan(level.puzzle, level.solution)
    total_fills = len(fill_steps)

    reporter.append_log("actions.log",
                        "level=%d start total_fills=%d" % (level_id, total_fills))

    # --- Screenshot ---
    ss_path = screenshot_dir / ("level_%02d_board.png" % level_id)
    device.screenshot_to(ss_path)

    # --- Locate cell centers via grid-line detection ---
    cell_centers = detector.locate_cell_centers(ss_path, retries=2)
    if cell_centers is None:
        # Fallback: layout-based
        br = calc_board_rect(detector.screen_width, detector.screen_height,
                             detector.density)
        board_rect = BoardRect(left=br.left, top=br.top, width=br.width,
                               height=br.height)
        cell_centers = [
            [cell_center(board_rect, r, c) for c in range(9)]
            for r in range(9)
        ]
        reporter.append_log("actions.log",
                            "level=%d cell_centers=layout_fallback" % level_id)
    else:
        reporter.append_log("actions.log",
                            "level=%d cell_centers=detected cell00=%s" % (
                                level_id, cell_centers[0][0]))

    # --- Locate keypad ---
    br_raw = (21, 481, 1110, 1110)  # dummy for keypad calc
    keypad_map = detector.locate_keypad(ss_path, br_raw, retries=1)
    reporter.append_log("actions.log",
                        "level=%d keypad_digits=%d key1=%s" % (
                            level_id, len(keypad_map), keypad_map.get(1, "?")))

    # --- Fill cells ---
    fill_count = 0
    retry_budget = config.level_retry_limit

    for step_index, (row, col, digit) in enumerate(fill_steps):
        try:
            execute_fill_step(device, cell_centers, keypad_map,
                              row, col, digit)
            fill_count += 1

            # Periodic state check
            if (step_index + 1) % 10 == 0:
                pkg, activity = device.current_package_and_activity()
                activity_lower = (activity or "").lower()
                if "play" not in activity_lower:
                    reporter.append_log("actions.log",
                                        "level=%d step=%d state=%s" % (
                                            level_id, step_index + 1, activity_lower))
                    if "map" in activity_lower:
                        break

        except Exception as exc:
            reporter.append_log("actions.log",
                                "level=%d step=%d error=%s" % (
                                    level_id, step_index + 1, exc))
            retry_budget -= 1
            if retry_budget <= 0:
                return {"level_id": level_id, "status": "failed",
                        "reason": "retry_exhausted", "fill_count": fill_count,
                        "total_fills": total_fills}
            time.sleep(0.3)

    # --- Post-completion wait ---
    time.sleep(1.0)
    for wait_i in range(6):
        pkg, activity = device.current_package_and_activity()
        activity_lower = (activity or "").lower()

        if "map" in activity_lower:
            reporter.append_log("actions.log",
                                "level=%d completed wait=%d" % (level_id, wait_i))
            return {"level_id": level_id, "status": "success",
                    "reason": "returned_to_map", "fill_count": fill_count,
                    "total_fills": total_fills}

        # Try dismissing any dialog
        if "dialog" in activity_lower:
            _handle_play_dialog(device, reporter, level_id)

        time.sleep(0.6)

    # Final check
    pkg, activity = device.current_package_and_activity()
    activity_lower = (activity or "").lower()
    status = "success" if "map" in activity_lower else "partial"
    reporter.append_log("actions.log",
                        "level=%d final=%s fill=%d/%d activity=%s" % (
                            level_id, status, fill_count, total_fills, activity_lower))
    return {"level_id": level_id, "status": status,
            "reason": "fill_complete", "fill_count": fill_count,
            "total_fills": total_fills}


def _handle_play_dialog(device, reporter, level_id):
    """Try to dismiss any dialog on the play screen."""
    pkg, activity = device.current_package_and_activity()
    reporter.append_log("actions.log",
                        "level=%d dialog_handler activity=%s" % (
                            level_id, activity))
    device.tap_screen_center()
    time.sleep(0.5)
    device.tap(576, int(2376 * 0.55))
    time.sleep(0.5)


def enter_level_from_map(device, detector, reporter,
                         level_id: int, screenshot_dir: Path) -> bool:
    """Navigate from MAP to a specific level's PLAY screen."""
    ss = screenshot_dir / "map_pre_enter.png"
    device.screenshot_to(ss)
    columns = detector.find_map_star_columns(ss)

    col_idx = (level_id - 1) % 4
    stars_in_col = columns.get(col_idx, [])
    if not stars_in_col:
        reporter.append_log("actions.log",
                            "map_level=%d no_stars_in_col=%d" % (level_id, col_idx))
        return False

    target_star = max(stars_in_col, key=lambda p: p[1])
    cx, cy = target_star
    reporter.append_log("actions.log",
                        "map_level=%d tap_star=(%d,%d) col=%d" % (
                            level_id, cx, cy, col_idx))
    device.tap(cx, cy)
    time.sleep(1.2)

    ss_dialog = screenshot_dir / "map_dialog.png"
    device.screenshot_to(ss_dialog)
    button = detector.find_dialog_button(ss_dialog)
    if button:
        bx, by = button
        reporter.append_log("actions.log",
                            "map_level=%d tap_dialog=(%d,%d)" % (level_id, bx, by))
        device.tap(bx, by)
        time.sleep(0.8)
    else:
        device.tap_screen_center()
        time.sleep(0.8)

    # Handle permission dialogs
    for _ in range(3):
        pkg, activity = device.current_package_and_activity()
        if "play" in (activity or "").lower():
            return True
        if "permission" in (activity or "").lower():
            device.tap(951, 2067)  # Allow button
            time.sleep(0.8)
            continue
        time.sleep(0.5)

    for retry in range(3):
        time.sleep(0.4)
        pkg, activity = device.current_package_and_activity()
        if "play" in (activity or "").lower():
            return True
        if retry == 0:
            device.tap(576, int(2376 * 0.52))
        if retry == 1:
            device.tap(cx, cy)
            time.sleep(0.8)
            device.tap(576, int(2376 * 0.52))

    return False

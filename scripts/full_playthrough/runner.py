#!/usr/bin/env python3
"""Full StarrySkySudoku playthrough: install -> guide -> all 40 levels."""
import argparse
import json
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent.parent
DATA_DIR = Path(__file__).resolve().parent / "data"
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from scripts.full_playthrough.config import RuntimeConfig
from scripts.full_playthrough.detector import ScreenDetector
from scripts.full_playthrough.geometry import calc_board_rect, calc_keypad_centers, cell_center
from scripts.full_playthrough.play_flow import build_fill_plan
from scripts.full_playthrough.reporter import Reporter, summarize_levels
from scripts.full_playthrough.solution_store import SolutionStore

DEVICE = "2FD0221410003338"  # may be overridden via --device
SCREEN_W, SCREEN_H = 1152, 2376
DENSITY = 3.0


def detect_device_params():
    """Auto-detect screen size and density from connected device."""
    global SCREEN_W, SCREEN_H, DENSITY
    # Screen size: prefer Override, fall back to Physical
    try:
        out = subprocess.run(["adb", "-s", DEVICE, "shell", "wm", "size"],
                             capture_output=True, text=True).stdout
        override_w, override_h = None, None
        physical_w, physical_h = None, None
        for line in out.splitlines():
            if "Override size:" in line:
                size = line.split(":", 1)[1].strip()
                if 'x' in size:
                    override_w, override_h = map(int, size.split("x", 1))
            elif "Physical size:" in line:
                size = line.split(":", 1)[1].strip()
                if 'x' in size:
                    physical_w, physical_h = map(int, size.split("x", 1))
        if override_w:
            SCREEN_W, SCREEN_H = override_w, override_h
        elif physical_w:
            SCREEN_W, SCREEN_H = physical_w, physical_h
    except Exception:
        pass

    # Density: prefer override, then ro.sf.lcd_density, then physical
    try:
        out = subprocess.run(["adb", "-s", DEVICE, "shell", "wm", "density"],
                             capture_output=True, text=True).stdout
        for line in out.splitlines():
            if "Override density:" in line:
                DENSITY = int(line.split(":", 1)[1].strip()) / 160.0
                break
            elif "Physical density:" in line:
                DENSITY = int(line.split(":", 1)[1].strip()) / 160.0
                break
        # Try ro.sf.lcd_density (actual rendering density)
        lcd = subprocess.run(["adb", "-s", DEVICE, "shell", "getprop", "ro.sf.lcd_density"],
                             capture_output=True, text=True).stdout.strip()
        if lcd and lcd.isdigit():
            DENSITY = int(lcd) / 160.0
    except Exception:
        pass


def tap(x, y):
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "tap", str(x), str(y)],
                   capture_output=True)


def back():
    subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "KEYCODE_BACK"],
                   capture_output=True)


def activity():
    r = subprocess.run(["adb", "-s", DEVICE, "shell", "dumpsys", "window"],
                       capture_output=True, text=True)
    for line in r.stdout.splitlines():
        if "mCurrentFocus" in line:
            return line.strip()
    return ""


def ss(path):
    subprocess.run(["adb", "-s", DEVICE, "exec-out", "screencap", "-p"],
                   stdout=open(path, "wb"))


class Runner:
    def __init__(self, apk_path, start_level=1, end_level=40,
                 skip_install=False, skip_guide=False):
        self.apk_path = apk_path
        self.start_level = start_level
        self.end_level = end_level
        self.skip_install = skip_install
        self.skip_guide = skip_guide
        self.pkg = "com.bird.starryskysudoku"
        self.detector = ScreenDetector(self.pkg, SCREEN_W, SCREEN_H, DENSITY)
        self.br = calc_board_rect(SCREEN_W, SCREEN_H, DENSITY)
        self.kp = calc_keypad_centers(SCREEN_W, SCREEN_H, self.br, DENSITY)
        self.data = json.loads((DATA_DIR / "level_solutions.json").read_text())
        self.reporter = Reporter(ROOT_DIR / "artifacts" / "full-playthrough")
        self.run_dir = None
        self.level_results = []
        self.recoveries = 0
        # Image-based calibration cache
        self._cell_centers_cache = None
        self._keypad_cache = None

    def log(self, msg):
        if self.run_dir:
            self.reporter.append_log("runner.log", msg)
        print(f"  {msg}")

    # ------------------------------------------------------------------
    def bootstrap(self):
        print("=== Bootstrap ===")
        self.run_dir = self.reporter.start_run()
        self.log(f"run_dir={self.run_dir}")

        if not self.skip_install:
            subprocess.run(["adb", "-s", DEVICE, "uninstall", self.pkg],
                           capture_output=True)
            time.sleep(0.5)
            subprocess.run(["adb", "-s", DEVICE, "install", "-r", self.apk_path],
                           capture_output=True)
            time.sleep(1.0)
            # Pre-grant permissions to avoid popup dialogs
            for perm in ["android.permission.POST_NOTIFICATIONS"]:
                subprocess.run(["adb", "-s", DEVICE, "shell", "pm", "grant",
                                self.pkg, perm], capture_output=True)

        subprocess.run(["adb", "-s", DEVICE, "shell", "monkey", "-p", self.pkg,
                        "-c", "android.intent.category.LAUNCHER", "1"],
                       capture_output=True)
        time.sleep(2.0)
        self.log(f"Launched: {activity()}")

    # ------------------------------------------------------------------
    def run_guide(self):
        if self.skip_guide:
            self.log("Skipping guide")
            return

        print("=== Guide ===")
        for i in range(12):
            act = activity()
            if "MapActivity" in act:
                self.log(f"MAP reached at step {i}")
                return True
            if "PlayActivity" in act:
                self.log(f"Already in PLAY at step {i}")
                return True
            self.log(f"Guide step {i+1}: {act}")
            tap(SCREEN_W // 2, SCREEN_H // 2)
            time.sleep(0.8)

        self.log("WARNING: guide may not have completed")
        return True

    # ------------------------------------------------------------------
    def enter_level(self, level_id):
        """Enter a level. Level 1 uses star tap, levels 2+ use dialog."""
        if level_id == 1:
            return self._enter_level_1()
        else:
            return self._enter_next_level(level_id)

    def _enter_level_1(self):
        """Tap level 1 star on map (NO SCROLLING!)."""
        self.log("Entering level 1 via star...")
        time.sleep(1.0)

        # Detect stars
        ss_path = self.run_dir / "screenshots" / "map_level1.png"
        ss(ss_path)
        cols = self.detector.find_map_star_columns(ss_path)
        col0 = cols.get(0, [])
        if col0:
            target = max(col0, key=lambda p: p[1])
        else:
            target = (549, 1760)  # known position from UI hierarchy

        self.log(f"Tap star at {target}")
        tap(target[0], target[1])
        time.sleep(1.5)

        self._handle_entry_dialog()
        self._handle_permission()
        return self._verify_entered_play(level_id=1)

    def _enter_next_level(self, level_id):
        """After completing previous level, next-level dialog auto-appears."""
        self.log(f"Entering level {level_id} via dialog...")

        for attempt in range(8):
            act = activity()
            if "PlayActivity" in act:
                return True
            if "MapActivity" in act:
                # Check for dialog
                ss_path = self.run_dir / "screenshots" / f"pre_level{level_id}.png"
                ss(ss_path)
                btn = self.detector.find_dialog_button(ss_path)
                if btn:
                    self.log(f"Dialog button at {btn}")
                    tap(btn[0], btn[1])
                    time.sleep(0.8)
                else:
                    self.log("No dialog visible, tapping center")
                    tap(SCREEN_W // 2, int(SCREEN_H * 0.54))
                    time.sleep(0.8)
            elif "permission" in act.lower():
                self._handle_permission()
            else:
                self.log(f"Unexpected state: {act}")
                tap(SCREEN_W // 2, int(SCREEN_H * 0.54))
                time.sleep(0.8)

        return self._verify_entered_play(level_id)

    def _handle_entry_dialog(self):
        """Handle the level-entry dialog after tapping a star."""
        ss_path = self.run_dir / "screenshots" / "entry_dialog.png"
        ss(ss_path)
        btn = self.detector.find_dialog_button(ss_path)
        if btn:
            self.log(f"Entry dialog button at {btn}")
            tap(btn[0], btn[1])
        else:
            self.log("No entry dialog, tapping center")
            tap(SCREEN_W // 2, int(SCREEN_H * 0.54))
        time.sleep(0.8)

    def _handle_permission(self):
        """Handle Android permission dialog."""
        for _ in range(3):
            act = activity()
            if "permission" not in act.lower():
                return
            self.log("Handling permission dialog")
            # "允许" is typically bottom-right
            tap(int(SCREEN_W * 0.72), int(SCREEN_H * 0.93))
            time.sleep(0.8)

    def _verify_entered_play(self, level_id):
        for _ in range(3):
            act = activity()
            if "PlayActivity" in act:
                self.log(f"Level {level_id} entered!")
                return True
            if "permission" in act.lower():
                self._handle_permission()
                continue
            time.sleep(0.5)
        self.log(f"FAILED to enter level {level_id}: {activity()}")
        return False

    # ------------------------------------------------------------------
    def _calibrate(self, screenshot_path, force=False):
        """Detect board cell centers and keypad from screenshot.
        Tries image detection first. Falls back to layout-based calculation
        (with auto-detected screen params) when detection fails.
        Results are cached across levels."""
        if self._cell_centers_cache is not None and self._keypad_cache is not None and not force:
            return True

        self.log("Calibrating board and keypad...")

        layout_centers = [
            [cell_center(self.br, r, c) for c in range(9)] for r in range(9)
        ]
        layout_kp = dict(self.kp)

        # Try full image-based detection (cells + keypad)
        detected_centers = self.detector.locate_cell_centers(screenshot_path, retries=1)
        if detected_centers is not None:
            # Grid detection worked — this device supports image-based detection
            self._cell_centers_cache = detected_centers
            self.log(f"Image calibration OK: cell(0,0)={detected_centers[0][0]}")

            # Only try image keypad if board detection also worked
            br_raw = self.detector.locate_board(screenshot_path, retries=0)
            detected_kp = self.detector.locate_keypad_buttons(screenshot_path, br_raw, retries=1)
            if detected_kp is not None and len(detected_kp) >= 5:
                self._keypad_cache = detected_kp
                self.log(f"Keypad image OK: {len(detected_kp)} keys")
                return True

        # Fallback: layout-based for everything
        self._cell_centers_cache = layout_centers
        self._keypad_cache = layout_kp
        self.log(f"Using layout calibration: cell(0,0)={layout_centers[0][0]}, key1={layout_kp.get(1)}")
        return True

    # ------------------------------------------------------------------
    def solve_level(self, level_id):
        """Fill all empty cells. Returns True if level appears solved."""
        level = self.data["levels"][str(level_id)]
        steps = build_fill_plan(level["puzzle"], level["solution"])
        self.log(f"Level {level_id}: {len(steps)} cells to fill")

        time.sleep(0.5)
        ss_path = self.run_dir / "screenshots" / f"board_lvl{level_id}.png"
        ss(ss_path)

        # Calibrate (first time) or use cache
        self._calibrate(ss_path, force=(level_id == 1))

        for i, (row, col, digit) in enumerate(steps):
            cx, cy = self._cell_centers_cache[row][col]
            kx, ky = self._keypad_cache[digit]
            tap(cx, cy)
            time.sleep(0.10)
            tap(kx, ky)
            time.sleep(0.08)

        return True

    # ------------------------------------------------------------------
    def verify_completion(self, level_id, timeout=15.0):
        """Wait for level completion. Returns True if completed successfully."""
        self.log(f"Waiting for level {level_id} completion...")
        deadline = time.time() + timeout
        dismiss_count = 0
        max_dismiss = 5

        while time.time() < deadline:
            act = activity()

            # Success: returned to map
            if "MapActivity" in act:
                self.log(f"Level {level_id} COMPLETED!")
                return True

            # Win dialog visible - dismiss it (with max retries)
            if "PlayActivity" in act:
                if dismiss_count >= max_dismiss:
                    self.log("Too many dismiss attempts, treating as timeout")
                    break

                ss_check = self.run_dir / "screenshots" / f"win_check_{level_id}.png"
                ss(ss_check)
                import cv2
                gray = cv2.cvtColor(cv2.imread(str(ss_check)), cv2.COLOR_BGR2GRAY)
                h, w = gray.shape
                # Check center region proportionally
                cy1, cy2 = int(h * 0.42), int(h * 0.59)
                cx1, cx2 = int(w * 0.26), int(w * 0.74)
                center_bright = gray[cy1:cy2, cx1:cx2].mean()

                if center_bright > 80:
                    dismiss_count += 1
                    self.log(f"Win dialog detected, dismissing ({dismiss_count}/{max_dismiss})...")
                    tap(w // 2, int(h * 0.56))
                    time.sleep(0.8)
                else:
                    time.sleep(0.5)

            elif "permission" in act.lower():
                self._handle_permission()

            else:
                time.sleep(0.5)

        # Timeout
        self.log(f"Level {level_id} completion TIMEOUT")
        return False

    # ------------------------------------------------------------------
    def run(self):
        print(f"\n{'='*60}")
        print(f"FULL PLAYTHROUGH: levels {self.start_level}-{self.end_level}")
        print(f"{'='*60}")

        # Phase 1: Bootstrap
        self.bootstrap()

        # Phase 2: Guide
        self.run_guide()

        # Phase 3: Level loop
        for level_id in range(self.start_level, self.end_level + 1):
            print(f"\n--- Level {level_id} ---")

            # Enter the level
            if not self.enter_level(level_id):
                self.level_results.append({
                    "level_id": level_id, "status": "failed",
                    "reason": "could_not_enter"
                })
                self.log(f"STOPPING: failed to enter level {level_id}")
                break

            # Solve
            self.solve_level(level_id)

            # Verify
            if not self.verify_completion(level_id):
                self.level_results.append({
                    "level_id": level_id, "status": "failed",
                    "reason": "completion_timeout"
                })
                self.log(f"STOPPING: level {level_id} did not complete")
                break

            self.level_results.append({
                "level_id": level_id, "status": "success",
                "reason": "completed"
            })

            # Small pause between levels
            time.sleep(0.5)

        # Phase 4: Summary
        print(f"\n{'='*60}")
        print("RESULTS")
        print(f"{'='*60}")
        completed = [r for r in self.level_results if r["status"] == "success"]
        failed = [r for r in self.level_results if r["status"] != "success"]
        print(f"Completed: {len(completed)}/{len(self.level_results)}")
        print(f"Failed: {[r['level_id'] for r in failed]}")

        summary = {
            "device": DEVICE,
            "start_level": self.start_level,
            "end_level": self.end_level,
            "completed": len(completed),
            "failed": [r["level_id"] for r in failed],
            "started_at": datetime.now().astimezone().isoformat(),
        }
        self.reporter.write_summary(summary)
        self.log(f"Summary written to {self.run_dir}/summary.json")

        return len(failed) == 0


def main():
    global DEVICE, SCREEN_W, SCREEN_H, DENSITY
    parser = argparse.ArgumentParser(description="Full StarrySkySudoku playthrough")
    parser.add_argument("--device", default=DEVICE)
    parser.add_argument("--apk", default=str(ROOT_DIR / "app/build/outputs/apk/debug/app-debug.apk"))
    parser.add_argument("--start-level", type=int, default=1)
    parser.add_argument("--end-level", type=int, default=40)
    parser.add_argument("--skip-install", action="store_true")
    parser.add_argument("--skip-guide", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    DEVICE = args.device
    detect_device_params()
    print(f"Device: {DEVICE}, Screen: {SCREEN_W}x{SCREEN_H}, Density: {DENSITY:.1f}")

    if args.dry_run:
        store = SolutionStore(DATA_DIR / "level_solutions.json")
        print(f"Dry run: {len(store.level_ids())} levels available")
        return

    runner = Runner(
        apk_path=args.apk,
        start_level=args.start_level,
        end_level=args.end_level,
        skip_install=args.skip_install,
        skip_guide=args.skip_guide,
    )
    success = runner.run()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()

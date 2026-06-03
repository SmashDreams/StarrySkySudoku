import re
import subprocess
from pathlib import Path
from typing import List, Optional, Tuple

FOCUS_RE = re.compile(r"([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)")


def parse_focus_line(line: str) -> Tuple[Optional[str], Optional[str]]:
    match = FOCUS_RE.search(line)
    if not match:
        return None, None
    return match.group(1), match.group(2)


class DeviceClient:
    def __init__(self, device_serial: Optional[str]):
        self.device_serial = device_serial

    def adb_prefix(self) -> List[str]:
        prefix = ["adb"]
        if self.device_serial:
            prefix.extend(["-s", self.device_serial])
        return prefix

    def run(self, *args: str, check: bool = True, text: bool = True):
        return subprocess.run(
            self.adb_prefix() + list(args),
            check=check,
            text=text,
            capture_output=True,
        )

    def shell(self, *args: str, check: bool = True) -> str:
        result = self.run("shell", *args, check=check)
        return result.stdout.replace("\r", "")

    def uninstall(self, package_name: str) -> None:
        self.run("uninstall", package_name, check=False)

    def install_apk(self, apk_path: Path) -> None:
        self.run("install", "-r", str(apk_path))

    def launch(self, package_name: str) -> None:
        self.shell("monkey", "-p", package_name, "-c", "android.intent.category.LAUNCHER", "1")

    def tap(self, x: int, y: int) -> None:
        self.shell("input", "tap", str(x), str(y))

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration_ms: int = 300) -> None:
        self.shell("input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration_ms))

    def back(self) -> None:
        self.shell("input", "keyevent", "KEYCODE_BACK")

    def screenshot_to(self, path: Path) -> Path:
        raw = self.run("exec-out", "screencap", "-p", check=True, text=False)
        path.write_bytes(raw.stdout)
        return path

    def screen_size(self) -> Tuple[int, int]:
        output = self.shell("wm", "size")
        for line in output.splitlines():
            if "Override size:" in line or "Physical size:" in line:
                size_text = line.split(":", 1)[1].strip()
                width_text, height_text = size_text.split("x", 1)
                return int(width_text), int(height_text)
        return 1080, 1920

    def tap_screen_center(self) -> None:
        width, height = self.screen_size()
        self.tap(width // 2, height // 2)

    def current_focus_line(self) -> str:
        for line in self.shell("dumpsys", "window").splitlines():
            if "mCurrentFocus=" in line:
                return line.strip()
        return ""

    def current_package_and_activity(self) -> Tuple[Optional[str], Optional[str]]:
        return parse_focus_line(self.current_focus_line())

import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import cv2
import numpy as np

from .geometry import BoardRect, calc_board_rect, calc_keypad_centers
from .models import AppState, DetectionResult
from .vision import (
    detect_board_by_gradient,
    detect_board_rect,
    detect_keypad_buttons,
    find_bright_contours,
    find_dialog_region,
    find_keypad_region,
    load_bgr,
    to_gray,
)


class ScreenDetector:
    def __init__(self, package_name: str, screen_width: int = 1152,
                 screen_height: int = 2376, density: float = 3.0):
        self.package_name = package_name
        self.screen_width = screen_width
        self.screen_height = screen_height
        self.density = density

    # ------------------------------------------------------------------
    # 顶层页面分类
    # ------------------------------------------------------------------
    def classify(self, screenshot_path: Path, package_name: Optional[str] = None,
                 activity_name: Optional[str] = None) -> DetectionResult:
        if package_name and package_name != self.package_name:
            return DetectionResult(
                state=AppState.OUT_OF_APP,
                package_name=package_name,
                activity_name=activity_name,
                confidence=1.0,
            )

        activity = (activity_name or "").lower()
        if "guide" in activity:
            return DetectionResult(state=AppState.GUIDE, package_name=package_name,
                                   activity_name=activity_name, confidence=0.85)
        if "map" in activity:
            return DetectionResult(state=AppState.MAP, package_name=package_name,
                                   activity_name=activity_name, confidence=0.9)
        if "play" in activity:
            return DetectionResult(state=AppState.PLAY, package_name=package_name,
                                   activity_name=activity_name, confidence=0.9)
        if "entry" in activity or "splash" in activity:
            return DetectionResult(state=AppState.SPLASH, package_name=package_name,
                                   activity_name=activity_name, confidence=0.7)

        try:
            return self._classify_from_screenshot(screenshot_path, package_name,
                                                  activity_name)
        except Exception:
            return DetectionResult(state=AppState.UNKNOWN, package_name=package_name,
                                   activity_name=activity_name, confidence=0.05)

    def _classify_from_screenshot(self, path: Path, package_name: Optional[str],
                                  activity_name: Optional[str]) -> DetectionResult:
        img = load_bgr(path)
        gray = to_gray(img)

        # Check for board presence
        if self._verify_board_region(gray):
            dialog = find_dialog_region(gray)
            if dialog is not None and dialog[2] > 200:
                return DetectionResult(state=AppState.PLAY_DIALOG,
                                       package_name=package_name,
                                       activity_name=activity_name, confidence=0.6)
            return DetectionResult(state=AppState.PLAY, package_name=package_name,
                                   activity_name=activity_name, confidence=0.7)

        # Check for map stars
        brights = find_bright_contours(gray, threshold=100, min_area=400, max_area=50000)
        star_like = [(x, y, w, h, a) for (x, y, w, h, a) in brights
                     if 0.5 < w / max(h, 1) < 2.0 and 500 < a < 50000]
        if len(star_like) >= 3:
            return DetectionResult(state=AppState.MAP, package_name=package_name,
                                   activity_name=activity_name, confidence=0.5)

        return DetectionResult(state=AppState.UNKNOWN, package_name=package_name,
                               activity_name=activity_name, confidence=0.05)

    def _verify_board_region(self, gray) -> bool:
        """Check if a Sudoku board is visible by verifying the layout-calculated
        board area has the expected brightness pattern."""
        try:
            br = calc_board_rect(self.screen_width, self.screen_height, self.density)
            # Sample center of board - should be bright (cell bitmap backgrounds)
            cx, cy = br.left + br.width // 2, br.top + br.height // 2
            if cx < 0 or cy < 0 or cx >= gray.shape[1] or cy >= gray.shape[0]:
                return False
            center_val = gray[cy, cx]
            # Check corners - should also be in a reasonable range for a board
            corners = [
                gray[br.top + 15, br.left + 15],
                gray[br.top + 15, br.left + br.width - 15],
                gray[br.top + br.height - 15, br.left + 15],
            ]
            # Board region typically has bright cells on dark-ish bg
            return center_val > 80 and all(10 < c < 255 for c in corners)
        except Exception:
            return False

    # ------------------------------------------------------------------
    # 棋盘定位
    # ------------------------------------------------------------------
    def locate_board(self, screenshot_path: Path,
                     retries: int = 2) -> Optional[Tuple[int, int, int, int]]:
        """Return board rect (x, y, w, h) in screen pixels.

        Tries gradient square detection first (most reliable), then
        grid-line clustering, then layout-based calculation.
        """
        # 1. Gradient detection (most reliable across devices)
        try:
            img = load_bgr(screenshot_path)
            gray = to_gray(img)
            result = detect_board_by_gradient(gray)
            if result is not None:
                return result
        except Exception:
            pass

        # 2. Grid-line detection
        for attempt in range(retries + 1):
            try:
                img = load_bgr(screenshot_path)
                gray = to_gray(img)
                grid_lines = self._detect_grid_lines(gray)
                if grid_lines is not None:
                    h_lines, v_lines = grid_lines
                    if len(h_lines) >= 8 and len(v_lines) >= 8:
                        h10 = self._best_line_cluster(h_lines)
                        v10 = self._best_line_cluster(v_lines)
                        if h10 is not None and v10 is not None:
                            left = v10[0]
                            top = h10[0]
                            right = v10[-1]
                            bottom = h10[-1]
                            return (left, top, right - left, bottom - top)
            except Exception:
                pass
            if attempt < retries:
                time.sleep(0.3)

        # 3. Layout calculation fallback
        br = calc_board_rect(self.screen_width, self.screen_height, self.density)
        return (br.left, br.top, br.width, br.height)

    def _detect_grid_lines(self, gray):
        """Detect horizontal and vertical grid lines.

        Uses morphological operations to isolate long lines from cell
        content (numbers), making detection robust against filled cells.
        """
        import numpy as np

        h_img, w_img = gray.shape
        # Kernel sized as ~30% of estimated cell width, min 25px
        kernel_len = max(25, int(min(w_img, h_img) * 0.03))

        blurred = cv2.GaussianBlur(gray, (3, 3), 0)
        edges = cv2.Canny(blurred, 30, 100)

        # Horizontal kernel: keep only long horizontal edges
        h_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (kernel_len, 1))
        h_edges = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, h_kernel)
        h_profile = np.sum(h_edges > 0, axis=1)

        # Vertical kernel: keep only long vertical edges
        v_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (1, kernel_len))
        v_edges = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, v_kernel)
        v_profile = np.sum(v_edges > 0, axis=0)

        h_lines = self._find_peaks(h_profile, min_dist=30, threshold=100)
        v_lines = self._find_peaks(v_profile, min_dist=30, threshold=100)
        return h_lines, v_lines

    @staticmethod
    def _find_peaks(profile, min_dist=40, threshold=200):
        """Find local maxima in a 1D profile."""
        peaks = []
        for i in range(1, len(profile) - 1):
            if profile[i] < threshold:
                continue
            if profile[i] >= profile[i-1] and profile[i] > profile[i+1]:
                # Local max - check it's the best in +/-min_dist
                start = max(0, i - min_dist // 2)
                end = min(len(profile), i + min_dist // 2 + 1)
                if profile[i] == profile[start:end].max():
                    if not peaks or i - peaks[-1] > min_dist * 0.8:
                        peaks.append(i)
                    elif profile[i] > profile[peaks[-1]]:
                        peaks[-1] = i
        return peaks

    @staticmethod
    def _best_line_cluster(lines):
        """Find the best cluster of ~10 lines with consistent spacing.

        Uses RANSAC-like voting: find the most common spacing, then
        collect all lines that follow that spacing pattern.
        Prefers clusters whose spacing matches expected cell size.
        """
        if len(lines) < 6:
            return None

        # Expected cell spacing: board takes ~85% of screen width
        # 9 cells → spacing ≈ screen_width * 0.85 / 9 ≈ screen_width * 0.094
        spacings = [lines[i+1] - lines[i] for i in range(len(lines) - 1)]
        if not spacings:
            return None

        # Vote for the best spacing range
        spacing_votes = {}
        for s in spacings:
            if 30 < s < 300:
                key = (s // 10) * 10
                spacing_votes[key] = spacing_votes.get(key, 0) + 1

        if not spacing_votes:
            return None

        best_range = max(spacing_votes, key=spacing_votes.get)
        target_spacing = best_range + 5

        # Collect lines with this spacing pattern
        best_cluster = None
        best_score = 0
        for start_idx in range(len(lines)):
            cluster = [lines[start_idx]]
            current = lines[start_idx]
            for j in range(start_idx + 1, len(lines)):
                gap = lines[j] - current
                tolerance = max(20, target_spacing * 0.3)
                if abs(gap - target_spacing) < tolerance:
                    cluster.append(lines[j])
                    current = lines[j]
                elif gap > target_spacing + tolerance:
                    break
            # Score = line count * spacing consistency bonus (prefer ~100px spacing)
            spacing_bonus = 1.0 if 60 < target_spacing < 200 else 0.5
            score = len(cluster) * spacing_bonus
            if len(cluster) >= 5 and score > best_score:
                best_score = score
                best_cluster = cluster

        if best_cluster and len(best_cluster) >= 6:
            return best_cluster[:10]
        return None

    # ------------------------------------------------------------------
    # 单元格中心定位
    # ------------------------------------------------------------------
    def locate_cell_centers(self, screenshot_path: Path,
                            retries: int = 2) -> Optional[List[List[Tuple[int, int]]]]:
        """Return a 9x9 grid of (cx, cy) cell centers in screen pixels.

        Tries grid-line detection first, then falls back to gradient-detected
        board rect with BroadView geometry."""
        # First attempt: grid-line detection
        for attempt in range(retries + 1):
            try:
                img = load_bgr(screenshot_path)
                gray = to_gray(img)
                grid_lines = self._detect_grid_lines(gray)
                if grid_lines is None:
                    continue
                h_lines, v_lines = grid_lines
                h10 = self._best_line_cluster(h_lines)
                v10 = self._best_line_cluster(v_lines)
                if h10 is None or v10 is None:
                    continue
                if len(h10) < 9 or len(v10) < 9:
                    continue
                h10 = h10[:10]
                v10 = v10[:10]
                if len(h10) < 10 or len(v10) < 10:
                    continue

                centers = []
                for row in range(9):
                    row_centers = []
                    cy = (h10[row] + h10[row + 1]) // 2
                    for col in range(9):
                        cx = (v10[col] + v10[col + 1]) // 2
                        row_centers.append((cx, cy))
                    centers.append(row_centers)

                # Validate: cell(0,0) to cell(8,8) span should be reasonable
                dx = centers[8][8][0] - centers[0][0][0]
                dy = centers[8][8][1] - centers[0][0][1]
                span = (dx * dx + dy * dy) ** 0.5
                if span > gray.shape[1] * 0.5:  # at least 50% of screen width
                    return centers
            except Exception:
                pass
            if attempt < retries:
                time.sleep(0.3)

        # Second attempt: gradient board rect + BroadView geometry
        try:
            img = load_bgr(screenshot_path)
            gray = to_gray(img)
            board = detect_board_by_gradient(gray)
            if board is not None:
                bx, by, bw, bh = board
                cell_size = (bw - 54) / 9.0
                inset = 28
                centers = []
                for row in range(9):
                    row_centers = []
                    cy = int(by + inset + (row + 0.5) * cell_size)
                    for col in range(9):
                        cx = int(bx + inset + (col + 0.5) * cell_size)
                        row_centers.append((cx, cy))
                    centers.append(row_centers)
                return centers
        except Exception:
            pass

        return None

    # ------------------------------------------------------------------
    # 数字键定位 (布局计算为主，图像识别为备)
    # ------------------------------------------------------------------
    def locate_keypad(self, screenshot_path: Path,
                      board_rect: Optional[Tuple[int, int, int, int]] = None,
                      retries: int = 2) -> Dict[int, Tuple[int, int]]:
        # Primary: layout-based calculation
        if board_rect is None:
            board = calc_board_rect(self.screen_width, self.screen_height, self.density)
        else:
            board = BoardRect(left=board_rect[0], top=board_rect[1],
                              width=board_rect[2], height=board_rect[3])

        layout_keypad = calc_keypad_centers(self.screen_width, self.screen_height,
                                                board, self.density)

        # Verify with screenshot
        try:
            gray = to_gray(load_bgr(screenshot_path))
            # Check a few key centers
            sample_keys = [1, 5, 9]
            bright_count = 0
            for d in sample_keys:
                kx, ky = layout_keypad.get(d, (0, 0))
                if 0 <= kx < gray.shape[1] and 0 <= ky < gray.shape[0]:
                    region = gray[max(0, ky-15):ky+15, max(0, kx-15):kx+15]
                    if region.size > 0 and region.mean() > 60:
                        bright_count += 1
            if bright_count >= 2:
                return layout_keypad
        except Exception:
            pass

        # Fallback: image-based detection
        for _ in range(retries + 1):
            try:
                img = load_bgr(screenshot_path)
                gray = to_gray(img)
                raw_board = board_rect or (board.left, board.top, board.width, board.height)
                region = find_keypad_region(gray, raw_board)
                if region is not None:
                    kx, ky, kw, kh = region
                    mapping = self._derive_keypad_centers(kx, ky, kw, kh)
                    if len(mapping) == 9:
                        return mapping
            except Exception:
                pass
            if retries > 0:
                time.sleep(0.3)

        return layout_keypad

    def _derive_keypad_centers(self, kx: int, ky: int, kw: int,
                               kh: int) -> Dict[int, Tuple[int, int]]:
        mapping = {}
        cell_w = kw / 9.0
        for i in range(9):
            cx = int(kx + (i + 0.5) * cell_w)
            cy = int(ky + kh / 2)
            mapping[i + 1] = (cx, cy)
        return mapping

    # ------------------------------------------------------------------
    # 数字键图像检测
    # ------------------------------------------------------------------
    def locate_keypad_buttons(self, screenshot_path: Path,
                              board_rect: Optional[Tuple[int, int, int, int]] = None,
                              retries: int = 1) -> Optional[Dict[int, Tuple[int, int]]]:
        """Detect keypad button centers from screenshot using image analysis."""
        for _ in range(retries + 1):
            try:
                img = load_bgr(screenshot_path)
                gray = to_gray(img)
                result = detect_keypad_buttons(gray, board_rect)
                if result is not None:
                    mapping, _ = result
                    if len(mapping) >= 5:
                        return mapping
            except Exception:
                pass
            if retries > 0:
                time.sleep(0.3)
        return None

    # ------------------------------------------------------------------
    # 对话框
    # ------------------------------------------------------------------
    def find_dialog_button(self, screenshot_path: Path,
                           button_label: str = "start") -> Optional[Tuple[int, int]]:
        """Find the primary action button in a dialog.

        Dialog layout: 300dp-wide white body centered on dimmed overlay.
        Start button: match_parent x 92dp at the bottom of the body.
        """
        try:
            img = load_bgr(screenshot_path)
            gray = to_gray(img)
            dialog = find_dialog_region(gray)
            if dialog is None:
                return None
            dx, dy, dw, dh = dialog
            # Button is 92dp at the bottom of the dialog body
            button_h_dp = 92
            button_h_px = int(button_h_dp * self.density)
            # Button center X = dialog center X
            button_x = dx + dw // 2
            # Button center Y = bottom of dialog - half button height - bottom padding (~16dp)
            bottom_pad_px = int(16 * self.density)
            button_y = dy + dh - bottom_pad_px - button_h_px // 2
            return (button_x, button_y)
        except Exception:
            return None

    # ------------------------------------------------------------------
    # 地图关卡星点
    # ------------------------------------------------------------------
    def find_map_stars(self, screenshot_path: Path,
                       min_area: float = 500,
                       max_area: float = 40000) -> List[Tuple[int, int, int, int]]:
        try:
            img = load_bgr(screenshot_path)
            gray = to_gray(img)
            brights = find_bright_contours(gray, threshold=90,
                                           min_area=min_area, max_area=max_area)
            results = []
            for x, y, w, h, area in brights:
                if 0.5 < w / max(h, 1) < 2.5:
                    results.append((x + w // 2, y + h // 2, w, h))
            return results
        except Exception:
            return []

    def find_map_star_columns(self, screenshot_path: Path) -> Dict[int, List[Tuple[int, int]]]:
        stars = self.find_map_stars(screenshot_path)
        sw = self.screen_width
        expected_x = {
            0: int(0.47 * sw),
            1: int(0.10 * sw),
            2: int(0.50 * sw),
            3: int(0.93 * sw),
        }
        columns: Dict[int, List[Tuple[int, int]]] = {0: [], 1: [], 2: [], 3: []}
        for cx, cy, w, h in stars:
            best_col = min(range(4), key=lambda c: abs(cx - expected_x[c]))
            if abs(cx - expected_x[best_col]) < 200:
                columns[best_col].append((cx, cy))
        for col in columns:
            columns[col].sort(key=lambda p: p[1])
        return columns

"""Low-level image helpers: load, find contours, detect grids and bright spots."""
import cv2
import numpy as np
from pathlib import Path
from typing import Dict, List, Optional, Tuple


def load_bgr(path: Path) -> np.ndarray:
    """Read screenshot as BGR (OpenCV default), stripping alpha if present."""
    img = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if img is None:
        raise FileNotFoundError(f"cannot read image: {path}")
    if img.shape[2] == 4:
        img = img[:, :, :3]
    return img


def to_gray(bgr: np.ndarray) -> np.ndarray:
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)


def find_bright_contours(
    gray: np.ndarray,
    threshold: int = 100,
    min_area: float = 200,
    max_area: float = 200_000,
) -> List[Tuple[int, int, int, int, float]]:
    """Return (x, y, w, h, area) for bright regions sorted by area descending."""
    _, binary = cv2.threshold(gray, threshold, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    results = []
    for cnt in contours:
        area = cv2.contourArea(cnt)
        if min_area < area < max_area:
            x, y, w, h = cv2.boundingRect(cnt)
            results.append((x, y, w, h, area))
    results.sort(key=lambda r: r[4], reverse=True)
    return results


def find_dark_contours(
    gray: np.ndarray,
    threshold: int = 60,
    min_area: float = 200,
    max_area: float = 2_000_000,
) -> List[Tuple[int, int, int, int, float]]:
    """Return (x, y, w, h, area) for dark regions, sorted by area descending."""
    _, binary = cv2.threshold(gray, threshold, 255, cv2.THRESH_BINARY_INV)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    results = []
    for cnt in contours:
        area = cv2.contourArea(cnt)
        if min_area < area < max_area:
            x, y, w, h = cv2.boundingRect(cnt)
            results.append((x, y, w, h, area))
    results.sort(key=lambda r: r[4], reverse=True)
    return results


def find_largest_grid_candidate(
    gray: np.ndarray,
) -> Optional[Tuple[int, int, int, int]]:
    """Locate the most grid-like rectangular region (the Sudoku board).
    Uses Canny edge detection + probabilistic Hough lines to find regions
    with many horizontal and vertical lines.

    Returns (x, y, w, h) or None."""
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 30, 120)

    lines = cv2.HoughLinesP(edges, 1, np.pi / 180, threshold=60,
                            minLineLength=80, maxLineGap=15)
    if lines is None or len(lines) < 10:
        return None

    horizontal = []
    vertical = []
    for line in lines:
        x1, y1, x2, y2 = line[0]
        angle = abs(np.arctan2(y2 - y1, x2 - x1) * 180 / np.pi)
        if angle < 20 or angle > 160:
            horizontal.append(line[0])
        elif 70 < angle < 110:
            vertical.append(line[0])

    if len(horizontal) < 3 or len(vertical) < 3:
        return None

    h_ys = [(y1 + y2) // 2 for _, y1, _, y2 in horizontal]
    v_xs = [(x1 + x2) // 2 for x1, _, x2, _ in vertical]
    h_ys.sort()
    v_xs.sort()

    left = max(v_xs[0] - 20, 0)
    right = min(v_xs[-1] + 20, gray.shape[1] - 1)
    top = max(h_ys[0] - 20, 0)
    bottom = min(h_ys[-1] + 20, gray.shape[0] - 1)

    w = right - left
    h = bottom - top
    if w < 200 or h < 200 or abs(w - h) > max(w, h) * 0.5:
        return None

    return (left, top, w, h)


def find_grid_by_contour(
    gray: np.ndarray,
) -> Optional[Tuple[int, int, int, int]]:
    """Fallback: find the largest approximately-square dark contour."""
    _, binary = cv2.threshold(gray, 80, 255, cv2.THRESH_BINARY_INV)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    best = None
    best_score = 0
    for cnt in contours:
        area = cv2.contourArea(cnt)
        if area < 80000:
            continue
        x, y, w, h = cv2.boundingRect(cnt)
        aspect = w / max(h, 1)
        if 0.6 < aspect < 1.6:
            score = area * (1 - abs(1 - aspect))
            if score > best_score:
                best_score = score
                best = (x, y, w, h)
    return best


def detect_board_by_gradient(gray: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
    """Find the Sudoku board as a large bright square via morphological gradient.

    The board area is bright (cell bitmap backgrounds) against a dark
    starry-sky background. A morphological gradient with a large kernel
    highlights the board outline, then we find the largest approximately-
    square contour.

    Returns (x, y, w, h) or None.
    """
    import numpy as np

    blur = cv2.GaussianBlur(gray, (9, 9), 0)
    kernel_size = max(10, min(gray.shape) // 60)
    kernel = np.ones((kernel_size, kernel_size), np.uint8)
    gradient = cv2.morphologyEx(blur, cv2.MORPH_GRADIENT, kernel)

    _, binary = cv2.threshold(gradient, 12, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    best = None
    best_area = 0
    h, w = gray.shape
    for cnt in contours:
        area = cv2.contourArea(cnt)
        if area < 30000:
            continue
        x, y, bw, bh = cv2.boundingRect(cnt)
        aspect = bw / max(bh, 1)
        # Board is roughly square and NOT full-screen
        if 0.80 < aspect < 1.25 and bw < w * 0.92 and area > best_area:
            best_area = area
            best = (x, y, bw, bh)

    return best


def detect_board_rect(gray: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
    """Composite: gradient square → Hough grid → contour fallback."""
    result = detect_board_by_gradient(gray)
    if result is not None:
        return result
    result = find_largest_grid_candidate(gray)
    if result is not None:
        return result
    return find_grid_by_contour(gray)


def find_keypad_region(
    gray: np.ndarray,
    board_rect: Optional[Tuple[int, int, int, int]] = None,
) -> Optional[Tuple[int, int, int, int]]:
    """Find the 1-9 keypad below the board.

    Looks for 9 similarly-sized bright/dark circular regions below the board.
    Returns (x, y, w, h) bounding box of the keypad, or None."""
    bottom_boundary = 0
    if board_rect is not None:
        _, board_y, _, board_h = board_rect
        bottom_boundary = board_y + board_h

    roi = gray[bottom_boundary:, :]

    blurred = cv2.GaussianBlur(roi, (9, 9), 0)
    circles = cv2.HoughCircles(
        blurred, cv2.HOUGH_GRADIENT, dp=1.5, minDist=40,
        param1=50, param2=30, minRadius=25, maxRadius=90,
    )

    if circles is None or len(circles[0]) < 3:
        return None

    circles = circles[0]
    xs = [c[0] for c in circles]
    ys = [c[1] for c in circles]
    rs = [c[2] for c in circles]

    left = int(max(min(xs) - max(rs) - 10, 0))
    right = int(min(max(xs) + max(rs) + 10, gray.shape[1] - 1))
    top = int(bottom_boundary + min(ys) - max(rs) - 10)
    bot = int(bottom_boundary + max(ys) + max(rs) + 10)

    return (left, top, right - left, bot - top)


def detect_keypad_buttons(
    gray: np.ndarray,
    board_rect: Optional[Tuple[int, int, int, int]] = None,
) -> Optional[Tuple[Dict[int, Tuple[int, int]], Tuple[int, int, int, int]]]:
    """Detect number keypad buttons below the board.

    Looks for rectangular button regions below the board area, identifies
    9 number buttons arranged in 2 rows (row1: 1-5, row2: 6-9).

    Returns (digit->center mapping, keypad_bbox) or None.
    """
    h, w = gray.shape

    # Require board_rect — without it we'd pick up board cells
    if board_rect is None:
        return None

    _, board_y, _, board_h = board_rect
    bottom_boundary = board_y + board_h
    # Add a small margin so we don't include the board's bottom edge
    bottom_boundary += int(board_h * 0.03)

    if bottom_boundary >= h - 50:
        return None

    roi = gray[bottom_boundary:, :]
    roi_h, roi_w = roi.shape

    # Strategy: find bright rectangular regions (button backgrounds)
    blur = cv2.GaussianBlur(roi, (5, 5), 0)

    # Try multiple thresholds to find buttons
    best_buttons = None
    best_count = 0

    for thresh in (80, 100, 120):
        _, binary = cv2.threshold(blur, thresh, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        buttons = []
        for cnt in contours:
            area = cv2.contourArea(cnt)
            # Button area: ~55*60dp² at various densities
            if area < 300 or area > 80000:
                continue
            x, y, bw, bh = cv2.boundingRect(cnt)
            aspect = bw / max(bh, 1)
            # Buttons are roughly square-ish (0.5 to 1.5 aspect)
            if 0.4 < aspect < 2.0 and 20 < bw < 500 and 20 < bh < 300:
                buttons.append((x, y, bw, bh, area))

        if len(buttons) > best_count:
            best_count = len(buttons)
            best_buttons = buttons

    if best_buttons is None or len(best_buttons) < 4:
        return None

    # Group buttons into rows by Y position
    centers = [(x + bw // 2, y + bh // 2, x, y, bw, bh) for x, y, bw, bh, _ in best_buttons]
    centers.sort(key=lambda c: c[1])  # sort by Y

    # Cluster Y positions into 2 rows
    ys = [c[1] for c in centers]
    if len(ys) >= 2 and (max(ys) - min(ys)) > 30:
        mid_y = (max(ys) + min(ys)) / 2
        row1 = [c for c in centers if c[1] < mid_y]
        row2 = [c for c in centers if c[1] >= mid_y]
    else:
        row1 = centers
        row2 = []

    # Sort each row by X
    row1.sort(key=lambda c: c[0])
    row2.sort(key=lambda c: c[0])

    # Map to digits: row1 = keys 1-5, row2 = keys 6-9
    # Take first 5 from row1, first 4 from row2
    mapping = {}
    for idx, (cx, cy, _, _, _, _) in enumerate(row1[:5]):
        mapping[idx + 1] = (int(cx), int(cy + bottom_boundary))

    for idx, (cx, cy, _, _, _, _) in enumerate(row2[:4]):
        mapping[idx + 6] = (int(cx), int(cy + bottom_boundary))

    if len(mapping) < 5:
        return None

    # Compute bounding box
    all_centers = row1[:5] + row2[:4]
    if not all_centers:
        return None
    kp_left = min(c[2] for c in all_centers)
    kp_top = min(c[3] for c in all_centers) + bottom_boundary
    kp_right = max(c[2] + c[4] for c in all_centers)
    kp_bot = max(c[3] + c[5] for c in all_centers) + bottom_boundary

    return mapping, (kp_left, kp_top, kp_right - kp_left, kp_bot - kp_top)


def find_dialog_region(
    gray: np.ndarray,
) -> Optional[Tuple[int, int, int, int]]:
    """Detect a centered dialog with white body on dimmed overlay.

    The dialog background is white (#FFFFFF) with 15dp rounded corners,
    centered on a semi-transparent black (#B3000000) overlay.
    Body is 300dp wide, wrap_content tall.
    """
    h, w = gray.shape

    blur = cv2.GaussianBlur(gray, (9, 9), 0)
    # Look for bright white regions (dialog body)
    _, binary = cv2.threshold(blur, 180, 255, cv2.THRESH_BINARY)

    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    best = None
    best_score = 0
    for cnt in contours:
        area = cv2.contourArea(cnt)
        if area < 15000 or area > w * h * 0.5:
            continue
        x, y, bw, bh = cv2.boundingRect(cnt)
        cx = x + bw / 2
        cy = y + bh / 2
        centrality = 1 - (abs(cx - w / 2) / (w / 2) + abs(cy - h / 2) / (h / 2)) / 2
        # Dialog body should be centered and have reasonable aspect ratio
        aspect = bw / max(bh, 1)
        if centrality > 0.3 and 0.5 < aspect < 3.0:
            score = area * centrality
            if score > best_score:
                best_score = score
                best = (x, y, bw, bh)
    return best

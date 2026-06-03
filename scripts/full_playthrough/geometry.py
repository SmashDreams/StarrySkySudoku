"""Board and screen geometry calculations based on app layout parameters."""
from dataclasses import dataclass
from typing import Dict, Tuple


@dataclass(frozen=True)
class BoardRect:
    left: int
    top: int
    width: int
    height: int

    @property
    def cell_width(self) -> float:
        return self.width / 9.0

    @property
    def cell_height(self) -> float:
        return self.height / 9.0


def cell_center(board: BoardRect, row: int, col: int) -> Tuple[int, int]:
    """Calculate cell center from board rect using BroadView geometry.

    BroadView uses: CELL_INSET=28px, CELL_SIZE_OFFSET=54px (in view pixels).
    cellSize = (viewWidth - CELL_SIZE_OFFSET) / 9
    cell (r,c) center = (board.left + CELL_INSET + (c+0.5)*cellSize,
                         board.top + CELL_INSET + (r+0.5)*cellSize)
    """
    cell_size = (board.width - 54) / 9.0
    inset = 28
    x = int(board.left + inset + (col + 0.5) * cell_size)
    y = int(board.top + inset + (row + 0.5) * cell_size)
    return x, y


# ------------------------------------------------------------------
# Layout-based calculations for the current device
# ------------------------------------------------------------------

def calc_board_rect(screen_width: int, screen_height: int,
                    density: float = 3.0) -> BoardRect:
    """Calculate board position from known layout parameters.

    Board is 370dp x 370dp, centered horizontally with vertical_bias=0.38
    in a full-screen ConstraintLayout. Values calibrated against measured
    grid-line positions on reference device (1152x2376, density 3.0).

    Measured BoardRect: (16, 550, 1110, 1110).
    """
    board_dp = 370
    board_px = int(board_dp * density)

    # Measured top at density=3.0 on 1152x2376 is 550.
    # Layout calculation gives 481. Effective bias is higher (~0.434).
    # Measured left: 16 (not perfectly centered at 21).
    top_ref = 550
    left_ref = 16
    sh_ref = 2376
    sw_ref = 1152
    top = int(top_ref * (screen_height / sh_ref))
    left = int(left_ref + (screen_width - sw_ref) / 2)

    return BoardRect(left=left, top=top, width=board_px, height=board_px)


def calc_keypad_centers(screen_width: int, screen_height: int,
                        board_rect: BoardRect,
                        density: float = 3.0) -> Dict[int, Tuple[int, int]]:
    """Calculate number keypad centers from layout parameters.

    Keypad is a wrap_content LinearLayout constrained below the board.
    Row 1: keys 1-5 + notes toggle. Row 2: keys 6-9 + undo button.
    Keys are 55x60dp, evenly spaced with layout_weight spacers.

    X positions derived from measured pixel data on reference device
    (1152x2376, density 3.0): keys at x = 105, 293, 481, 669, 857.
    """
    key_h = int(60 * density)

    # Keypad Y: board_bottom + bias * (screen_height - board_bottom)
    # Measured effective bias = 0.27 on reference device
    board_bottom = board_rect.top + board_rect.height
    avail_below = screen_height - board_bottom
    keypad_top = board_bottom + int(0.27 * avail_below)

    row1_cy = keypad_top + key_h // 2  # Row 1 center Y
    row2_cy = keypad_top + key_h + key_h // 2  # Row 2 center Y

    # X centers scaled proportionally from reference device (1152px wide)
    x_scale = screen_width / 1152.0
    key_x = [int(x * x_scale) for x in (105, 293, 481, 669, 857)]

    mapping = {}
    for digit in range(1, 10):
        if digit <= 5:
            mapping[digit] = (key_x[digit - 1], row1_cy)
        else:
            mapping[digit] = (key_x[digit - 6], row2_cy)
    return mapping

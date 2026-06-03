import json
import sqlite3
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List

_SCRIPT_DIR = Path(__file__).resolve().parent
_PROJECT_ROOT = _SCRIPT_DIR.parent.parent
DB_PATH = _PROJECT_ROOT / "app/src/main/assets/sudoku.db"
OUTPUT_PATH = _SCRIPT_DIR / "data" / "level_solutions.json"


def extract_levels(db_path: Path) -> Dict[int, List[int]]:
    conn = sqlite3.connect(db_path)
    try:
        rows = conn.execute(
            "SELECT pass_num, value FROM problem ORDER BY pass_num, rowid"
        ).fetchall()
    finally:
        conn.close()

    grouped: Dict[int, List[int]] = defaultdict(list)
    for pass_num, value in rows:
        grouped[int(pass_num)].append(int(value))

    levels = dict(grouped)
    for pass_num, values in levels.items():
        if len(values) != 81:
            raise ValueError(f"level {pass_num} has {len(values)} cells")
    return levels


def is_valid(board: List[int], row: int, col: int, num: int) -> bool:
    row_start = row * 9
    if num in board[row_start:row_start + 9]:
        return False

    for current_row in range(9):
        if board[current_row * 9 + col] == num:
            return False

    start_row = (row // 3) * 3
    start_col = (col // 3) * 3
    for box_row in range(start_row, start_row + 3):
        for box_col in range(start_col, start_col + 3):
            if board[box_row * 9 + box_col] == num:
                return False
    return True


def solve_board(puzzle: List[int]) -> List[int]:
    board = puzzle[:]

    def backtrack() -> bool:
        for idx, value in enumerate(board):
            if value == 0:
                row, col = divmod(idx, 9)
                for num in range(1, 10):
                    if is_valid(board, row, col, num):
                        board[idx] = num
                        if backtrack():
                            return True
                        board[idx] = 0
                return False
        return True

    if not backtrack():
        raise ValueError("unsolved puzzle encountered")
    return board


def validate_solution(puzzle: List[int], solution: List[int]) -> bool:
    if len(solution) != 81:
        return False

    for idx, given in enumerate(puzzle):
        if given and solution[idx] != given:
            return False

    digits = list(range(1, 10))
    for row in range(9):
        row_values = solution[row * 9:(row + 1) * 9]
        if sorted(row_values) != digits:
            return False

    for col in range(9):
        col_values = [solution[row * 9 + col] for row in range(9)]
        if sorted(col_values) != digits:
            return False

    for box_row in range(0, 9, 3):
        for box_col in range(0, 9, 3):
            values = []
            for row in range(box_row, box_row + 3):
                for col in range(box_col, box_col + 3):
                    values.append(solution[row * 9 + col])
            if sorted(values) != digits:
                return False
    return True


def build_payload(db_path: Path) -> dict:
    levels = extract_levels(db_path)
    level_payloads: Dict[str, Dict[str, str]] = {}
    for level_id, puzzle in sorted(levels.items()):
        solution = solve_board(puzzle)
        if not validate_solution(puzzle, solution):
            raise ValueError(f"invalid solution for level {level_id}")
        level_payloads[str(level_id)] = {
            "puzzle": "".join(str(value) for value in puzzle),
            "solution": "".join(str(value) for value in solution),
        }

    return {
        "meta": {
            "source_db": str(db_path),
            "level_count": len(level_payloads),
            "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        },
        "levels": level_payloads,
    }


def main() -> None:
    payload = build_payload(DB_PATH)
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"generated {payload['meta']['level_count']} levels -> {OUTPUT_PATH}")


if __name__ == "__main__":
    main()

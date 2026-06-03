import unittest
from pathlib import Path

from scripts.full_playthrough.generate_level_solutions import (
    build_payload,
    extract_levels,
    solve_board,
    validate_solution,
)


class GenerateLevelSolutionsTest(unittest.TestCase):
    def test_extract_levels_groups_rows_by_pass_num(self):
        db_path = Path("app/src/main/assets/sudoku.db")
        levels = extract_levels(db_path)
        self.assertEqual(len(levels), 40)
        self.assertEqual(levels[1][0], 0)
        self.assertEqual(len(levels[1]), 81)

    def test_solve_board_returns_full_solution_for_first_level(self):
        puzzle = [
            0, 3, 0, 6, 4, 7, 0, 8, 0,
            7, 0, 9, 0, 0, 0, 2, 0, 6,
            0, 1, 0, 9, 0, 3, 0, 4, 0,
            3, 0, 1, 0, 7, 0, 8, 0, 4,
            8, 0, 0, 3, 0, 4, 0, 0, 2,
            4, 0, 2, 0, 5, 0, 6, 0, 3,
            0, 8, 0, 5, 0, 1, 0, 2, 0,
            1, 0, 3, 0, 0, 0, 4, 0, 9,
            0, 2, 0, 4, 3, 9, 0, 6, 0,
        ]
        solution = solve_board(puzzle)
        self.assertEqual(len(solution), 81)
        self.assertTrue(validate_solution(puzzle, solution))

    def test_build_payload_contains_meta_and_level_payloads(self):
        db_path = Path("app/src/main/assets/sudoku.db")
        payload = build_payload(db_path)
        self.assertEqual(payload["meta"]["level_count"], 40)
        self.assertTrue(payload["levels"]["1"]["puzzle"])
        self.assertTrue(payload["levels"]["1"]["solution"])

    def test_build_payload_applies_known_level_overrides(self):
        db_path = Path("app/src/main/assets/sudoku.db")
        payload = build_payload(db_path)
        self.assertEqual(
            payload["levels"]["19"]["puzzle"],
            "009530600003000000500091028005803009306000804900104200790420003000000400002017900",
        )
        self.assertEqual(
            payload["levels"]["25"]["puzzle"],
            "046375890000000000000892000005903100020000050003206700000137000000000000097648310",
        )
        self.assertEqual(
            payload["levels"]["26"]["puzzle"],
            "230960001000007008000803000023000905400010003709000160000501000600300000800092057",
        )


if __name__ == "__main__":
    unittest.main()

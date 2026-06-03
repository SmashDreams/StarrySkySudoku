import unittest
from pathlib import Path

from scripts.full_playthrough.solution_store import SolutionStore


class SolutionStoreTest(unittest.TestCase):
    def test_solution_store_loads_first_level_solution(self):
        store = SolutionStore(Path("scripts/full_playthrough/data/level_solutions.json"))
        level = store.get_level(1)
        self.assertEqual(len(level.puzzle), 81)
        self.assertEqual(len(level.solution), 81)

    def test_solution_store_returns_editable_cells_only(self):
        store = SolutionStore(Path("scripts/full_playthrough/data/level_solutions.json"))
        moves = store.get_fill_steps(1)
        self.assertTrue(moves)
        row, col, digit = moves[0]
        self.assertGreaterEqual(row, 0)
        self.assertLess(row, 9)
        self.assertGreaterEqual(col, 0)
        self.assertLess(col, 9)
        self.assertGreaterEqual(digit, 1)
        self.assertLessEqual(digit, 9)


if __name__ == "__main__":
    unittest.main()

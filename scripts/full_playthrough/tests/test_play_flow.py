import unittest

from scripts.full_playthrough.play_flow import build_fill_plan, should_restart_level


class PlayFlowTest(unittest.TestCase):
    def test_build_fill_plan_returns_only_empty_cells_in_order(self):
        puzzle = "103000000" + ("0" * 72)
        solution = "123456789" * 9
        steps = build_fill_plan(puzzle, solution)
        self.assertEqual(steps[0], (0, 1, 2))
        self.assertEqual(steps[1], (0, 3, 4))

    def test_should_restart_level_when_retry_budget_exhausted(self):
        self.assertTrue(should_restart_level(retry_count=3, retry_limit=3))
        self.assertFalse(should_restart_level(retry_count=2, retry_limit=3))


if __name__ == "__main__":
    unittest.main()

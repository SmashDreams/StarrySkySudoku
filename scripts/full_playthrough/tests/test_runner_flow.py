import unittest

from scripts.full_playthrough.reporter import summarize_levels


class RunnerFlowTest(unittest.TestCase):
    def test_summarize_levels_counts_successes_and_failures(self):
        payload = summarize_levels(
            [
                {"level_id": 1, "status": "success"},
                {"level_id": 2, "status": "failed"},
            ]
        )
        self.assertEqual(payload["completed_levels"], 1)
        self.assertEqual(payload["failed_levels"], [2])

    def test_summarize_all_success(self):
        payload = summarize_levels(
            [
                {"level_id": 1, "status": "success"},
                {"level_id": 2, "status": "success"},
            ]
        )
        self.assertEqual(payload["completed_levels"], 2)
        self.assertEqual(payload["failed_levels"], [])


if __name__ == "__main__":
    unittest.main()

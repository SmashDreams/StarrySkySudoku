import unittest
from pathlib import Path

from scripts.full_playthrough.device import DeviceClient, parse_focus_line
from scripts.full_playthrough.reporter import Reporter, summarize_levels


class FoundationTest(unittest.TestCase):
    def test_reporter_creates_expected_directories(self):
        reporter = Reporter(Path("/tmp/full-playthrough-foundation-test"))
        run_dir = reporter.start_run()
        self.assertTrue((run_dir / "screenshots").exists())
        self.assertTrue((run_dir / "levels").exists())
        self.assertTrue((run_dir / "failures").exists())

    def test_device_client_formats_adb_command_with_device_serial(self):
        client = DeviceClient(device_serial="SERIAL123")
        self.assertEqual(client.adb_prefix(), ["adb", "-s", "SERIAL123"])

    def test_parse_focus_line_extracts_package_and_activity(self):
        line = "mCurrentFocus=Window{16ed888 u0 com.bird.starryskysudoku/com.bird.starryskysudoku.ui.play.PlayActivity}"
        package_name, activity_name = parse_focus_line(line)
        self.assertEqual(package_name, "com.bird.starryskysudoku")
        self.assertTrue(activity_name.endswith("PlayActivity"))

    def test_summarize_levels_counts_successes_and_failures(self):
        payload = summarize_levels([
            {"level_id": 1, "status": "success"},
            {"level_id": 2, "status": "failed"},
        ])
        self.assertEqual(payload["completed_levels"], 1)
        self.assertEqual(payload["failed_levels"], [2])


if __name__ == "__main__":
    unittest.main()

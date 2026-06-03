import unittest

from scripts.full_playthrough.guide_flow import (
    GuideStep,
    execute_basic_guide_progression,
    pick_guide_action,
)


class GuideFlowTest(unittest.TestCase):
    def test_pick_guide_action_returns_wait_for_intro_step(self):
        step = GuideStep(name="intro_wait", detector="guide_intro", action="wait")
        self.assertEqual(pick_guide_action(step), "wait")

    def test_execute_basic_guide_progression_taps_requested_times(self):
        class StubDevice:
            def __init__(self):
                self.calls = 0

            def tap_screen_center(self):
                self.calls += 1

            def current_package_and_activity(self):
                return ("com.bird.starryskysudoku", "com.bird.starryskysudoku.ui.guide.GuideActivity")

        class StubReporter:
            def __init__(self):
                self.messages = []

            def append_log(self, filename, message):
                self.messages.append((filename, message))

        device = StubDevice()
        reporter = StubReporter()
        completed = execute_basic_guide_progression(
            device,
            reporter,
            max_steps=3,
            settle_seconds=0.0,
        )

        self.assertEqual(completed, 3)
        self.assertEqual(device.calls, 3)
        self.assertEqual(len(reporter.messages), 3)


if __name__ == "__main__":
    unittest.main()

import unittest

from scripts.full_playthrough.models import AppState, DetectionResult
from scripts.full_playthrough.state_machine import next_action_for_detection


class StateMachineTest(unittest.TestCase):
    def test_out_of_app_routes_to_recover_action(self):
        detection = DetectionResult(state=AppState.OUT_OF_APP, package_name="com.android.settings")
        self.assertEqual(next_action_for_detection(detection), "recover_to_app")

    def test_map_routes_to_enter_next_level(self):
        detection = DetectionResult(state=AppState.MAP, package_name="com.bird.starryskysudoku")
        self.assertEqual(next_action_for_detection(detection), "enter_next_level")


if __name__ == "__main__":
    unittest.main()

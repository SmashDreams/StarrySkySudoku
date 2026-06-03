import unittest

from scripts.full_playthrough.geometry import BoardRect, cell_center


class GeometryTest(unittest.TestCase):
    def test_cell_center_returns_expected_center_point(self):
        board = BoardRect(left=100, top=200, width=900, height=900)
        self.assertEqual(cell_center(board, 0, 0), (175, 275))
        self.assertEqual(cell_center(board, 8, 8), (927, 1027))


if __name__ == "__main__":
    unittest.main()

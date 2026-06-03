import json
from pathlib import Path
from typing import Dict, List, Tuple

from .models import LevelSolution


class SolutionStore:
    def __init__(self, path: Path):
        self._path = path
        self._payload = json.loads(path.read_text(encoding="utf-8"))

    def level_ids(self) -> List[int]:
        return sorted(int(level_id) for level_id in self._payload["levels"].keys())

    def get_level(self, level_id: int) -> LevelSolution:
        item = self._payload["levels"][str(level_id)]
        puzzle = item["puzzle"]
        solution = item["solution"]
        if len(puzzle) != 81 or len(solution) != 81:
            raise ValueError("invalid level payload")
        return LevelSolution(level_id=level_id, puzzle=puzzle, solution=solution)

    def get_fill_steps(self, level_id: int) -> List[Tuple[int, int, int]]:
        level = self.get_level(level_id)
        steps = []
        for idx, cell in enumerate(level.puzzle):
            if cell == "0":
                row, col = divmod(idx, 9)
                steps.append((row, col, int(level.solution[idx])))
        return steps

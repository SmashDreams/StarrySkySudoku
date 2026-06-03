import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List


class Reporter:
    def __init__(self, output_root: Path):
        self.output_root = output_root
        self.run_dir = None

    def start_run(self) -> Path:
        timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        self.run_dir = self.output_root / timestamp
        for relative in ["screenshots", "levels", "failures", "device", "checkpoints"]:
            (self.run_dir / relative).mkdir(parents=True, exist_ok=True)
        return self.run_dir

    def append_log(self, filename: str, message: str) -> None:
        if self.run_dir is None:
            raise RuntimeError("run not started")
        with (self.run_dir / filename).open("a", encoding="utf-8") as handle:
            handle.write(message + "\n")

    def write_summary(self, payload: Dict) -> None:
        if self.run_dir is None:
            raise RuntimeError("run not started")
        (self.run_dir / "summary.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    def write_level_result(self, level_id: int, payload: Dict) -> None:
        if self.run_dir is None:
            raise RuntimeError("run not started")
        target = self.run_dir / "levels" / ("level_%02d.json" % level_id)
        target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def summarize_levels(level_results: List[Dict]) -> Dict:
    completed = [item["level_id"] for item in level_results if item["status"] == "success"]
    failed = [item["level_id"] for item in level_results if item["status"] != "success"]
    return {
        "completed_levels": len(completed),
        "failed_levels": failed,
    }

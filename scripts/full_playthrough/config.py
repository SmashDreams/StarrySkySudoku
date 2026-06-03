from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class RuntimeConfig:
    package_name: str = "com.bird.starryskysudoku"
    apk_path: Path = Path("app/build/outputs/apk/release/app-release-unsigned.apk")
    default_device_serial: str = "2FD0221410003338"
    output_root: Path = Path("artifacts/full-playthrough")
    unknown_retry_limit: int = 3
    level_retry_limit: int = 3

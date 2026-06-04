# scripts

本目录包含两套自动化测试脚本，分别存放在独立的子文件夹中。

## 目录结构

```
scripts/
├── README.md                    # 本文件
├── monkey/                      # Monkey 随机压测
│   ├── README.md
│   ├── monkey_run.sh
│   └── issue/
└── full_playthrough/            # 全量真人式通关
    ├── README.md
    ├── runner.py                # 主入口
    ├── generate_level_solutions.py
    ├── data/
    ├── tests/
    └── *.py                     # 各功能模块
```

---

## monkey — Monkey 随机压测

通过 Android Monkey 工具对应用执行伪随机点击压力测试，自动采集崩溃、ANR、进程退出等诊断信息。

```bash
# 已安装应用直接压测
scripts/monkey/monkey_run.sh \
  --device 2FD0221410003338 \
  --events 5000 \
  --throttle 80

# 从新安装开始（自动卸载→安装→授权→压测）
scripts/monkey/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --events 5000
```

详见：[monkey/README.md](monkey/README.md)

---

## full_playthrough — 全量真人式通关

从全新安装开始，自动完成引导页、逐关填入正确答案、处理通关弹窗，走完全部 40 关。

### 前提

- Python 3.8+，已安装 `opencv-python`
- 真机已连接 `adb devices`
- APK 已构建：`app/build/outputs/apk/release/app-release.apk`

### 快速开始

```bash
# 1. 生成全量答案（首次使用）
python3 scripts/full_playthrough/generate_level_solutions.py

# 2. 执行全量 40 关通关（从安装开始）
python3 scripts/full_playthrough/runner.py

# 3. 从指定关卡开始（跳过安装和引导）
python3 scripts/full_playthrough/runner.py --start-level 5 --skip-install --skip-guide

# 4. 只跑前 3 关
python3 scripts/full_playthrough/runner.py --start-level 1 --end-level 3

# 5. 本地预览（不操作设备）
python3 scripts/full_playthrough/runner.py --dry-run
```

### 命令行参数

| 参数 | 说明 | 默认值 |
|---|---|---|
| `--device` | 设备序列号 | `2FD0221410003338` |
| `--apk` | APK 路径 | `app/build/outputs/apk/release/app-release.apk` |
| `--start-level` | 起始关卡 | `1` |
| `--end-level` | 结束关卡 | `40` |
| `--skip-install` | 跳过卸载/安装 | 关闭 |
| `--skip-guide` | 跳过引导页 | 关闭 |
| `--dry-run` | 预览计划 | 关闭 |

### 运行测试

```bash
python3 -m unittest discover -s scripts/full_playthrough/tests -q
```

### 输出产物

每轮运行输出到 `artifacts/full-playthrough/<timestamp>/`：

- `summary.json` — 总摘要（完成数/失败列表）
- `runner.log` / `states.log` / `actions.log` — 分层日志
- `screenshots/` — 关键节点截图
- `levels/` — 逐关结果 JSON
- `failures/` — 失败现场（如有）

详见：[full_playthrough/README.md](full_playthrough/README.md)

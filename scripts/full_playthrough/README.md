# full_playthrough — 全量真人式通关自动化脚本

从全新安装开始，自动完成引导页，并以真人 UI 交互方式逐关填入数独答案，走完全部 40 关。

## 文件结构

```
scripts/full_playthrough/
├── runner.py                  # 主入口：安装→引导→40关循环→摘要
├── generate_level_solutions.py # 离线工具：从 sudoku.db 生成全量答案
├── geometry.py                # 棋盘/键盘坐标计算（布局参数驱动）
├── detector.py                # 页面分类 + 网格线检测 + 对话框定位
├── vision.py                  # 底层图像处理（OpenCV）
├── play_flow.py               # 关卡填入逻辑 + 地图入关
├── guide_flow.py              # 引导页推进
├── device.py                  # ADB 命令封装
├── reporter.py                # 日志/截图/摘要输出
├── solution_store.py          # 答案文件加载
├── state_machine.py           # 状态路由
├── models.py                  # 共享数据类/枚举
├── config.py                  # 默认配置
├── data/
│   └── level_solutions.json   # 预生成答案
└── tests/                     # 单元测试
```

## 前提条件

- Python 3.8+，已安装 `opencv-python` 和 `numpy`
- 真机 `2FD0221410003338` 已连接并可通过 `adb` 访问
- APK 已构建：`app/build/outputs/apk/debug/app-debug.apk`

## 快速开始

### 1. 生成答案文件

```bash
python3 scripts/full_playthrough/generate_level_solutions.py
```

产物：`scripts/full_playthrough/data/level_solutions.json`（全部 40 关的题面和解答）

### 2. 执行全量通关

```bash
python3 scripts/full_playthrough/runner.py
```

流程：卸载旧包 → 安装 APK → 启动 → 引导页（中心点击推进）→ 第 1 关星点检测入关 → 逐关填数 → 通关弹窗 → 下一关弹窗 → … → 40 关 → 摘要

### 3. 部分通关

```bash
# 从第 5 关开始（跳过安装和引导）
python3 scripts/full_playthrough/runner.py --start-level 5 --skip-install --skip-guide

# 只跑 1-3 关
python3 scripts/full_playthrough/runner.py --start-level 1 --end-level 3
```

### 4. Dry-run

```bash
python3 scripts/full_playthrough/runner.py --dry-run
```

## 命令行参数

| 参数 | 说明 | 默认值 |
|---|---|---|
| `--device` | 设备序列号 | `2FD0221410003338` |
| `--apk` | APK 路径 | `app/build/outputs/apk/debug/app-debug.apk` |
| `--start-level` | 起始关卡 | `1` |
| `--end-level` | 结束关卡 | `40` |
| `--skip-install` | 跳过卸载/安装 | 关闭 |
| `--skip-guide` | 跳过引导页 | 关闭 |
| `--dry-run` | 本地预览计划 | 关闭 |

## 棋盘定位原理

- **布局参数计算**（主要）：基于 BroadView 的 `CELL_INSET=28px`、`CELL_SIZE_OFFSET=54px`、`cellSize=(viewWidth-54)/9` 和 `vertical_bias=0.38`
- **边缘检测**（兜底）：Canny + 形态学提取网格线，聚类出 10×10 线阵，计算交点

数字键通过布局参数 + 实测校准定位（`vertical_bias=0.27`，55×60dp 按键）。

## 运行测试

```bash
python3 -m unittest discover -s scripts/full_playthrough/tests -q
```

## 输出产物

每轮运行输出到 `artifacts/full-playthrough/<timestamp>/`：

- `summary.json` — 总摘要（完成数/失败列表/耗时）
- `runner.log` — 高层流程
- `states.log` — 页面识别与状态切换
- `actions.log` — 点击/填数/恢复动作
- `screenshots/` — 关键节点截图
- `levels/` — 逐关结果 JSON
- `failures/` — 失败现场（如有）

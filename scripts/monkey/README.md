# monkey_run.sh 使用说明

`monkey_run.sh` 用于在 Android 真机上对 `com.bird.starryskysudoku` 执行 monkey 压测，并自动收集崩溃、ANR、进程退出、事件日志和系统诊断信息。

## 默认值

- 包名：`com.bird.starryskysudoku`
- 默认 APK：`app/build/outputs/apk/release/app-release-unsigned.apk`
- 默认输出：`artifacts/monkey`

## 能力概览

- 按批次执行 monkey，避免长时间完全失控
- 自动采集 `logcat`、`dumpsys`、`bugreport`
- 自动统计本轮测试内目标包相关的崩溃、ANR、进程死亡、dropbox 条目
- 自动检测设备锁屏、熄屏、前台焦点离开，并尝试拉回
- 支持 `--fresh-grant` 模式：卸载→安装→授权通知权限→启动测试

## 最常用的两种模式

### 常规压测（已安装应用）

```bash
scripts/monkey/monkey_run.sh \
  --device 2FD0221410003338 \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

### fresh-grant 模式（从新安装开始）

```bash
scripts/monkey/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

## 完整参数

| 参数 | 说明 | 默认值 |
|---|---|---|
| `--device SERIAL` | 设备序列号 | - |
| `--package NAME` | 目标包名 | `com.bird.starryskysudoku` |
| `--events COUNT` | monkey 事件总数 | `50000` |
| `--throttle MS` | 事件间隔(ms) | `200` |
| `--batch-size COUNT` | 每批次事件数 | `100` |
| `--seed VALUE` | 随机种子 | 当前时间戳 |
| `--fresh-grant` | 卸载→安装→授权→压测 | 关闭 |
| `--apk PATH` | APK 路径 | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| `--no-bugreport` | 不抓取 bugreport | 关闭 |
| `--no-launch` | 不主动拉起应用 | 关闭 |
| `--output-root DIR` | 输出根目录 | `artifacts/monkey` |

## 事件权重

当前只做应用内点击压力测试：

- `--pct-touch 100`
- 其余事件类型全为 0

## 输出产物

每次执行生成 `artifacts/monkey/<timestamp>/`：

- `summary.txt` — 摘要（崩溃/ANR/进程死亡统计）
- `watchdog.txt` — 前台焦点偏离、锁屏、自动拉回事件
- `monkey.log` — monkey 原始日志
- `logcat-all.txt` — 全量 logcat
- `logcat-crash.txt` — 崩溃/错误过滤
- `logcat-events.txt` — Activity/Window 事件
- `dumpsys-activity.txt` — Activity 状态
- `dumpsys-package.txt` — 包信息
- `dumpsys-dropbox.txt` — dropbox 条目
- `bugreport.zip` — 完整 bugreport

## summary.txt 关键字段

- `fatal_or_native_crash_matches` — 崩溃 / native crash 数量
- `anr_matches` — ANR 数量
- `process_death_matches` — 进程死亡数量
- `dropbox_issue_matches` — dropbox 问题条目
- `interventions` — 自动恢复前台次数
- `status` — `finished` 或 `ended_with_interrupt_or_error`

# monkey_run.sh 使用说明

这个目录下的 `monkey_run.sh` 用于在 Android 真机上对 `com.bird.starryskysudoku` 执行 monkey 压测，并自动收集崩溃、ANR、进程退出、事件日志和系统诊断信息。

当前脚本默认服务于本项目，已经内置以下默认值：

- 包名：`com.bird.starryskysudoku`
- 默认 APK 路径：`app/build/outputs/apk/debug/app-debug.apk`
- 默认输出目录：`artifacts/monkey`

## 一、脚本能力概览

脚本当前具备以下能力：

- 按批次执行 monkey，避免长时间完全失控
- 自动采集 `logcat`、`dumpsys`、`bugreport`
- 自动统计本轮测试内、目标包相关的：
  - 应用崩溃
  - ANR
  - 进程死亡
  - dropbox 问题条目
- 自动检测设备是否锁屏、熄屏、或前台焦点离开目标应用
- 当应用被系统页面抢走前台时，自动尝试拉回应用
- 支持“新安装 + 自动授权通知权限”后再开始 monkey

注意：

- 这个脚本当前不会自动构建 APK
- `--fresh-grant` 模式下默认直接使用：`app/build/outputs/apk/debug/app-debug.apk`
- 如果你后续改成别的构建产物，可以显式传 `--apk 路径`

## 二、使用前提

执行脚本前，请确保：

- 真机已连接并可被 `adb devices` 识别
- 目标 APK 已存在于项目目录中
- 如果使用默认 APK 路径，请确保这个文件存在：
  - `app/build/outputs/apk/debug/app-debug.apk`

如果有多个设备连接，建议显式传入 `--device SERIAL`。

## 三、最常用的两种模式

### 1. 常规压测模式

适用于：

- 应用已经安装到真机
- 不需要重新安装
- 直接开始 monkey

示例：

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

### 2. fresh-grant 模式

适用于：

- 你希望从“新安装状态”开始压测
- 但又不希望被通知权限弹窗打断
- 需要脚本自动完成卸载、安装、授权、启动测试

示例：

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

这个模式会自动执行：

1. 卸载设备中的旧包（如果已安装）
2. 安装默认 APK：`app/build/outputs/apk/debug/app-debug.apk`
3. 尝试授予：`android.permission.POST_NOTIFICATIONS`
4. 启动应用并执行 monkey

如果你不想使用默认 APK，可以手动指定：

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --apk app/build/outputs/apk/release/app-release-unsigned.apk \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

## 四、完整参数说明

### 基础参数

- `--device SERIAL`
  - 指定测试设备序列号
  - 如果只有一台设备在线，可以不传

- `--package NAME`
  - 指定目标包名
  - 默认：`com.bird.starryskysudoku`

- `--events COUNT`
  - monkey 事件总数
  - 默认：`50000`

- `--throttle MS`
  - 每次事件之间的间隔，单位毫秒
  - 默认：`200`

- `--batch-size COUNT`
  - 每批次执行多少个事件后做一次安全检查
  - 默认：`100`

- `--seed VALUE`
  - 指定 monkey 随机种子，便于复现
  - 默认：当前时间戳

### fresh-grant 相关参数

- `--fresh-grant`
  - 开启“卸载 -> 安装 -> 授权通知权限 -> 压测”的流程

- `--apk PATH`
  - 指定安装用 APK 路径
  - 默认：`app/build/outputs/apk/debug/app-debug.apk`

### 日志和启动控制

- `--no-bugreport`
  - 不抓取 `bugreport.zip`
  - 适合快速测试，能明显缩短收尾时间

- `--no-launch`
  - monkey 开始前不主动拉起应用
  - 适合你希望手动把应用停留在特定页面后再测试

- `--output-root DIR`
  - 指定输出根目录
  - 默认：`artifacts/monkey`

- `-h` / `--help`
  - 查看帮助

## 五、脚本默认测试行为

脚本当前为了尽量减少系统页面干扰，已经把 monkey 权重收紧到了主要点击事件：

- `--pct-touch 100`
- `--pct-motion 0`
- `--pct-nav 0`
- `--pct-majornav 0`
- `--pct-appswitch 0`
- `--pct-syskeys 0`
- `--pct-trackball 0`
- `--pct-anyevent 0`

这意味着：

- 当前压测更偏向“应用内点击压力测试”
- 不会主动模拟系统键、应用切换、通知栏操作
- 但某些设备/系统页面仍可能因为应用行为本身或系统策略而插入前台

## 六、输出结果说明

每次执行都会在 `artifacts/monkey/<时间戳>/` 下生成一套完整产物。

典型文件包括：

- `summary.txt`
  - 本轮测试摘要
  - 包括设备、事件数、统计结果、完成状态

- `watchdog.txt`
  - 记录前台焦点偏离、锁屏、自动拉回等事件

- `monkey.log`
  - monkey 原始执行日志

- `logcat-all.txt`
  - 全量 `logcat`

- `logcat-crash.txt`
  - 崩溃/错误级别过滤日志

- `logcat-events.txt`
  - Activity / Window / monkey 事件日志

- `dumpsys-activity.txt`
  - 结束时抓取的 activity 状态

- `dumpsys-package.txt`
  - 结束时抓取的包信息

- `dumpsys-dropbox.txt`
  - 系统 dropbox 条目

- `bugreport.zip`
  - 完整 bugreport（如果没有使用 `--no-bugreport`）

## 七、summary.txt 中关键字段含义

- `fatal_or_native_crash_matches`
  - 本轮时间窗口内，目标包对应的崩溃 / native crash 条目数量

- `anr_matches`
  - 本轮时间窗口内，目标包对应的 ANR 条目数量

- `process_death_matches`
  - 本轮时间窗口内，目标包对应的进程死亡数量

- `dropbox_issue_matches`
  - 本轮时间窗口内，目标包在 dropbox 中出现的问题条目数量

- `monkey_completion_markers`
  - monkey 完成批次数
  - 正常情况下应接近：`events / batch-size`

- `interventions`
  - 脚本自动恢复前台的次数

- `status`
  - `finished` 表示事件跑完
  - `ended_with_interrupt_or_error` 表示中途终止

## 八、推荐用法

### 快速验证

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --events 1000 \
  --throttle 80 \
  --batch-size 50 \
  --no-bugreport
```

### 正式压测

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --events 5000 \
  --throttle 80 \
  --batch-size 50
```

### 长时压测

```bash
scripts/monkey_run.sh \
  --device 2FD0221410003338 \
  --fresh-grant \
  --events 20000 \
  --throttle 80 \
  --batch-size 50
```

## 九、常见问题

### 1. 提示 `APK not found`

说明默认路径下没有 APK：

- `app/build/outputs/apk/debug/app-debug.apk`

处理方式：

- 先构建 APK
- 或者显式传入 `--apk 你的实际路径`

### 2. 提示 `Package not installed on device`

说明：

- 非 `fresh-grant` 模式下，设备里还没有安装应用
- 或者安装失败

处理方式：

- 使用 `--fresh-grant`
- 或者先手动安装 APK

### 3. monkey 跑着跑着还是被系统页面打断

说明：

- 虽然脚本已尽量约束事件分布
- 但某些页面会因为应用行为、权限页、厂商系统策略而仍然插入前台

当前脚本处理方式是：

- 自动记录到 `watchdog.txt`
- 自动尝试把应用重新拉回前台

### 4. 为什么统计结果和 `logcat` 里看到的系统错误数量不一致

因为脚本的汇总已经做了两层过滤：

- 只统计目标包 `com.bird.starryskysudoku`
- 只统计本轮测试时间窗口内产生的条目

所以系统噪声、其他应用错误、历史 dropbox 条目不会被算进最终统计。

## 十、当前默认 APK 路径说明

当前脚本内置的默认 APK 路径是：

- `app/build/outputs/apk/debug/app-debug.apk`

这是我在当前项目目录中实际找到的构建产物路径之一，适合作为默认的 fresh-grant 安装来源。

如果你后续改成 release 包或别的输出路径，请同步通过 `--apk` 指定。

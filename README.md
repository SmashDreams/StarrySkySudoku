# 星空数独 (StarrySkySudoku)

一款星空主题的数独游戏 Android 应用，40 个关卡从易到难，支持标记、撤销、倒计时、前台通知、跨应用战绩共享和茶苑登录联动。

## 版本

**2.0** — 补全测试自动化脚本，修复倒计时跳秒、前台通知延迟、暂停时间不同步和音效音量。

## 技术栈

| 层面 | 技术选型 |
|---|---|
| 语言 | Kotlin (JVM 17) |
| 架构 | MVVM + Controller 委托 + Repository 模式 |
| 响应式 | LiveData + Lifecycle 感知观察者 |
| 数据库 | Room (v6, KSP 编译), 5 次增量迁移 |
| 跨进程通信 | ContentProvider + 共享契约源码目录 |
| 倒计时 | 前台 Service + BroadcastReceiver + Handler/elapsedRealtime |
| 棋盘渲染 | 自定义 View (Canvas 三层绘制) + Bitmap 状态图 |
| 音效 | SoundPool (Builder API, 双池隔离) |
| BGM | MediaPlayer + Service 绑定 + ActivityLifecycleCallbacks |
| DI | 手动 ViewModelProvider.Factory (无 Hilt/Dagger) |
| 测试 | JUnit + Robolectric + Espresso + 结构测试 |
| 自动化脚本 | Python + OpenCV + ADB + Bash |
| 构建 | Gradle 8.11 + Version Catalog + ProGuard |

## 架构

```
Activity 层
  AppEntry -> Guide -> Map <-> Play
               |          |
          MapViewModel  PlayViewModel
               |          |
  +------------+----------+------------------------+
  | Repository 层                                    |
  | MapRepository / PlayRepository /                |
  | UserProgressRepository                          |
  +------------+------------------------------------+
               |
  +------------+------------------------------------+
  | Data 层 (Room DAO -> SQLite)                    |
  | ProblemDao / UserMapDao / HistoryDao /          |
  | GameResultDao                                    |
  +-------------------------------------------------+

  Controller 委托 (PlayActivity -> 6 Controller)
  Board / Input / GameState / Nav / Dialog /
  CountdownCoordinator

  跨进程  ContentProvider -> 茶苑 Launcher 读战绩
  计时    CountdownTimerService (前台) -> 广播 -> UI
```

## 数据库

Room 数据库 `AppDatabase` (版本 6)，4 张表：

| 表 | 用途 | 关键字段 |
|---|---|---|
| `problem` | 40 关题目数据 | `pass_num`, `value` (81 个/关) |
| `user_map` | 每用户关卡进度 | `username`+`pass_num` (复合主键), `status`, `play_time` |
| `history` | 撤销/重做历史 | `row`, `col`, `type`, `value`, `pass_num`, `game_session` |
| `game_result` | 通关/失败战绩 | `level`, `elapsed_seconds`, `completed`, `username` |

`DatabaseInitializer` 在首次启动时从 `assets/sudoku.db` 预填充题目数据并执行增量迁移 (v1→v6)。

## 模块说明

### `ui/play/` — 棋盘页

核心技术：Canvas 自定义绘制 + 触摸坐标解析 + 6 控制器委托。

- **BroadView** — 核心棋盘 View，继承 `AppCompatImageView`，三层绘制：
  1. `onDraw`: 81 个格子的 7 种状态 Bitmap (given/empty/selected/error × 不同组合)
  2. `dispatchDraw`: 候选标记 (3×3 子网格)
  3. `onDrawForeground`: 数字文本 (80sp)、1px 格线、9px 宫线、2px 白色外框
- **SudokuBoardGeometry** — 几何计算：`cellSize=(viewWidth-54)/9`, `CELL_INSET=28`, `BORDER_INSET=24`
- **PlayBoardRules** — 纯规则逻辑：创建题面、选中高亮同行/列/宫/同数、填数冲突检测、完成判定、错误回滚
- **6 个 Controller** — 棋盘、输入、游戏状态、导航、弹窗、倒计时协调器，各自独立注入 `PlayActivity`

### `timer/` — 倒计时模块

- **CountdownTimerService** — 前台 Service (`foregroundServiceType="specialUse"`)，`Handler` + `SystemClock.elapsedRealtime()` 锚定结束时间反算剩余秒数，支持 `ACTION_PAUSE_TIMER`/`ACTION_RESUME_TIMER` 暂停恢复，每秒系统广播 `ACTION_COUNTDOWN_TICK`
- **CountdownTimerContract** — 常量与格式化，默认 600s (10 分钟)
- **CountdownCoordinator** — 桥接 Service 与 Activity，管理启动/停止/暂停/恢复，注册/解注册 BroadcastReceiver

### `ui/map/` — 地图页

- **MapActivity** — RecyclerView 倒序渲染 10 行×4 列星星节点，header 行 + MapPathOverlayView 虚线连接 + 流星动画
- **MapNotificationNavigator** — Android 13+ 通知权限前置处理 + 华为设备厂商通知预热 (同 ID 覆盖，避免取消重建延迟)
- **MapPassDialogController** — 关卡确认弹窗，按钮点击播放音效→关闭弹窗→跳转 PlayActivity
- **MapSettingsController** — 音乐/音效开关、语言切换 (zh/en)、玩法说明入口

### `media/` — 音频模块

- **PlayMusic** — 单例 SoundPool，8 种短音效，双池隔离 (超时提示音独立池避免流争抢)
- **BgmMusicService** — MediaPlayer 循环 BGM，50% 音量，Service 绑定模式
- **AppForegroundBgmController** — `ActivityLifecycleCallbacks` 追踪前后台，自动 bind/pause/unbind BGM

### `data/` — 数据层

- **Repository 模式** — `PlayRepository` (棋盘+历史)、`MapRepository` (地图+进度)、`UserProgressRepository` (关卡解锁链)
- **GameResultProvider** — ContentProvider 对外提供战绩，签名级权限保护，支持 query/insert/delete
- **Manual DI** — ViewModelFactory 手动构建 Repository → DAO → Database 依赖链

### `account/` — 登录态

- **LauncherSessionReader** — ContentResolver 读取茶苑 Session Provider，fallback `"guest"`
- **共享契约** — `shared-contracts/` 目录供两端项目通过 Gradle `sourceSets` 共同引用

### `ui/guide/` — 新手引导

- **GuideActivity** — 6 步交互教程，GuideSpotlightView 高亮区域 + 演示棋盘 + 步骤说明文字
- **GuideBoardFactory** — 为教程生成预配置的演示棋盘

## 自动化测试脚本

位于 `scripts/` 目录，详见 [scripts/README.md](scripts/README.md)。

```
scripts/
├── start.sh                    # 交互式启动：选择设备 → Monkey 压测 / 全量通关
├── monkey/                     # Android Monkey 随机点击压测
│   ├── monkey_run.sh           # 分批压测 + 崩溃/ANR 诊断采集
│   └── README.md
└── full_playthrough/           # 全量 40 关真人式通关
    ├── runner.py               # 入口：安装→引导→逐关填答案→通关弹窗处理
    ├── generate_level_solutions.py  # 离线回溯求解全部 40 关
    ├── geometry.py             # 棋盘/键盘坐标计算 (BroadView 几何参数)
    ├── detector.py             # 页面识别 (梯度/网格线/星点/对话框)
    ├── vision.py               # OpenCV 底层图像处理
    ├── data/                   # 预生成答案 JSON
    ├── tests/                  # 19 个单元测试
    └── README.md
```

## 跨应用契约

2.0 起集中在 `shared-contracts/` 下维护，茶苑通过相邻目录引用。

**星空数独读取茶苑登录态：**

| 项 | 值 |
|---|---|
| Authority | `com.bird.starryskyteahouse.provider` |
| URI | `content://com.bird.starryskyteahouse.provider/session` |
| 权限 | `com.bird.starryskyteahouse.permission.READ_SESSION` |
| 字段 | `username`, `logged_in` |

**星空数独对外提供战绩：**

| 项 | 值 |
|---|---|
| Authority | `com.bird.starryskysudoku.provider` |
| URI | `content://com.bird.starryskysudoku.provider/results` |
| 读权限 | `com.bird.starryskysudoku.permission.READ_RESULTS` |
| 写权限 | `com.bird.starryskysudoku.permission.WRITE_RESULTS` |
| 排序 | `created_at DESC` |

## 构建

```bash
# Debug
./gradlew assembleDebug

# Release (需 local.properties 配置签名)
./gradlew assembleRelease

# 单元测试
./gradlew test

# Lint
./gradlew lintDebug
```

## 版本记录

- **2.0** — 修复倒计时跳秒 (Handler 替代 CountDownTimer)、前台通知 HarmonyOS 6s 延迟、暂停时间不同步、后台暂停通知保持；新增全量真人式通关和 Monkey 压测自动化脚本；增大背景音乐和点击音效音量
- **1.5 维护更新** — 拆分棋盘页控制器职责，抽出 PlayBoardRules 并补充规则测试
- **1.5** — 新增倒计时前台通知；Android 13+ 通知权限前置 + 华为厂商通知预热
- **1.4** — 补全四大组件：Service 倒计时广播、Activity 动态 Receiver、Room + ContentProvider 战绩共享
- **1.3** — 重构导航界面，统一棋盘绘制与教程高亮坐标计算
- **1.2** — 使用真实棋盘展示新手教程，加入遮罩、高亮和第 1 关题面演示
- **1.1** — 迁移废弃 API，现代化 Activity 转场、语言切换和 Kotlin Gradle 配置
- **1.0** — 星空主题数独基础玩法、关卡地图、音效和中英文切换

## 作者

SmashDreams

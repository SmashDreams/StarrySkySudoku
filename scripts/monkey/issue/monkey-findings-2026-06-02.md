# Monkey 压测问题定位记录

日期：2026-06-02
项目：`StarrySkySudoku`
目标包名：`com.bird.starryskysudoku`
测试方式：Android 真机 `monkey` 压测
测试设备：`2FD0221410003338` / `NOH_AN01`

## 一、文档目的

这份文档用于沉淀本次 monkey 压测中已经确认的问题。

范围限定如下：

- 只记录已经通过真机日志、monkey 结果、代码阅读三者交叉确认的问题
- 只记录问题现象、触发条件、代码定位和原因分析
- 不包含修复方案
- 不包含尚未确认的猜测性问题

## 二、本次压测中确认的问题总览

本次压测确认的真实应用问题主要有两类：

1. `CountdownTimerService` 前台服务启动窗口被生命周期打断，导致应用崩溃
2. `BgmMusicService` 在后台限制场景下被普通 `startService` 启动，导致应用崩溃

此外，还确认了一个会放大上述问题的外部触发条件：

3. 系统通知权限页 `GrantPermissionsActivity` 仍会插入棋盘页流程，导致前台焦点切换和生命周期扰动

## 三、问题 1：CountdownTimerService 前台服务启动竞态导致崩溃

### 3.1 问题现象

在 monkey 压测过程中，应用发生崩溃，异常为：

- `ForegroundServiceDidNotStartInTimeException`

对应服务：

- `com.bird.starryskysudoku/.timer.CountdownTimerService`

### 3.2 日志证据

本轮确认问题的主要证据位于：

- `artifacts/monkey/2026-06-02_15-17-11/logcat-crash.txt:5`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-all.txt:23800`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-all.txt:23565`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-all.txt:24052`
- `artifacts/monkey/2026-06-02_15-17-11/dumpsys-dropbox.txt:1512`

关键日志结论如下：

- 系统明确抛出：`ForegroundServiceDidNotStartInTimeException`
- 崩溃目标进程是：`com.bird.starryskysudoku`
- 崩溃目标服务是：`CountdownTimerService`
- 系统还明确记录了：
  - `Bringing down service while still waiting for start foreground`

这说明服务还处于“系统等待其完成前台化”的窗口中，就已经被系统判定需要拆除。

### 3.3 代码定位

#### 服务启动入口

- `app/src/main/java/com/bird/starryskysudoku/ui/play/CountdownCoordinator.kt:51`
- `app/src/main/java/com/bird/starryskysudoku/ui/play/CountdownCoordinator.kt:57`

说明：

- `CountdownCoordinator.start()` 在 Android 8+ 上调用 `startForegroundService(...)`

#### 服务停止入口

- `app/src/main/java/com/bird/starryskysudoku/ui/play/CountdownCoordinator.kt:64`

说明：

- `CountdownCoordinator.stop()` 直接调用 `stopService(...)`

#### 页面生命周期里的调用点

- `app/src/main/java/com/bird/starryskysudoku/ui/play/PlayNavigationController.kt:22`
- `app/src/main/java/com/bird/starryskysudoku/ui/play/PlayNavigationController.kt:26`
- `app/src/main/java/com/bird/starryskysudoku/ui/play/PlayNavigationController.kt:32`
- `app/src/main/java/com/bird/starryskysudoku/ui/play/PlayNavigationController.kt:41`

说明：

- 页面 `onPause()` 时无条件停止倒计时服务
- 页面 `onResume()` 时在满足条件时重新启动倒计时服务

#### 服务内部前台化逻辑

- `app/src/main/java/com/bird/starryskysudoku/timer/CountdownTimerService.kt:31`
- `app/src/main/java/com/bird/starryskysudoku/timer/CountdownTimerService.kt:44`
- `app/src/main/java/com/bird/starryskysudoku/timer/CountdownTimerService.kt:55`
- `app/src/main/java/com/bird/starryskysudoku/timer/CountdownTimerService.kt:57`

说明：

- `CountdownTimerService` 的 `onStartCommand()` 内部确实会调用 `startForeground(...)`
- 因此问题并不是“服务内部完全没有转前台”

### 3.4 原因分析

根因不是单一代码点，而是一个生命周期竞态：

1. 页面恢复时，`CountdownCoordinator.start()` 调用 `startForegroundService(...)`
2. 服务开始进入“必须尽快调用 `startForeground()`”的系统等待窗口
3. 但页面前台可能被短暂打断，例如被权限页或系统覆盖层抢走焦点
4. 页面 `onPause()` 触发后，`PlayNavigationController.onPause()` 无条件执行 `mCountdownCoordinator.stop()`
5. 服务在尚未稳定完成前台化前，就被页面侧停掉
6. 系统最终抛出 `ForegroundServiceDidNotStartInTimeException`

### 3.5 结论

`CountdownTimerService` 的崩溃根因是：

- 倒计时前台服务的启停过度绑定 `Activity` 的短时可见性变化
- 一旦出现页面切换、权限页插入、系统覆盖层抢焦点，就会形成：
  - `startForegroundService(...) -> stopService(...)`
- 这个竞态直接导致前台服务启动窗口被打断，最终触发崩溃

## 四、问题 2：BgmMusicService 后台启动限制导致崩溃

### 4.1 问题现象

在 monkey 压测中，应用还出现过另一类崩溃，异常为：

- `BackgroundServiceStartNotAllowedException`

对应服务：

- `com.bird.starryskysudoku/.media.BgmMusicService`

### 4.2 日志证据

主要证据位于：

- `artifacts/monkey/2026-06-02_14-29-00/logcat-crash.txt:73`
- `artifacts/monkey/2026-06-02_14-29-00/logcat-crash.txt:77`
- `artifacts/monkey/2026-06-02_14-29-00/logcat-all.txt:23968`
- `artifacts/monkey/2026-06-02_14-29-00/logcat-events.txt:480`
- `artifacts/monkey/2026-06-02_14-29-00/logcat-events.txt:628`
- `artifacts/monkey/2026-06-02_14-29-00/logcat-events.txt:708`

关键日志结论如下：

- 系统明确判定：后台不允许启动该服务
- 堆栈清晰指向：
  - `BgmMusicController.send(...)`
  - `BgmMusicController.playIfEnabled(...)`
  - `AppForegroundBgmController.onActivityStarted(...)`

### 4.3 代码定位

#### 控制入口

- `app/src/main/java/com/bird/starryskysudoku/media/BgmMusicController.kt:12`
- `app/src/main/java/com/bird/starryskysudoku/media/BgmMusicController.kt:28`

说明：

- `playIfEnabled(...)` 最终通过 `context.applicationContext.startService(...)` 发送播放命令

#### 应用级生命周期接管点

- `app/src/main/java/com/bird/starryskysudoku/media/AppForegroundBgmController.kt:20`
- `app/src/main/java/com/bird/starryskysudoku/media/AppForegroundBgmController.kt:36`

说明：

- 应用回到前台时，会自动调用 `BgmMusicController.playIfEnabled(...)`
- 某些恢复场景下还会在 `onActivityResumed(...)` 再兜底调用一次

#### 服务类型

- `app/src/main/java/com/bird/starryskysudoku/media/BgmMusicService.kt:9`

说明：

- `BgmMusicService` 是普通 `Service`
- 并不是前台服务

#### 应用注册位置

- `app/src/main/java/com/bird/starryskysudoku/StarrySkySudokuApp.kt:16`

说明：

- 应用启动后即注册 `AppForegroundBgmController`
- 背景音乐生命周期由应用级回调统一接管

### 4.4 原因分析

问题链路如下：

1. 应用级生命周期回调感知到页面启动或恢复
2. `AppForegroundBgmController` 调用 `BgmMusicController.playIfEnabled(...)`
3. `BgmMusicController` 直接用 `startService(...)` 启动普通背景服务
4. 但此时系统未必把应用判定为稳定前台，可能仍处于：
   - 页面切换中
   - 权限页插入中
   - 短暂后台/恢复边界中
5. Android 后台限制生效，直接抛出：
   - `BackgroundServiceStartNotAllowedException`

### 4.5 结论

`BgmMusicService` 的根因是：

- 背景音乐服务采用了“应用级生命周期回调 + 普通 `startService(...)`”的启动模型
- 该模型在 Android 后台限制下不稳定
- 一旦生命周期回调发生在系统视角的“非安全前台窗口”里，就会直接崩溃

## 五、问题 3：系统权限页仍会插入棋盘页流程

### 5.1 问题现象

在进入或运行棋盘页期间，前台会被系统权限页抢走：

- `com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity`

这不是应用直接崩溃，但它会显著放大倒计时服务和背景音乐服务的问题。

### 5.2 日志证据

- `artifacts/monkey/2026-06-02_15-17-11/watchdog.txt`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-events.txt:605`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-events.txt:636`
- `artifacts/monkey/2026-06-02_15-17-11/logcat-events.txt:651`

从这些日志可以确认：

- 权限页确实在棋盘页阶段被系统拉起
- 前台焦点从应用页面切到权限页
- 脚本多次记录并尝试把应用拉回前台

### 5.3 代码定位

#### 地图页通知权限导航逻辑

- `app/src/main/java/com/bird/starryskysudoku/ui/map/MapNotificationNavigator.kt:36`
- `app/src/main/java/com/bird/starryskysudoku/ui/map/MapNotificationNavigator.kt:65`
- `app/src/main/java/com/bird/starryskysudoku/ui/map/MapNotificationNavigator.kt:97`
- `app/src/main/java/com/bird/starryskysudoku/ui/map/MapNotificationNavigator.kt:109`

#### 权限策略

- `app/src/main/java/com/bird/starryskysudoku/notification/NotificationPermissionPolicy.kt:8`
- `app/src/main/java/com/bird/starryskysudoku/notification/NotificationPermissionPolicy.kt:13`
- `app/src/main/java/com/bird/starryskysudoku/notification/NotificationPermissionPolicy.kt:20`

### 5.4 原因分析

代码设计上已经尝试把通知权限处理前置到地图页：

- 地图页进入棋盘前先处理 `POST_NOTIFICATIONS`
- 华为低版本系统还增加了“通知预热”逻辑

但是从真机日志看，这层保护并没有完全阻止权限页在棋盘阶段插入。

这说明：

- 权限页触发条件并没有被现有预热策略彻底覆盖
- 或者某些设备/系统时序下，通知相关动作仍会在棋盘页阶段触发权限链路

### 5.5 结论

权限页插入不是单独的崩溃点，但它是当前服务生命周期问题的重要外部触发器：

- 它会打断 `PlayActivity` 前台状态
- 它会触发 `onPause()/onResume()` 链路
- 它会放大 `CountdownTimerService` 和 `BgmMusicService` 的竞态与后台限制问题

## 六、设计层面的共性问题

除了上面三个可直接落到日志的问题，还确认了两个更上层的结构性问题。

### 6.1 服务状态过度依赖短时页面生命周期

涉及：

- `CountdownTimerService`
- `BgmMusicService`

共同特征：

- 服务启停过多依赖 `Activity` 的 `onResume/onPause/onStart/onStop`
- 但页面生命周期并不等同于稳定前台状态
- 一旦系统页、权限页、切页动画、页面重建插入，就会出现错误判断

### 6.2 系统状态建模过于理想化

项目当前的多个地方默认认为：

- `onActivityStarted()` 说明应用已安全回到前台
- `onResume()` 后即可安全拉起服务
- 通知权限预热一次后，后续就不会再被权限页打断

而真机压测结果证明，这些假设在当前设备和系统版本下并不成立。

## 七、本次确认的问题清单

本次压测已确认的问题可以压缩为如下列表：

1. `CountdownTimerService` 存在前台服务启动/停止竞态
2. `CountdownTimerService` 会因该竞态触发 `ForegroundServiceDidNotStartInTimeException`
3. `BgmMusicService` 使用普通 `startService(...)`，会触发后台启动限制
4. `BgmMusicService` 已在真机 monkey 中触发 `BackgroundServiceStartNotAllowedException`
5. 系统权限页 `GrantPermissionsActivity` 仍会插入棋盘页流程
6. 权限页插入会放大倒计时服务与背景音乐服务的问题
7. 当前服务生命周期管理过度依赖页面短时可见性，设计边界不稳定

## 八、后续修复时建议优先关注的文件

- `app/src/main/java/com/bird/starryskysudoku/ui/play/CountdownCoordinator.kt`
- `app/src/main/java/com/bird/starryskysudoku/ui/play/PlayNavigationController.kt`
- `app/src/main/java/com/bird/starryskysudoku/timer/CountdownTimerService.kt`
- `app/src/main/java/com/bird/starryskysudoku/media/BgmMusicController.kt`
- `app/src/main/java/com/bird/starryskysudoku/media/AppForegroundBgmController.kt`
- `app/src/main/java/com/bird/starryskysudoku/media/BgmMusicService.kt`
- `app/src/main/java/com/bird/starryskysudoku/ui/map/MapNotificationNavigator.kt`
- `app/src/main/java/com/bird/starryskysudoku/notification/NotificationPermissionPolicy.kt`

## 九、说明

这份文档仅记录定位结果。

如果后续代码修复完成，应重新执行一轮 fresh-grant 模式 monkey 压测，验证以下目标：

- 不再出现 `ForegroundServiceDidNotStartInTimeException`
- 不再出现 `BackgroundServiceStartNotAllowedException`
- 权限页即便插入，也不会再引发服务崩溃

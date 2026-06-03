# 星空数独 (StarrySkySudoku)

一款星空主题的数独游戏 Android 应用，包含 40 个关卡（从易到难）。

## 当前版本
- 版本：2.0
- 更新内容：完成页面逻辑加注释修正，补齐地图页、棋盘页、倒计时、战绩、登录态、语言切换和背景音乐等核心链路的中文注释；同步整理当前项目结构与脚本目录说明。

## 功能
- 标准数独玩法：9×9 棋盘，行列宫内无重复
- 标记模式：空格内可标记候选数字（1-9 均可）
- 撤销功能：支持数字和标记撤销（上限 20 条）
- 倒计时：每关 10 分钟，由后台 Service 每秒广播剩余时间
- 前台通知：游戏中通过前台 Service 展示关卡名和剩余时间
- 通知权限前置：进入棋盘前处理 Android 13+ 通知权限，并预热华为 Android 12 的厂商通知授权
- 关卡地图：星空主题滚动地图，已通关关卡点亮星星
- 战绩共享：通过 ContentProvider 对外提供 Room 存储的通关/失败记录
- 茶苑登录联动：读取星空茶苑登录态 Provider，按当前用户保存和查询战绩
- 背景音乐/音效：独立开关
- 中英文切换

## 技术栈
- Kotlin
- Room 数据库
- ViewModel + LiveData
- Android 四大组件：Activity / Service / BroadcastReceiver / ContentProvider
- SoundPool (Builder API)
- AndroidX AppCompat / Activity KTX
- Gradle Version Catalog
- 共享源码契约（`shared-contracts` + Gradle `sourceSets`）

## 项目结构

- `ui/splash/AppEntryActivity.kt`：启动页入口，负责首启引导分流和按当前语言选择启动图
- `ui/common/BaseLocalizedActivity.kt`、`ui/common/AppLocaleContext.kt`：统一处理页面级语言上下文包装与即时刷新
- `ui/common/ActivityTransitions.kt`：统一封装页面切换和关闭转场
- `ui/map/MapActivity.kt`：地图页入口，负责组合控制器、列表展示、登录态刷新和生命周期分发
- `ui/map/MapPassDialogController.kt`：关卡确认/重试弹窗、胜负返回地图后的弹窗消费和关卡跳转入口
- `ui/map/MapSettingsController.kt`：设置弹窗、音乐音效开关、玩法页入口和语言切换
- `ui/map/MapNotificationNavigator.kt`：进入棋盘前的通知权限申请、厂商通知预热和页面跳转
- `ui/map/MapPathOverlayView.kt`、`ui/map/PassListAdapter.kt`：地图行内路径虚线绘制、星星状态和滚动定位辅助
- `ui/play/PlayActivity.kt`：棋盘页入口，负责组合控制器和生命周期分发
- `ui/play/PlayRoute.kt`、`ui/map/MapRoute.kt`：集中管理页面跳转 Intent 与 Extra Key
- `ui/play/PlayBoardRules.kt`：纯棋盘规则，包括题面创建、选中高亮、填数冲突、完成判断和错误回滚
- `ui/play/PlayBoardController.kt`：棋盘初始化、棋盘数据观察和格子触摸响应
- `ui/play/PlayInputController.kt`：数字输入、候选标记和撤销交互
- `ui/play/PlayGameStateController.kt`：倒计时 UI、胜负状态、错误输入状态和胜利动画
- `ui/play/PlayDialogController.kt`：暂停、胜利、失败弹窗及弹窗内导航
- `ui/play/PlayNavigationController.kt`：暂停按钮、返回键和页面恢复/暂停协调
- `ui/play/CountdownCoordinator.kt`：倒计时前台服务启动、停止和广播接收
- `ui/play/GameResultRecordGate.kt`、`ui/play/GameResultRecorder.kt`：终局战绩去重闸门与 Provider 落库校验
- `timer/CountdownTimerService.kt`、`timer/CountdownTimerContract.kt`：前台倒计时 service 与广播/通知协议
- `media/AppForegroundBgmController.kt`、`media/BgmMusicService.kt`、`media/BgmMusicController.kt`：应用前后台背景音乐 service 生命周期管理
- `media/PlayMusic.kt`：页面短音效、提示音与流编号管理
- `notification/NotificationPermissionPolicy.kt`：通知权限与厂商预热策略判定
- `account/LauncherSessionReader.kt`、`account/LauncherSessionContract.kt`：茶苑登录态读取与共享契约适配
- `data/provider/GameResultProvider.kt`、`data/provider/GameResultContract.kt`：战绩共享 Provider 与跨应用契约
- `data/repository/MapRepository.kt`、`data/repository/PlayRepository.kt`、`data/repository/UserProgressRepository.kt`：地图、棋盘、用户进度仓储
- `data/database/DatabaseInitializer.kt`：Room 初始化与数据库迁移
- `shared-contracts/`：与星空茶苑共用的跨应用契约源码，避免 Provider 字段和 URI 两边漂移
- `scripts/monkey_run.sh`、`scripts/issue/`：真机 monkey 压测脚本与问题记录文档

## 跨应用契约

2.0 起，跨应用常量集中在 `shared-contracts/src/main/java/com/bird/starrysky/contracts/` 下维护。星空茶苑通过相邻目录引用这份源码，避免两端分别复制 Provider 字段、URI 和权限字符串。

星空数独会读取星空茶苑提供的登录态：

- Authority：`com.bird.starryskyteahouse.provider`
- URI：`content://com.bird.starryskyteahouse.provider/session`
- 权限：`com.bird.starryskyteahouse.permission.READ_SESSION`
- 字段：`username`、`logged_in`

星空数独对外提供战绩数据：

- Authority：`com.bird.starryskysudoku.provider`
- URI：`content://com.bird.starryskysudoku.provider/results`
- 读取权限：`com.bird.starryskysudoku.permission.READ_RESULTS`
- 写入权限：`com.bird.starryskysudoku.permission.WRITE_RESULTS`
- 常用筛选：`username=?`
- 默认排序：`created_at DESC`

## 与星空茶苑一起开发

如果需要同时构建星空茶苑，请保持两个仓库为同级目录：

```text
projects/
  StarrySkySudoku/
  StarrySkyTeaHouse/
```

星空茶苑会引用 `../StarrySkySudoku/shared-contracts/` 中的共享契约，并可在 `StarrySkyTeaHouse` 目录下执行 `./gradlew syncBundledSudokuApk`，重新内置当前星空数独 Debug APK。

## 构建
项目使用 Gradle Wrapper 构建：

```bash
./gradlew assembleDebug
```

运行本地单元测试：

```bash
./gradlew test
```

如需运行 lint：

```bash
./gradlew lintDebug
```

如需分别校验主工程和单元测试 Kotlin 编译：

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:compileDebugUnitTestKotlin
```

## 版本记录
- 2.0：拆分地图页关卡弹窗、设置弹窗和通知权限导航职责；新增 `shared-contracts` 共享跨应用契约；同步茶苑内置 APK 构建说明与 Provider 契约维护方式。
- 1.5 维护更新：拆分棋盘页路由、输入、棋盘、倒计时、弹窗、导航和战绩记录职责；抽出 `PlayBoardRules` 并补充规则测试；同步茶苑小写包名后的登录 Provider 契约。
- 1.5：新增倒计时前台通知，通知内容包含图标、关卡名和剩余时间；进入棋盘前处理通知权限，避免权限弹窗打断棋盘并触发暂停，同时兼容华为 Android 12 厂商通知授权。
- 1.4：补全 Android 四大组件业务闭环；新增 Service 倒计时广播、Activity 动态 Receiver、Room + ContentProvider 战绩共享，以及对应契约单元测试。
- 1.3：重构导航界面，统一棋盘绘制与教程高亮坐标计算，并移除不再使用的旧 onboarding 图片素材。
- 1.2：调整导航界面，使用真实棋盘展示新手教程，并加入遮罩、高亮和第 1 关题面演示。
- 1.1：迁移废弃 API，现代化 Activity 转场、返回处理、应用内语言切换和 Kotlin Gradle 配置。
- 1.0：完成星空主题数独基础玩法、关卡地图、音效和中英文切换。

## 作者
SmashDreams

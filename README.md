# 星空数独 (StarrySkySudoku)

一款星空主题的数独游戏 Android 应用，包含 40 个关卡（从易到难）。

## 当前版本
- 版本：1.5
- 更新内容：新增游戏倒计时前台通知，通知栏展示关卡名和剩余时间；进入棋盘前完成通知权限处理，并兼容华为 Android 12 的厂商通知授权体验。本次维护同步完成玩法页面结构拆分、跨应用登录/战绩契约整理，以及可复用棋盘规则测试。

## 功能
- 标准数独玩法：9×9 棋盘，行列宫内无重复
- 标记模式：空格内可标记候选数字（1-9均可）
- 撤销功能：支持数字和标记撤销（上限 20 条）
- 倒计时：每关 10 分钟，由后台 Service 每秒广播剩余时间
- 前台通知：游戏中通过前台 Service 展示关卡名和剩余时间
- 通知权限前置：进入棋盘前处理 Android 13+ 通知权限，并预热华为 Android 12 厂商通知授权
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

## 项目结构

- `ui/play/PlayActivity.kt`：棋盘页入口，负责组合控制器和生命周期分发
- `ui/play/PlayRoute.kt`、`ui/map/MapRoute.kt`：集中管理页面跳转 Intent 与 Extra Key
- `ui/play/PlayBoardRules.kt`：纯棋盘规则，包括题面创建、选中高亮、填数冲突、完成判断和错误回滚
- `ui/play/PlayBoardController.kt`：棋盘初始化、棋盘数据观察和格子触摸响应
- `ui/play/PlayInputController.kt`：数字输入、候选标记和撤销交互
- `ui/play/PlayGameStateController.kt`：倒计时 UI、胜负状态、错误输入状态和胜利动画
- `ui/play/PlayDialogController.kt`：暂停、胜利、失败弹窗及弹窗内导航
- `ui/play/PlayNavigationController.kt`：暂停按钮、返回键和页面恢复/暂停协调
- `ui/play/CountdownCoordinator.kt`：倒计时前台服务启动、停止和广播接收
- `ui/play/GameResultRecorder.kt`：通过 Provider 保存并校验游戏战绩
- `ui/play/PlayViewModelFactory.kt`、`ui/map/MapViewModelFactory.kt`：统一 ViewModel 创建逻辑

## 跨应用契约

星空数独会读取星空茶苑提供的登录态：

- Authority：`com.bird.starryskyteahouse.provider`
- URI：`content://com.bird.starryskyteahouse.provider/session`
- 权限：`com.bird.starryskyteahouse.permission.READ_SESSION`

星空数独对外提供战绩数据：

- Authority：`com.bird.starryskysudoku.provider`
- URI：`content://com.bird.starryskysudoku.provider/results`
- 权限：`com.bird.starryskysudoku.permission.READ_RESULTS`

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

## 版本记录
- 1.5 维护更新：拆分棋盘页路由、输入、棋盘、倒计时、弹窗、导航和战绩记录职责；抽出 `PlayBoardRules` 并补充规则测试；同步茶苑小写包名后的登录 Provider 契约。
- 1.5：新增倒计时前台通知，通知内容包含图标、关卡名和剩余时间；进入棋盘前处理通知权限，避免权限弹窗打断棋盘并触发暂停，同时兼容华为 Android 12 厂商通知授权。
- 1.4：补全 Android 四大组件业务闭环；新增 Service 倒计时广播、Activity 动态 Receiver、Room + ContentProvider 战绩共享，以及对应契约单元测试。
- 1.3：重构导航界面，统一棋盘绘制与教程高亮坐标计算，并移除不再使用的旧 onboarding 图片素材。
- 1.2：调整导航界面，使用真实棋盘展示新手教程，并加入遮罩、高亮和第 1 关题面演示。
- 1.1：迁移废弃 API，现代化 Activity 转场、返回处理、应用内语言切换和 Kotlin Gradle 配置。
- 1.0：完成星空主题数独基础玩法、关卡地图、音效和中英文切换。

## 作者
SmashDreams

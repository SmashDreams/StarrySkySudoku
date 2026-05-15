# StarrySkySudoku 代码审阅报告

审阅日期：2026-05-15

## 审阅范围

- 主要审阅目录：`StarrySkySudoku/`
- 需求参考：`星空数独开发资源/星空数独开发.pptx`
- 按要求未阅读 `sudoku/` 文件夹下内容。
- 本次只做审阅与建议，未修改业务代码。

## 需求要点摘录

- 9x9 标准数独：每行、每列、每个 3x3 宫内数字 1-9 不重复。
- 玩家输入数字时，如当前行、列、宫内有重复则不放行；无重复则放行。
- 支持输入、覆盖、同数字清除。
- 标记模式只用于空格；标记不做正确性检查。
- 每关倒计时 10 分钟，可暂停。
- 历史记录包含数字与标记，最多 20 条，可撤销。
- 一共 40 关；只能玩已通关关卡和“下一关”，不能玩更后面的关卡。
- 游戏过程中返回星空时，该关卡数据和历史记录需要清空。
- 支持背景音乐和音效开关。

## 总体结论

项目整体没有发现网络传输、明文账号密码、动态代码加载等高危安全问题；主要风险集中在 Android 本地数据保护、生命周期资源释放、游戏状态隔离、输入边界校验和自定义 View/RecyclerView 的可靠性。若作为课程或演示项目，当前功能框架基本完整；若作为正式发布版本，建议优先修复下列 P0/P1 问题。

## 主要问题

### P0 - 游戏历史记录未按关卡和会话隔离，可能跨关卡撤销

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/data/entity/HistoryEntity.kt:7`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/data/dao/HistoryDao.kt:8`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:344`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:445`
- 问题：`history` 表没有 `pass_num` 或会话 ID，`getLatest()` 直接取全表最新记录；进入新关卡、失败重试、胜利进入下一关时没有统一清空历史。只有暂停返回星空时清空。
- 影响：玩家在新关卡点击撤销时，可能撤销上一个关卡的坐标和值，导致棋盘被错误修改；历史还会跨 App 重启保留，和“返回星空清空该关卡数据/历史记录”的需求不一致。
- 建议：为历史记录增加 `pass_num`/`game_session_id` 字段，所有查询和清理按当前关卡过滤；在进入关卡时初始化或清空当前关卡历史；在胜利、失败、重开、返回地图等路径明确清理或关闭当前会话。

### P0 - 音频单例在 Activity 生命周期中释放，后续使用可能失效或崩溃

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/splash/SplashActivity.kt:57`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/splash/SplashActivity.kt:70`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/media/PlayMusic.kt:163`
- 问题：`PlayMusic` 是全局单例，但在 `SplashActivity.onDestroy()` 中调用 `release()`；释放后实例仍保留，后续页面继续调用 `playBGM()`、`playSound()` 时可能访问已释放的 `MediaPlayer/SoundPool`。
- 影响：后台回收、语言切换、返回栈销毁等场景下可能出现背景音乐/音效失效、`IllegalStateException`、静默失败或资源状态不一致。
- 建议：将音频初始化和释放放到 `Application` 级生命周期，或在 `release()` 后将单例状态重置为未初始化；每个播放入口检查初始化状态；避免由单个 Activity 销毁全局音频资源。

### P1 - Android 本地数据保护策略偏宽

- 位置：`StarrySkySudoku/app/src/main/AndroidManifest.xml:4`
- 位置：`StarrySkySudoku/app/src/main/AndroidManifest.xml:5`
- 位置：`StarrySkySudoku/app/src/main/AndroidManifest.xml:8`
- 位置：`StarrySkySudoku/app/src/main/res/xml/backup_rules.xml:8`
- 位置：`StarrySkySudoku/app/src/main/res/xml/data_extraction_rules.xml:6`
- 问题：声明了 `READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`，但当前代码未看到外部存储读写需求；同时 `allowBackup="true"`，备份规则仍是模板配置，未明确排除数据库和偏好设置。
- 影响：多余权限会增加审核和用户信任成本；备份/迁移可能带走 `sudoku.db`、通关记录、音频设置、语言设置等本地数据。游戏数据敏感度不高，但仍属于不必要的数据暴露面。
- 建议：删除未使用的外部存储权限；如无跨设备恢复需求，设置 `android:allowBackup="false"`；如需要保留备份，则明确 include/exclude，仅备份确实需要迁移的数据。

### P1 - 关卡编号和 Intent 参数缺少边界校验

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:63`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:105`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:95`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:295`
- 问题：`num`、`next`、`lose`、`roll` 等参数多处直接 `toInt()` 或传入数据库查询，未限制必须在 1..40。
- 影响：虽然相关 Activity 默认不导出，外部直接利用面有限，但内部异常参数、恢复旧 Intent、测试或后续新增入口时会造成崩溃或进入不存在关卡；无效关卡会生成空棋盘，影响游戏完整性。
- 建议：统一提供 `parseLevel(extra): Int?`，只接受 1..40；无效值返回地图或默认第一关；数据库读取到非 81 个值时应阻断并给出错误态。

### P1 - 地图页 onCreate/onResume 重复处理胜负参数，可能重复弹窗

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:74`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:81`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:303`
- 问题：`onCreate()` 中 `initMapData()` 已处理 `next`/`lose` 并延迟弹窗；Activity 随后进入 `onResume()` 又读取同一 Intent 并再次处理，然后才移除 extra。
- 影响：胜利/失败回到地图时可能出现重复弹窗、重复动画、重复音效或重复滚动。
- 建议：只在一个生命周期入口消费一次导航事件；读取后立即 `removeExtra()`，或使用 `savedInstanceState`/一次性事件对象防重复消费。

### P1 - 返回星空以外的路径没有清理关卡数据/历史

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:434`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:487`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:494`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:505`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:513`
- 问题：暂停返回星空会 `clearHistory()`，但重开、失败返回、失败重试、胜利关闭、胜利下一关等路径没有相同清理。
- 影响：与“返回星空清空该关卡数据/历史记录”需求不一致，也会放大跨关卡撤销问题。
- 建议：抽取 `finishCurrentGame(clearHistory = true)`，所有离开当前关卡的路径统一调用；如果需要保留未完成进度，则应显式实现保存/恢复，不应半保留历史。

### P1 - 自定义棋盘触摸边界不严谨

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/BroadView.kt:224`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/BroadView.kt:239`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/BroadView.kt:241`
- 问题：点击棋盘外部时，`mRow/mCol/mBigBlock` 会保留上一次值或默认 0，仍然触发 `onTouch()`；处理完触摸后返回 `super.onTouchEvent(event)`，不保证事件被消费。
- 影响：玩家点到边缘或空白区域可能误选单元格，造成输入目标错误；不同设备触摸行为可能不一致。
- 建议：触摸时先计算命中的 row/col，未命中直接返回 `false` 或不回调；命中后返回 `true`；坐标计算使用棋盘实际矩形和 `min(width,height)`。

### P1 - 地图列表 ViewHolder 状态未完整复位，存在复用隐患

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt:60`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt:92`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt:153`
- 问题：`onBindViewHolder()` 的锁定关卡分支没有清理 `OnClickListener`，也没有统一重置 alpha、visibility、动画和文本颜色；`getItemViewType(position)=position` 实际上规避了部分复用，但牺牲了 RecyclerView 的复用机制。
- 影响：后续数据刷新、布局复用或增加关卡数后，可能出现锁定关卡仍响应点击、动画残留、显示状态错乱。
- 建议：在绑定每个星星前先执行统一 reset；锁定状态显式 `setOnClickListener(null)`；不要用 position 作为 viewType，除非确实有不同布局类型。

### P1 - 延迟任务未在销毁时取消

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:32`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:192`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:226`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:107`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt:74`
- 问题：多个 `Handler.postDelayed` 捕获 Activity、Dialog、ViewHolder，但 `onDestroy()` 没有统一 `removeCallbacksAndMessages(null)`。
- 影响：页面关闭后延迟回调仍可能执行，引发内存泄漏、已销毁 Activity 上弹窗、动画访问旧 View 等问题。
- 建议：Activity 持有的 Handler 在 `onDestroy()` 清理；适合生命周期的延迟逻辑改用 `lifecycleScope.launch { delay(...) }` 并依赖生命周期取消；Adapter 中避免持有 Dialog 和 Handler。

### P2 - 标记按钮可在非空玩家格上切入标记模式

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:257`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:262`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:315`
- 问题：需求要求“仅可在空格内标记”。当前点击玩家已填数字格时，代码会先把 `tagMode` 改为 true，再因 `cell.value != "0"` 提前返回；随后点数字会走标记模式并报错。
- 影响：UI 状态和实际可操作状态不一致，容易误导玩家。
- 建议：标记按钮点击入口同时校验“已选中 + 非题目格 + 当前值为 0”；不满足时不要改变图标和 `tagMode`。

### P2 - ViewModel 暴露可变棋盘数组，状态边界模糊

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayViewModel.kt:35`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:203`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:373`
- 问题：`LiveData<Array<Array<CellData>>>` 中的数组和 `CellData` 均可被 Activity 直接修改；ViewModel 也直接复用同一数组实例。
- 影响：状态变化难以追踪，后续添加测试、旋转屏幕恢复或并发协程时容易出现 UI 和数据不一致。
- 建议：棋盘状态尽量不可变，更新时 copy 新对象；将“选择、输入、撤销、胜负判定”都收敛到 ViewModel，Activity 只观察和渲染。

### P2 - 数独数据和输入缺少防御式校验

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayViewModel.kt:57`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayViewModel.kt:123`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/TagData.kt:6`
- 问题：初始题目不足 81 个值时用 0 补齐；输入数字虽然 UI 限定 1..9，但 ViewModel 层没有统一校验 row/col/number。
- 影响：数据库损坏或内部调用错误时，可能出现异常棋盘、越界崩溃或错误通关。
- 建议：加载题目时检查数量、取值范围和初始盘无冲突；ViewModel 公共方法校验坐标 0..8、数字 1..9。

### P2 - 音效播放实现资源开销偏高

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/media/PlayMusic.kt:87`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/media/PlayMusic.kt:115`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:158`
- 问题：按钮音效已加载进 `SoundPool`，但 `playButtonTap()` 每次点击都新建 `MediaPlayer`；倒计时最后 10 秒每秒调用 `playTimesUp()`，可能重复启动同一音效。
- 影响：频繁点击时对象创建和释放压力较大；低端设备上可能出现延迟、音频重叠或资源占用。
- 建议：短音效统一使用 `SoundPool`；倒计时提示音明确设计为“一次性”或“每秒滴答”，并为每种行为设置独立控制逻辑。

### P2 - Release 构建未开启压缩混淆

- 位置：`StarrySkySudoku/app/build.gradle:22`
- 位置：`StarrySkySudoku/app/build.gradle:23`
- 问题：`release` 构建 `minifyEnabled false`。
- 影响：包体更大，代码更容易被反编译；对单机游戏不是高危，但正式发布通常建议开启。
- 建议：正式发布时开启 R8：`minifyEnabled true`、`shrinkResources true`，并补齐必要 keep 规则。

### P2 - Room 数据库迁移策略不利于后续升级

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/data/database/AppDatabase.kt:12`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/data/database/AppDatabase.kt:15`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/data/database/DatabaseInitializer.kt:18`
- 问题：`exportSchema=false`，当前版本没有 Migration；预置数据库中同时放了题目、地图状态和临时历史。
- 影响：后续字段变更或数据表拆分时难以可靠升级；临时历史随数据库备份/恢复一起迁移，职责混杂。
- 建议：开启 schema 导出；题库数据、进度数据、临时历史分层管理；版本升级时提供 Migration 或明确破坏性迁移策略。

### P3 - 代码结构和可维护性问题

- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt:74`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt:63`
- 位置：`StarrySkySudoku/app/src/main/java/com/bird/starryskysudoku/ui/dialog/MyDialog.kt:10`
- 位置：`StarrySkySudoku/app/src/test/java/com/bird/starryskysudoku/ExampleUnitTest.java:13`
- 问题：偏好设置 key、状态字符串、最大关卡数、倒计时秒数等散落在多个类；`MyDialog` 的 `gravity` 参数未使用；单元测试仍是模板测试。
- 影响：后续改需求容易漏改；测试无法覆盖核心逻辑。
- 建议：抽取 `GameConstants`、`PreferenceKeys`、`PassStatus`；删除无用参数和无用代码；为数独判重、撤销、计时、关卡解锁添加单元测试。

## 优先修复路线

1. 先修 P0/P1：历史记录按关卡隔离、音频生命周期、Intent 关卡校验、重复弹窗、离开关卡清理逻辑。
2. 再补安全配置：删除外部存储权限，明确备份策略，正式包开启 R8。
3. 然后做可靠性优化：棋盘触摸边界、Handler 清理、RecyclerView reset、ViewModel 状态不可变。
4. 最后补测试：至少覆盖输入/清除/标记/撤销/胜利判定/关卡解锁/倒计时失败。

## 建议补充的测试用例

- 输入同一数字清除后撤销，应恢复原数字。
- 不同关卡之间撤销互不影响。
- 返回星空、失败重试、胜利下一关后历史为空或只属于新关卡。
- 关卡参数为 0、41、非数字时不崩溃。
- 初始题目不是 81 个数字或存在冲突时不进入正常游戏。
- 锁定关卡不可点击，已通关关卡可重玩，下一关可进入。
- 倒计时到 0 只触发一次失败弹窗。
- 点击棋盘外部不会改变当前选中格。

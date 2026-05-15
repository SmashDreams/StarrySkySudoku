# StarrySkySudoku 重构设计文档

## 一、概述

将已有 Android 数独游戏项目进行以下改造：
1. 清理 Git 仓库，重置作者身份为 SmashDreams
2. 从 `关卡.et` 替换关卡数据
3. Java → Kotlin 语言迁移，引入 ViewModel + Room 架构
4. 代码审阅并对照 PPTX 需求修复缺陷

## 二、Git 仓库重建

| 操作 | 说明 |
|------|------|
| 删除 `.git/` | 清除全部历史 commit（原作者：Vernon Wei / weizhongbang） |
| 删除 `Thumbs.db` | Windows 缩略图缓存，非功能性文件 |
| 删除 `README.md` | 完成后重新生成 |
| 删除 `app/release/` | 清除旧签名 APK |
| 清理 `.idea/` 个人配置 | misc.xml 含用户路径，保留 gradle.xml 等共享配置 |
| `git init` | 新仓库初始 commit，作者 `SmashDreams <smashdreams@example.com>` |
| `.gitignore` | 补充条目：`*.db`、`app/release/`、`Thumbs.db`、`~*` |

## 三、关卡数据更新

源文件：`关卡.et`（40关完整数据，格式：第1列关号、第2列81位数字串）

与现有 `sudoku.db` 差异（仅3关）：

| 关卡 | 行 | 旧值 | 新值 |
|------|-----|------|------|
| 19 | Row 8 | 002017900 | 002017300 |
| 25 | Row 3 | 005903100 | 005303100 |
| 26 | Row 0 | 230960001 | 230930001 |

操作：直接 SQL UPDATE 修改 problem 表中对应数据，其余37关不变。

## 四、技术栈升级

### 4.1 语言：Java 8 → Kotlin

17个 Java 文件全部转换为 Kotlin。

### 4.2 架构改进

| 模块 | 当前 | 升级后 |
|------|------|--------|
| 数据库 | 原始 SQLiteOpenHelper + 手动 SQL | Room (保留 assets 预置 db 拷贝模式) |
| 状态管理 | Activity 内手动管理字段和 Handler | ViewModel + LiveData |
| 音效 | `new SoundPool(1, *, 0)` (deprecated) | `SoundPool.Builder()` |
| SharedPrefs | `commit()` 同步写 | `apply()` 异步写 |
| 定时器 | CountDownTimer | kotlinx.coroutines delay loop |

### 4.3 包结构

```
com.bird.starryskysudoku/
├── StarrySkySudokuApp.kt
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   └── DatabaseInitializer.kt
│   ├── dao/
│   │   ├── ProblemDao.kt
│   │   ├── MapDao.kt
│   │   └── HistoryDao.kt
│   └── entity/
│       ├── ProblemEntity.kt
│       ├── MapEntity.kt
│       └── HistoryEntity.kt
├── ui/
│   ├── splash/
│   │   └── SplashActivity.kt
│   ├── guide/
│   │   └── GuideActivity.kt
│   ├── howtoplay/
│   │   └── HowToPlayActivity.kt
│   ├── map/
│   │   ├── MapActivity.kt
│   │   ├── MapViewModel.kt
│   │   └── PassListAdapter.kt
│   ├── play/
│   │   ├── PlayActivity.kt
│   │   ├── PlayViewModel.kt
│   │   ├── BroadView.kt
│   │   └── TagData.kt
│   └── dialog/
│       ├── MyDialog.kt
│       └── MyDialogManager.kt
└── media/
    └── PlayMusic.kt
```

### 4.4 依赖版本更新

在 `libs.versions.toml` 中更新到最新稳定版：
- AGP, Kotlin, Room, Lifecycle (ViewModel/LiveData), AppCompat, Material

## 五、代码审阅清单（对照 PPTX 需求）

| # | 需求 | 状态 | 操作 |
|---|------|------|------|
| 1 | 数独验证（行列宫无重复） | 已实现 | 保留 |
| 2 | 数字输入/覆盖/清除 | 已实现 | 保留 |
| 3 | 标记模式（空格标记1-9） | 待验证 | TagData 支持9个标记，需确认 |
| 4 | 计时器10min倒计时 | 已实现 | 改用协程 |
| 5 | 视觉区分（预填/玩家填/选中/高亮/冲突） | 已实现 | 保留 BroadView 逻辑 |
| 6 | BGM/音效开关 | 已实现 | 修复 SoundPool 废弃 API |
| 7 | 闪屏页 | 已实现 | 保留 |
| 8 | 关卡地图（滚动、星空背景） | 已实现 | 保留 |
| 9 | 设置对话框 | 已实现 | 保留 |
| 10 | 过关对话框动效 | 已实现 | 保留 |
| 11 | 历史记录上限20条 | 待验证 | HistoryDao 需检查 LIMIT 逻辑 |
| 12 | 不能玩未解锁关卡 | 待验证 | PassListAdapter 点击过滤 |
| 13 | 撤销上限后 disable | 待验证 | Revoke 按钮状态联动 |
| 14 | 暂停后返回清除本关数据 | 待验证 | onPause/onDestroy 逻辑 |
| 15 | 倒计时格式 09:09 | 已修复 | 代码中已使用 %02d |
| 16 | 中文/英文切换 | 已实现 | 保留 |
| 17 | 界面跳转流程 | 已实现 | 保留 |

## 六、不修改的部分

- XML 布局文件
- 资源文件（图片、音效、动画）
- 应用包名 `com.bird.starryskysudoku`
- 应用名 "星空数独"
- assets/sudoku.db 预置数据库（仅更新数据，结构不变）
- AndroidManifest.xml（仅修正 namespace 引用）

## 七、实施顺序

1. 清理工作（删除 .git、Thumbs.db、旧 APK、README.md）
2. 更新关卡数据（sudoku.db 中修改3关）
3. Kotlin 转换 + Room + ViewModel（按模块：data → media → ui）
4. 代码审阅修复（对照清单逐项验证）
5. 验证构建通过
6. 重新 git init + 初始 commit（作者 SmashDreams）
7. 生成 README.md

## 八、验证标准

- `./gradlew assembleDebug` 编译通过
- 所有40关数据与 `关卡.et` 一致
- 应用可正常启动5个页面
- 数独输入、验证、标记、撤销功能正常
- BGM/音效播放正常
- 中英文切换正常

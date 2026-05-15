# 星空数独 (StarrySkySudoku)

一款星空主题的数独游戏 Android 应用，包含 40 个关卡（从易到难）。

## 当前版本
- 版本：1.1
- 更新内容：修正 Android/Kotlin 中已废弃或旧式 API 的调用，更新语言切换、页面转场、返回事件处理和 Gradle 配置写法。

## 功能
- 标准数独玩法：9×9 棋盘，行列宫内无重复
- 标记模式：空格内可标记候选数字（1-9均可）
- 撤销功能：支持数字和标记撤销（上限 20 条）
- 倒计时：每关 10 分钟
- 关卡地图：星空主题滚动地图，已通关关卡点亮星星
- 背景音乐/音效：独立开关
- 中英文切换

## 技术栈
- Kotlin
- Room 数据库
- ViewModel + LiveData
- SoundPool (Builder API)
- AndroidX AppCompat / Activity KTX
- Gradle Version Catalog

## 构建
项目使用 Gradle Wrapper 构建：

```bash
./gradlew assembleDebug
```

如需运行 lint：

```bash
./gradlew lintDebug
```

## 版本记录
- 1.1：迁移废弃 API，现代化 Activity 转场、返回处理、应用内语言切换和 Kotlin Gradle 配置。
- 1.0：完成星空主题数独基础玩法、关卡地图、音效和中英文切换。

## 作者
SmashDreams

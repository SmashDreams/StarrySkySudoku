# 星空数独 (StarrySkySudoku)

一款星空主题的数独游戏 Android 应用，包含 40 个关卡（从易到难）。

## 功能
- 标准数独玩法：9×9棋盘，行列宫内无重复
- 标记模式：空格内可标记候选数字（1-9均可）
- 撤销功能：支持数字和标记撤销（上限20条）
- 倒计时：每关10分钟
- 关卡地图：星空主题滚动地图，已通关关卡点亮星星
- 背景音乐/音效：独立开关
- 中英文切换

## 技术栈
- Kotlin
- Room 数据库
- ViewModel + LiveData
- SoundPool (Builder API)

## 构建
```bash
./gradlew assembleDebug
```

## 作者
SmashDreams

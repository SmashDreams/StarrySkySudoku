package com.bird.starryskysudoku.ui.guide

// 引导步骤顺序同时决定文案、聚焦框和演示棋盘的切换顺序。
enum class GuideStep {
    WELCOME,
    RULE_UNIQUE,
    SELECT_CELL,
    ENTER_NUMBER,
    TIMER,
    GOOD_LUCK
}

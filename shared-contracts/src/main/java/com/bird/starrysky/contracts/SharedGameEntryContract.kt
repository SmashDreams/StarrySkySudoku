package com.bird.starrysky.contracts

object SharedGameEntryContract {
    /*
     * 游戏包名会同时影响茶苑启动游戏、战绩 Provider authority 和 APK 安装判断，
     * 因此集中在共享契约里维护。
     */
    const val PACKAGE_NAME = "com.bird.starryskysudoku"
}

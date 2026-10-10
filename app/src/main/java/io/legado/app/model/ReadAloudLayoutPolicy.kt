package io.legado.app.model

internal fun shouldRestartReadAloudAfterContentLoad(
    preserveReadAloudPosition: Boolean,
    serviceChapterIndex: Int,
    loadedChapterIndex: Int,
): Boolean = !preserveReadAloudPosition || serviceChapterIndex != loadedChapterIndex

/** 手动翻页/滚动时朗读跟随的处理方式。 */
internal enum class ReadAloudManualTurnAction {
    /** 脱离朗读位置：朗读继续但不驱动可见页面，由悬浮条提供"回到朗读位置"。 */
    DetachFollow,

    /** 保持跟随：朗读从新页面重新开始（旧行为，脱离提示关闭时）。 */
    RestartOnPage,

    /** 允许浏览：朗读不停不重启，页面停在用户位置，朗读推进到下一页时自动跳回。 */
    BrowseThenReturn,
}

/**
 * 手动导航（翻页/滚动/跳章）后朗读跟随怎么走。
 * 新开关优先于旧的脱离提示：两者同时开启时走 [ReadAloudManualTurnAction.BrowseThenReturn]。
 */
internal fun readAloudManualTurnAction(
    keepFollowingOnManualTurn: Boolean,
    detachReminderEnabled: Boolean,
): ReadAloudManualTurnAction = when {
    keepFollowingOnManualTurn -> ReadAloudManualTurnAction.BrowseThenReturn
    detachReminderEnabled -> ReadAloudManualTurnAction.DetachFollow
    else -> ReadAloudManualTurnAction.RestartOnPage
}

internal fun activeReadAloudProgress(
    isPlaying: Boolean,
    currentProgress: Int,
): Int? = currentProgress.takeIf { isPlaying && it > 0 }

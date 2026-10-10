package io.legado.app.ui.main

import io.legado.app.domain.model.PlaybackCapsuleSource
import io.legado.app.domain.model.PlaybackCapsuleState

/** 只有同一播放会话的胶囊才能作为封面飞行起点。不同会话仍使用真实胶囊位置，但封面只淡出交接。 */
internal fun capsuleMatchesPlayer(
    capsule: PlaybackCapsuleState,
    source: PlaybackCapsuleSource,
    bookUrl: String,
): Boolean = capsule.source == source &&
        (source == PlaybackCapsuleSource.ReadAloud || bookUrl.isBlank() || capsule.bookUrl == bookUrl)

/** 与主页底栏的实际组合条件一致，隐藏底栏时由全局胶囊提供锚点。 */
internal fun shouldUseHomePlaybackCapsule(
    onMainRoute: Boolean,
    showBottomView: Boolean,
    useFloatingBottomBar: Boolean,
    useRail: Boolean,
): Boolean = onMainRoute && showBottomView && useFloatingBottomBar && !useRail

/**
 * 宿主兜底处理返回的条件：关闭预测性返回且没有播放浮层时，**任何深度都保持启用**。
 *
 * 必须常驻，不能只看根页面：AndroidX 只在「存在启用的返回处理器」时才向系统注册
 * OnBackInvokedCallback。子页面刚压入时 nav3 的 NavigationBackHandler 还没随场景启用
 * （`previousEntries` 仍是旧的空列表），一旦这个空窗里没有任何启用的处理器，系统就会把
 * 本次返回当成「应用不处理」，于是显示预测性返回手势（back-to-home），甚至可能落到
 * Activity 默认行为退出应用。常驻一个兜底处理器既堵住空窗，也在导航层没接住时把栈弹一层。
 *
 * 优先级靠注册顺序而不是条件判断：它在组合里注册在导航宿主之前，AndroidX 的回调按
 * 「后注册先调用」派发，所以页面/弹层/覆盖层自己的处理器永远先拿到返回。播放浮层
 * 自己带预测性返回动画，开启时让位（[playerPresent]）。
 */
internal fun shouldHandleActivityBack(
    predictiveBackEnabled: Boolean,
    playerPresent: Boolean,
): Boolean =
    !predictiveBackEnabled && !playerPresent

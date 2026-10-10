package io.legado.app.ui.main

import io.legado.app.domain.model.PlaybackCapsuleSource
import io.legado.app.domain.model.PlaybackCapsuleState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPlayerAnchorsTest {
    private val audio = PlaybackCapsuleState(
        source = PlaybackCapsuleSource.AudioBook,
        bookUrl = "audio-a",
    )

    @Test
    fun currentAudiobookUsesItsCapsule() {
        assertTrue(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, "audio-a"))
    }

    @Test
    fun differentAudiobookDoesNotFlyThePreviousCover() {
        assertFalse(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, "audio-b"))
    }

    @Test
    fun notificationWithoutBookUrlUsesCurrentAudioSession() {
        assertTrue(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, ""))
    }

    @Test
    fun readAloudCapsuleCannotAnimateIntoAudioPlayer() {
        val readAloud = audio.copy(source = PlaybackCapsuleSource.ReadAloud)
        assertFalse(capsuleMatchesPlayer(readAloud, PlaybackCapsuleSource.AudioBook, "audio-a"))
        assertFalse(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.ReadAloud, ""))
    }

    @Test
    fun stoppedSessionUsesFallbackRatherThanStaleAnchors() {
        assertFalse(
            capsuleMatchesPlayer(
                audio.copy(source = null),
                PlaybackCapsuleSource.AudioBook,
                "audio-a"
            )
        )
    }

    @Test
    fun hiddenBottomBarAlwaysUsesGlobalCapsuleEvenIfFloatingBarIsEnabled() {
        assertFalse(shouldUseHomePlaybackCapsule(true, false, true, false))
        assertTrue(shouldUseHomePlaybackCapsule(true, true, true, false))
    }

    @Test
    fun otherRoutesRailAndStandardBottomBarUseGlobalCapsule() {
        assertFalse(shouldUseHomePlaybackCapsule(false, true, true, false))
        assertFalse(shouldUseHomePlaybackCapsule(true, true, true, true))
        assertFalse(shouldUseHomePlaybackCapsule(true, true, false, false))
    }

    @Test
    fun backFallbackStaysEnabledOnSubPagesWhilePredictiveBackIsOff() {
        // 关闭预测性返回时任何深度都常驻：AndroidX 只在「存在启用的返回处理器」时才向系统
        // 注册 OnBackInvokedCallback，子页面刚压入、nav3 处理器尚未启用的空窗里若没有任何
        // 启用的处理器，系统会当成应用不处理返回而显示预测性返回手势。
        assertTrue(shouldHandleActivityBack(predictiveBackEnabled = false, playerPresent = false))
        assertFalse(shouldHandleActivityBack(predictiveBackEnabled = true, playerPresent = false))
        assertFalse(shouldHandleActivityBack(predictiveBackEnabled = false, playerPresent = true))
        assertFalse(shouldHandleActivityBack(predictiveBackEnabled = true, playerPresent = true))
    }
}

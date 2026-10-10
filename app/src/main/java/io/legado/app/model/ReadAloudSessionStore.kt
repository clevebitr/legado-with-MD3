package io.legado.app.model

import io.legado.app.domain.model.readaloud.ReadAloudPlaybackInfo
import io.legado.app.domain.model.readaloud.ReadAloudSessionState
import io.legado.app.domain.model.readaloud.ReadAloudSessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-local source of truth for the active read-aloud session. */
class ReadAloudSessionStore {

    private val _state = MutableStateFlow(ReadAloudSessionState())
    val state = _state.asStateFlow()

    fun setStatus(status: ReadAloudSessionStatus) {
        _state.update { it.copy(status = status) }
    }

    fun updatePlayback(playback: ReadAloudPlaybackInfo) {
        _state.update { it.copy(playback = playback) }
    }

    fun updateTimer(minutes: Int) {
        _state.update { it.copy(timerMinutes = minutes) }
    }

    /** 用户手动导航（翻页/跳章/拖动进度）导致页面脱离朗读位置。 */
    fun detachReadAloudFollow() {
        _state.update { it.copy(followReadAloudPosition = false, browsingWhileSpeaking = false) }
    }

    /** 回到朗读位置或新朗读会话开始时恢复跟随。 */
    fun restoreReadAloudFollow() {
        _state.update { it.copy(followReadAloudPosition = true, browsingWhileSpeaking = false) }
    }

    /**
     * 用户手动翻页/滚动：允许浏览但不脱离朗读。朗读继续推进，
     * 页面停在用户位置，等朗读推进到下一页时由服务端 [endManualBrowsing] 并跳回。
     */
    fun startManualBrowsing() {
        _state.update { it.copy(followReadAloudPosition = true, browsingWhileSpeaking = true) }
    }

    /** 朗读推进到新页，结束浏览并恢复跟随朗读位置。 */
    fun endManualBrowsing() {
        _state.update { it.copy(browsingWhileSpeaking = false) }
    }

    fun stop() {
        _state.value = ReadAloudSessionState()
    }
}

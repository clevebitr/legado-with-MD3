package io.legado.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAloudLayoutPolicyTest {

    @Test
    fun sameChapterRelayoutKeepsCurrentPlaybackPosition() {
        assertFalse(
            shouldRestartReadAloudAfterContentLoad(
                preserveReadAloudPosition = true,
                serviceChapterIndex = 4,
                loadedChapterIndex = 4,
            )
        )
    }

    @Test
    fun userPositionChangeRestartsPlayback() {
        assertTrue(
            shouldRestartReadAloudAfterContentLoad(
                preserveReadAloudPosition = false,
                serviceChapterIndex = 4,
                loadedChapterIndex = 4,
            )
        )
    }

    @Test
    fun nextChapterLoadStartsPendingChapter() {
        assertTrue(
            shouldRestartReadAloudAfterContentLoad(
                preserveReadAloudPosition = true,
                serviceChapterIndex = 4,
                loadedChapterIndex = 5,
            )
        )
    }

    @Test
    fun activePlaybackRestoresLatestServiceProgress() {
        assertEquals(
            128,
            activeReadAloudProgress(
                isPlaying = true,
                currentProgress = 128,
            )
        )
    }

    @Test
    fun inactivePlaybackDoesNotRestoreStaleProgress() {
        assertNull(
            activeReadAloudProgress(
                isPlaying = false,
                currentProgress = 128,
            )
        )
    }

    @Test
    fun keepFollowingSwitchWinsOverDetachReminder() {
        assertEquals(
            ReadAloudManualTurnAction.BrowseThenReturn,
            readAloudManualTurnAction(
                keepFollowingOnManualTurn = true,
                detachReminderEnabled = true,
            ),
        )
    }

    @Test
    fun keepFollowingSwitchAloneAllowsBrowsing() {
        assertEquals(
            ReadAloudManualTurnAction.BrowseThenReturn,
            readAloudManualTurnAction(
                keepFollowingOnManualTurn = true,
                detachReminderEnabled = false,
            ),
        )
    }

    @Test
    fun detachReminderKeepsLegacyDetachBehaviour() {
        assertEquals(
            ReadAloudManualTurnAction.DetachFollow,
            readAloudManualTurnAction(
                keepFollowingOnManualTurn = false,
                detachReminderEnabled = true,
            ),
        )
    }

    @Test
    fun bothSwitchesOffRestartPlaybackOnTheNewPage() {
        assertEquals(
            ReadAloudManualTurnAction.RestartOnPage,
            readAloudManualTurnAction(
                keepFollowingOnManualTurn = false,
                detachReminderEnabled = false,
            ),
        )
    }
}

package io.legado.app.core.ui.player

import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.unit.Density
import io.legado.app.data.repository.CoverAlbumRepository
import io.legado.app.data.repository.SettingsRepository
import io.legado.app.domain.model.settings.AppShellSettings
import io.legado.app.domain.model.settings.AppUiConfiguration
import io.legado.app.domain.usecase.CoverAlbumUseCase
import io.legado.app.help.config.AppConfigStore
import io.legado.app.ui.book.readaloud.morph.ReadAloudMorphState
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.components.text.AppText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
// API 34 的独立 sandbox 避免与胶囊触摸测试共用已被重置的 Choreographer。
@Config(application = Application::class, sdk = [34])
class PlayerMorphHostInteractionTest {
    @Test
    fun backClosesTheModalPlayerBeforeTheCoveredRoute() {
        // 宿主兜底返回已收敛到 Activity 级的 PredictiveBackHost（关闭预测性返回时拦截并转发给
        // dispatcher），这里用「路由处理器注册在播放浮层之前」复现真实组合顺序：
        // 播放浮层后注册，所以浮层优先拿到返回。
        verifyBack(predictive = false)
        verifyBack(predictive = true)
        verifyBack(predictive = false, initialProgress = 0f)
    }

    /**
     * 反向顺序：路由处理器后注册就先拿到返回。这不是缺陷而是新契约——宿主不再用 Compose
     * BackHandler 兜底，优先级由 dispatcher 的「后注册先调用」决定，所以播放浮层必须组合在
     * 导航宿主之后。这条用例就是钉住这个顺序要求的。
     */
    @Test
    fun coveredRouteWinsBackWhenItRegistersAfterThePlayerOverlay() {
        val application = RuntimeEnvironment.getApplication()
        AppConfigStore.init(application)
        stopKoin()
        startKoin {
            modules(module {
                single {
                    CoverAlbumUseCase(
                        CoverAlbumRepository(
                            application,
                            SettingsRepository()
                        )
                    )
                }
            })
        }
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val activity = controller.get()
        val visible = mutableStateOf(true)
        val morph = ReadAloudMorphState(Animatable(1f), Density(1f))
        var routeBacks = 0
        var dismissals = 0
        try {
            activity.setContent {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    AppTheme(
                        AppUiConfiguration(
                            appShell = AppShellSettings(predictiveBackEnabled = false),
                        ),
                        applyBackground = false,
                    ) {
                        PlayerMorphHost(
                            appearance = PlayerMorphAppearance("Book", "", null, null, 0),
                            playerTheme = null, morph = morph, visible = visible.value,
                            awaitCapsuleAnchor = true,
                            onDismiss = { dismissals++; visible.value = false },
                        ) { AppText("Player controls") }
                        BackHandler { routeBacks++ }
                    }
                }
            }
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals("Later-registered route handler wins the back", 1, routeBacks)
            assertEquals(0, dismissals)
        } finally {
            controller.pause().stop().destroy()
            stopKoin()
        }
    }

    private fun verifyBack(
        predictive: Boolean,
        initialProgress: Float = 1f
    ) {
        val application = RuntimeEnvironment.getApplication()
        AppConfigStore.init(application)
        stopKoin()
        startKoin {
            modules(module {
                single {
                    CoverAlbumUseCase(
                        CoverAlbumRepository(
                            application,
                            SettingsRepository()
                        )
                    )
                }
            })
        }
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val activity = controller.get()
        var visible by mutableStateOf(true)
        val morph = ReadAloudMorphState(Animatable(initialProgress), Density(1f))
        var routeBacks = 0
        var dismissals = 0
        try {
            activity.setContent {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    AppTheme(
                        AppUiConfiguration(
                            appShell = AppShellSettings(predictiveBackEnabled = predictive),
                        ),
                        applyBackground = false,
                    ) {
                        val present by remember { derivedStateOf { visible || morph.progress.value > 0f } }
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .playerUnderlaySemantics(present)
                            ) { AppText("Bookshelf") }
                            BackHandler { routeBacks++ }
                            PlayerMorphHost(
                                appearance = PlayerMorphAppearance("Book", "", null, null, 0),
                                playerTheme = null, morph = morph, visible = visible,
                                awaitCapsuleAnchor = true,
                                onDismiss = { dismissals++; visible = false },
                            ) { AppText("Player controls") }
                        }
                    }
                }
            }
            fun frames(count: Int) {
                repeat(count) {
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
                    val decor = activity.window.decorView
                    decor.measure(
                        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                    )
                    decor.layout(0, 0, 1080, 1920)
                }
            }
            frames(if (initialProgress == 0f) 180 else 5)
            assertEquals(1f, morph.progress.value)
            assertFalse(visibleTexts(activity.window.decorView).contains("Bookshelf"))
            assertTrue(visibleTexts(activity.window.decorView).contains("Player controls"))
            activity.onBackPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(
                    0f,
                    0f,
                    0f,
                    BackEventCompat.EDGE_LEFT
                )
            )
            activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(
                    0f,
                    0f,
                    0.6f,
                    BackEventCompat.EDGE_LEFT
                )
            )
            frames(5)
            if (predictive) assertTrue(morph.progress.value < 1f) else assertEquals(
                1f,
                morph.progress.value
            )
            activity.onBackPressedDispatcher.dispatchOnBackCancelled()
            frames(90)
            assertEquals(1f, morph.progress.value)
            assertEquals(0, dismissals)
            activity.onBackPressedDispatcher.onBackPressed()
            frames(90)
            assertEquals("The covered route must not receive this back", 0, routeBacks)
            assertEquals(
                "progress=${morph.progress.value} running=${morph.progress.isRunning} visible=$visible",
                1,
                dismissals
            )
            assertFalse(visible)
            assertEquals(0f, morph.progress.value)
            assertTrue(visibleTexts(activity.window.decorView).contains("Bookshelf"))
            assertFalse(visibleTexts(activity.window.decorView).contains("Player controls"))
            activity.onBackPressedDispatcher.onBackPressed()
            assertEquals(1, routeBacks)
        } finally {
            controller.pause().stop().destroy()
            stopKoin()
        }
    }

    private fun visibleTexts(view: View): List<String> {
        if (view is ViewRootForTest) return view.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
            .flatMap {
                it.config.getOrElse(SemanticsProperties.Text) { emptyList() }
                    .map { text -> text.text }
            }
        return if (view is ViewGroup) (0 until view.childCount).flatMap {
            visibleTexts(
                view.getChildAt(
                    it
                )
            )
        }
        else emptyList()
    }
}

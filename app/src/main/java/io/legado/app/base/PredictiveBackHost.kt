package io.legado.app.base

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.legado.app.domain.gateway.AppShellSettingsGateway
import kotlinx.coroutines.launch

/**
 * 关闭预测性返回时是否要常驻拦截返回。API 33 以下没有这套平台回调，无从模拟。
 *
 * 单列成纯函数是为了让"关闭 ⇒ 必须拦截"这条不变量可被测试钉住：它一旦不成立，
 * 系统就会在"没有任何启用的返回处理器"的瞬间把返回当成应用不处理，显示出
 * back-to-home 预测手势（曾经表现为"刚进子页面立即返回仍有动画"）。
 */
internal fun shouldRegisterBackInterception(
    predictiveBackEnabled: Boolean,
    sdkInt: Int,
): Boolean = !predictiveBackEnabled && sdkInt >= Build.VERSION_CODES.TIRAMISU

/**
 * 预测性返回的运行时总开关，全应用唯一实现，由 [BaseActivity]（老 View 页面）与
 * [BaseComposeActivity]（Compose 宿主）共用。
 *
 * 平台只在 manifest 的 `enableOnBackInvokedCallback` 里提供**静态** opt-in，没有运行时 API，
 * 所以"用户关掉预测性返回"只能模拟成"让应用拦住返回，系统就不进预测模式"。收敛在这里之后：
 *
 * - **关闭**：在 Activity 级注册一个**不支持进度**的 [OnBackInvokedCallback]，并放在
 *   [OnBackInvokedDispatcher.PRIORITY_OVERLAY]（高于 AndroidX 输入的 DEFAULT）。它常驻，
 *   因此不存在"某一帧没有任何启用的返回处理器"的空窗——那正是过去"刚进子页面立即返回
 *   仍显示 back-to-home 预测手势"的成因；手势到达后立刻转发给 `onBackPressedDispatcher`，
 *   Nav3 / 弹层 / 覆盖层自己的处理器照常先执行，没人接才走 Activity 默认行为（退出）。
 * - **开启**：什么都不注册，让 AndroidX 的 `OnBackAnimationCallback` 正常拿到进度，
 *   应用内跟手动画（Nav3 预测转场、封面 morph、覆盖层）才有效。
 *
 * 这是模拟而不是平台能力：Android 15+ 对已 opt-in 的应用会显示系统动画，应用侧唯一的杠杆
 * 就是"拦不拦返回"，本类即那唯一杠杆。新增 Activity 请继承上面两个基类之一，不要再各自
 * 注册返回回调。
 */
internal class PredictiveBackHost(
    private val activity: ComponentActivity,
    private val settingsGateway: AppShellSettingsGateway,
) {

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val registration = BackInterceptionRegistration(activity)
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                settingsGateway.settings.collect { settings ->
                    registration.setEnabled(
                        shouldRegisterBackInterception(
                            predictiveBackEnabled = settings.predictiveBackEnabled,
                            sdkInt = Build.VERSION.SDK_INT,
                        )
                    )
                }
            }
        }
    }

    /**
     * API 33+ 的注册句柄独立成类：低版本设备不会加载它，也就不会解析到不存在的平台类型。
     * 类级 @RequiresApi 让 lint 认账（minSdk 26 下直接调用这些平台 API 会报 NewApi）。
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class BackInterceptionRegistration(
        private val activity: ComponentActivity,
    ) {
        private var callback: OnBackInvokedCallback? = null

        fun setEnabled(enabled: Boolean) {
            val current = callback
            when {
                enabled && current == null -> {
                    val registered = OnBackInvokedCallback {
                        activity.onBackPressedDispatcher.onBackPressed()
                    }
                    activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                        registered,
                    )
                    callback = registered
                }

                !enabled && current != null -> {
                    activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(current)
                    callback = null
                }
            }
        }
    }
}

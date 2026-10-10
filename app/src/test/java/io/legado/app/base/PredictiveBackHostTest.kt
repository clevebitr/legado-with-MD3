package io.legado.app.base

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackHostTest {

    @Test
    fun `关闭预测性返回时 API 33 以上必须常驻拦截返回`() {
        // 拦截一旦缺失，系统会在「没有任何启用的返回处理器」的瞬间把返回当成应用不处理，
        // 显示 back-to-home 预测手势（曾经表现为刚进子页面立即返回仍有动画）。
        assertTrue(shouldRegisterBackInterception(predictiveBackEnabled = false, sdkInt = 33))
        assertTrue(shouldRegisterBackInterception(predictiveBackEnabled = false, sdkInt = 34))
        assertTrue(shouldRegisterBackInterception(predictiveBackEnabled = false, sdkInt = 37))
    }

    @Test
    fun `开启预测性返回时不注册拦截，让应用内跟手动画拿到进度`() {
        assertFalse(shouldRegisterBackInterception(predictiveBackEnabled = true, sdkInt = 33))
        assertFalse(shouldRegisterBackInterception(predictiveBackEnabled = true, sdkInt = 34))
    }

    @Test
    fun `API 33 以下没有平台回调，无从模拟`() {
        assertFalse(shouldRegisterBackInterception(predictiveBackEnabled = false, sdkInt = 32))
        assertFalse(shouldRegisterBackInterception(predictiveBackEnabled = true, sdkInt = 32))
    }
}

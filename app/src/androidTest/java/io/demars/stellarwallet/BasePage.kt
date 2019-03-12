package io.demars.stellarwallet

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

abstract class BasePage {
    internal abstract fun onPageLoaded(): BasePage

    fun pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }
}

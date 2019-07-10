package io.demars.stellarwallet.helpers

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.utils.DebugPreferencesHelper
import io.demars.stellarwallet.utils.GlobalGraphHelper

class WalletLifecycleListener(val context: Context) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        DmcApp.showPin = true

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        if (BuildConfig.DEBUG && DebugPreferencesHelper(context).isPinDisabled) {
            // in debug builds it is possible to disable pin in the session
        } else {
            DmcApp.showPin = false
            GlobalGraphHelper.clearSession()
        }
    }
}

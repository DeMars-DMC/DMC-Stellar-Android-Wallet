package io.demars.stellarwallet.helpers

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.mvvm.effects.EffectsRepository
import io.demars.stellarwallet.utils.DebugPreferencesHelper
import io.demars.stellarwallet.utils.GlobalGraphHelper

class WalletLifecycleListener(val context: Context) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        WalletApplication.appReturnedFromBackground = true

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        if (BuildConfig.DEBUG && DebugPreferencesHelper(context).isPinDisabled) {
            // in debug builds it is possible to disable pin in the session
        } else {
            WalletApplication.appReturnedFromBackground = false
            GlobalGraphHelper.clearSession()
        }
    }
}

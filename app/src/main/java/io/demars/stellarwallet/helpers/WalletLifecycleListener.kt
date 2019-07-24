package io.demars.stellarwallet.helpers

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.utils.DebugPreferencesHelper

class WalletLifecycleListener(val context: Context) : LifecycleObserver {

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onMoveToForeground() {
    if (canAskPinAgain()) {
      DmcApp.showPin = true
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onMoveToBackground() {
    if (BuildConfig.DEBUG && DebugPreferencesHelper(context).isPinDisabled) {
      // in debug builds it is possible to disable pin in the session
    } else if (!DmcApp.showPin && DmcApp.userSession.getPin() != null) {
      // App is unlocked so let's update last pin time
      updatePinTime()
    }
  }

  private fun updatePinTime() {
    DmcApp.latestPinTime = System.currentTimeMillis()
  }

  private fun canAskPinAgain(): Boolean =
    System.currentTimeMillis() - DmcApp.latestPinTime > 60000L
}

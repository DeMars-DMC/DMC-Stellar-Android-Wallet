package io.demars.stellarwallet

import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers

import android.support.test.espresso.Espresso.onView

object SettingsPage : BasePage() {
    override fun onPageLoaded(): SettingsPage {
        onView(ViewMatchers.withId(R.id.clearWalletButton))
        return this
    }

    fun clearWallet() : SettingsPage {
        onView(ViewMatchers.withId(R.id.clearWalletButton)).perform(ViewActions.click())
        return this
    }

    fun pressViewSecretKey(): SettingsPage {
        onView(ViewMatchers.withId(R.id.viewSeedButton)).perform(ViewActions.click())
        return this
    }
}

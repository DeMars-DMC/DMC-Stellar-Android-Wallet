package io.demars.stellarwallet

import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers

import androidx.test.espresso.Espresso.onView

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

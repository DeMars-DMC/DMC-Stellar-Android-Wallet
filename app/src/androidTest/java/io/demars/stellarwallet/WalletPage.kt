package io.demars.stellarwallet

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers

object WalletPage : BasePage() {
    override fun onPageLoaded(): WalletPage {
        onView(ViewMatchers.withId(R.id.navigationView))
        return this
    }

    fun pressReceive(): WalletPage {
        onView(ViewMatchers.withId(R.id.receiveButton)).perform(ViewActions.click())
        return this
    }

    fun pressSettings(): WalletPage {
        onView(ViewMatchers.withId(R.id.nav_settings)).perform(ViewActions.click())
        return this
    }

}

package io.demars.stellarwallet

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers

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

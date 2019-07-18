package io.demars.stellarwallet

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText

object ReceivePage : BasePage() {
    override fun onPageLoaded(): ReceivePage {
        onView(ViewMatchers.withId(R.id.addressEditText))
        return this
    }

    fun assertAccount(accountId : String): ReceivePage {
        // compare with account id on page
        onView(ViewMatchers.withId(R.id.addressEditText)).check(matches(withText(accountId)))
        return this
    }

    fun goBack() : ReceivePage {
        onView(ViewMatchers.isRoot()).perform(ViewActions.pressBack())
        return this
    }
}
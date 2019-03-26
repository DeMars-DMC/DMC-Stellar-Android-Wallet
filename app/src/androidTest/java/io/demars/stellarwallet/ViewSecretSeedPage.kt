package io.demars.stellarwallet

import androidx.test.espresso.matcher.ViewMatchers

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions

object ViewSecretSeedPage : BasePage() {
    override fun onPageLoaded(): ViewSecretSeedPage {
        onView(ViewMatchers.withId(R.id.secretSeedTextView))
        return this
    }

    fun assertSecretSeed(secretSeed : String): ViewSecretSeedPage {
        // compare with account id on page
        onView(ViewMatchers.withId(R.id.secretSeedTextView)).check(ViewAssertions.matches(ViewMatchers.withText(secretSeed)))
        return this
    }

    fun goBack() : ViewSecretSeedPage {
        onView(ViewMatchers.isRoot()).perform(ViewActions.pressBack())
        return this
    }
}

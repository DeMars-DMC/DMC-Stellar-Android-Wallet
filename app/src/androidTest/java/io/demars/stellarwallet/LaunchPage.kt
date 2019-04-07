package io.demars.stellarwallet

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry

object LaunchPage : BasePage() {

    override fun onPageLoaded(): LaunchPage {
        onView(ViewMatchers.withId(R.id.loginButton))
        return this
    }

    fun createWallet(option : MnemonicType, pin : String): LaunchPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val optionString : String = when (option) {
            MnemonicType.WORD_12 -> context.getString(R.string.create_word_option_1)
            MnemonicType.WORD_24 -> context.getString(R.string.create_word_option_2)
        }

        Espresso.onView(ViewMatchers.withText(optionString))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.confirmButton)).perform(ViewActions.click())

        return this
    }

    fun clickRecoverFromSecretKey() : LaunchPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        onView(ViewMatchers.withId(R.id.loginButton)).perform(ViewActions.click())
        val string = context.getString(R.string.recover_from_seed)

        Espresso.onView(ViewMatchers.withText(string))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        return this
    }

    fun clickRecoverFromPhrase(): LaunchPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        onView(ViewMatchers.withId(R.id.loginButton)).perform(ViewActions.click())
        val string = context.getString(R.string.recover_from_phrase)

        Espresso.onView(ViewMatchers.withText(string))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        return this
    }
}

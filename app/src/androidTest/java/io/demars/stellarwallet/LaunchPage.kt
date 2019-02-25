package io.demars.stellarwallet

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers

object LaunchPage : BasePage() {

    override fun onPageLoaded(): LaunchPage {
        onView(ViewMatchers.withId(R.id.recoverWalletButton))
        onView(ViewMatchers.withId(R.id.createWalletButton))
        return this
    }

    fun createWallet(option : MnemonicType, pin : String): LaunchPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        onView(ViewMatchers.withId(R.id.createWalletButton)).perform(ViewActions.click())

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

        onView(ViewMatchers.withId(R.id.recoverWalletButton)).perform(ViewActions.click())
        val string = context.getString(R.string.recover_from_seed)

        Espresso.onView(ViewMatchers.withText(string))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        return this
    }

    fun clickRecoverFromPhrase(): LaunchPage {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        onView(ViewMatchers.withId(R.id.recoverWalletButton)).perform(ViewActions.click())
        val string = context.getString(R.string.recover_from_phrase)

        Espresso.onView(ViewMatchers.withText(string))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        return this
    }
}

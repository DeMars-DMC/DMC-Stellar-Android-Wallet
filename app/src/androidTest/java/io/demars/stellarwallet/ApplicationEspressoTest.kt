package io.demars.stellarwallet

import android.app.Activity
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import io.demars.stellarwallet.activities.LaunchActivity
import io.demars.stellarwallet.activities.PinActivity
import io.demars.stellarwallet.models.stellar.MnemonicType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

/**
 * Instrumented test, which will execute on an Android device.
 *
 * IMPORTANT: make sure that the app and previous tests are not installed in the device.
 * Run sh uninstallApk.sh first. This could be solved using orchestrator tests.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ApplicationEspressoTest {
    private lateinit var context : Context
    private val pin = "1234"

    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule<LaunchActivity>(LaunchActivity::class.java)

    @Before
    fun before() {
        context = activityTestRule.activity.applicationContext
    }

    @Test
    fun testCreateWalletOption12Words() {
        onActivity(LaunchActivity::class)

        createWallet(MnemonicType.WORD_12, pin)

        clearWallet()

        writePin(pin)

        onActivity(LaunchActivity::class)
    }

    @Test
    fun testCreateWalletOption24Words() {
        onActivity(LaunchActivity::class)

        createWallet(MnemonicType.WORD_24, pin)

        clearWallet()

        writePin(pin)

        onActivity(LaunchActivity::class)
    }

    /**
     * Must be called from ActivityType.WALLET_ACTIVITY
     */
    private fun clearWallet() {
        onView(ViewMatchers.withId(R.id.nav_settings)).perform(ViewActions.click())

        onView(ViewMatchers.withId(R.id.logOutButton)).perform(ViewActions.click())

        onActivity(LaunchActivity::class)
    }

    private fun onActivity(activity : KClass<out Activity>) {
        when (activity) {
            LaunchActivity::class -> {
                onView(ViewMatchers.withId(R.id.loginButton))
            }

            PinActivity::class -> {
                onView(ViewMatchers.withId(R.id.pinLockView))
            }
        }
    }

    /**
     * Must be called from ActivityType.LAUNCH_ACTIVITY
     */
    private fun createWallet(option : MnemonicType, pin : String) {
        onActivity(LaunchActivity::class)

        val optionString : String = when (option) {
            MnemonicType.WORD_12 -> context.getString(R.string.create_word_option_1)
            MnemonicType.WORD_24 -> context.getString(R.string.create_word_option_2)
        }

        Espresso.onView(ViewMatchers.withText(optionString))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.confirmButton)).perform(ViewActions.click())
        // create pin
        writePin(pin)
        // re-enter
        writePin(pin)
    }

    /**
     * Must be called from ActivityType.PIN_ACTIVITY
     */
    private fun writePin(pin : String) {
        onActivity(PinActivity::class)

        if (pin.length != 4) {
            throw IllegalStateException("PIN has to have 4 characters, now it has " + pin.length)
        }

        writeIndividualPinNumber(pin[0])
        writeIndividualPinNumber(pin[1])
        writeIndividualPinNumber(pin[2])
        writeIndividualPinNumber(pin[3])
    }

    /**
     * Must be called from ActivityType.PIN_ACTIVITY
     */
    private fun writeIndividualPinNumber(number : Char) {
        Espresso.onView(ViewMatchers.withId(R.id.pinLockView))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder> (
                        ViewMatchers.hasDescendant(ViewMatchers.withText(number.toString())), ViewActions.click()))
    }

}

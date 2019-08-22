package io.demars.stellarwallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import io.demars.stellarwallet.activities.LaunchActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * IMPORTANT: make sure that the app and previous tests are not installed in the device.
 * Run sh uninstallApk.sh first. This could be solved using orchestrator tests.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class WalletCreationTest {
    private val pin = "1234"

    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule<LaunchActivity>(LaunchActivity::class.java)

    @Test
    fun testCreateWalletOption12Words() {
        createWallet(MnemonicType.WORD_12, pin)
    }

    @Test
    fun testCreateWalletOption24Words() {
        createWallet(MnemonicType.WORD_24, pin)
    }

    private fun createWallet(type: MnemonicType, pin: String) {
      LaunchPage.createWallet(type, pin)
        // createAuth pin > re-enter
      PinPage.proceedWithPin(pin)

      WalletPage.pressSettings()

      SettingsPage.clearWallet()
      PinPage.proceedWithPin(pin)
        //restart
      LaunchPage.onPageLoaded()
    }

    @Test
    fun test_cancelling_from_recovery_flow() {
        //TDD for GH-52
      LaunchPage.clickRecoverFromSecretKey()
      RecoveryWalletPage.goBack()

        testCreateWalletOption12Words()
    }
}

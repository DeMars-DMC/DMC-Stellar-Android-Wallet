package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.utils.DebugPreferencesHelper
import io.demars.stellarwallet.utils.GlobalGraphHelper
import io.demars.stellarwallet.utils.ViewUtils
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity() {
  companion object {
    const val VERIFY_PIN_REQUEST: Int = 0x01
  }

  override fun onResume() {
    super.onResume()
    val askForPin = !DebugPreferencesHelper(applicationContext).isPinDisabled
    if (WalletApplication.showPin && askForPin) {
      WalletApplication.showPin = false

      if (GlobalGraphHelper.isExistingWallet()) {
        Timber.d("Existing wallet, opening WalletManagerActivity to verify the pin")
        startActivityForResult(WalletManagerActivity.verifyPin(this), VERIFY_PIN_REQUEST)
      } else {
        Timber.d("Bad state, wiping wallet")
        // bad state, let's clean the wallet
        GlobalGraphHelper.wipe(applicationContext)
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == VERIFY_PIN_REQUEST) {
      when (resultCode) {
        Activity.RESULT_OK -> {
          Timber.d("pin was successful, user will go back to the screen")
        }
        Activity.RESULT_CANCELED -> finish()
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item != null) {
      if (item.itemId == android.R.id.home) {
        finish()
        return true
      }
    }
    return false
  }

  fun finishWithToast(message : String) {
    ViewUtils.showToast(this, message)
    finish()
  }

  fun finishWithToast(messageRes: Int) {
    ViewUtils.showToast(this, messageRes)
    finish()
  }
}
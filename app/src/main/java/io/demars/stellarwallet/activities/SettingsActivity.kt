package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.DiagnosticUtils
import io.demars.stellarwallet.utils.GlobalGraphHelper
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_settings.*
import timber.log.Timber

class SettingsActivity : BaseActivity() {

  enum class SettingsAction {
    SHOW_MNEMONIC, SHOW_SECRET_SEED, SHOW_ACCOUNT, LOG_OUT, TOGGLE_PIN_ON_SENDING, TOGGLE_ENABLE_WEAR_APP
  }

  companion object {
    const val RC_CREATE_DMC_ACCOUNT = 111
    fun newInstance(context: Context): Intent {
      return Intent(context, SettingsActivity::class.java)
    }
  }

  //region Init UI
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    setupUI()
  }

  override fun onResume() {
    super.onResume()
    setSavedSettings()
    updatePersonalInfo()
  }

  private fun setupUI() {
    updatePersonalInfo()

    backButton.setOnClickListener {
      onBackPressed()
    }

    viewPhraseButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.showMnemonic(it.context), SettingsAction.SHOW_MNEMONIC.ordinal)
    }

    viewSeedButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.showSecretSeed(it.context), SettingsAction.SHOW_SECRET_SEED.ordinal)
    }

    logOutButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.verifyPin(it.context), SettingsAction.LOG_OUT.ordinal)
    }

    pinOnSendPaymentsButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.verifyPin(it.context), SettingsAction.TOGGLE_PIN_ON_SENDING.ordinal)
    }

    diagnosticButton.setOnClickListener {
      startActivity(Intent(it.context, DiagnosticActivity::class.java))
    }

    supportEmailButton.setOnClickListener {
      ViewUtils.copyToClipBoard(this, "support@demars.io", "DMC Email",
        R.string.email_copied_message)
    }

    supportWhatsAppButton.setOnClickListener {
      ViewUtils.copyToClipBoard(this, "+230 5 775 8837", "DMC WhatsApp",
        R.string.number_copied_message)
    }

    quickStartButton.setOnClickListener {
      startActivity(WebViewActivity.newIntent(it.context, getString(R.string.quick_start), Constants.URL_QUICK_START))
    }

    termsOfServiceButton.setOnClickListener {
      startActivity(WebViewActivity.newIntent(it.context, getString(R.string.terms_of_service), Constants.URL_TERMS_AND_CONDITIONS))
    }

    appVersionText.text = getString(R.string.pattern_version, DiagnosticUtils.getAppVersion())

    if (BuildConfig.DEBUG) {
      debug.visibility = View.VISIBLE
      debug.setOnClickListener {
        startActivity(Intent(it.context, DebugActivity::class.java))
      }
    } else {
      debug.visibility = View.GONE
    }
  }

  private fun updatePersonalInfo() {
    val isRegistered = DmcApp.wallet.isRegistered()
    editInfoButton.setText(if (isRegistered) R.string.personal_information else R.string.open_account)
    editInfoButton.setOnClickListener {
      if (isRegistered) {
        startActivityForResult(WalletManagerActivity.showDmcAccount(it.context), SettingsAction.SHOW_ACCOUNT.ordinal)
      } else {
        startActivity(OpenAccountActivity.newInstance(it.context))
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (resultCode == Activity.RESULT_OK) {
      when (requestCode) {
        SettingsAction.SHOW_MNEMONIC.ordinal -> {

          val mnemonic = WalletManagerActivity.getResultDataString(data)
          if (mnemonic != null) {
            val phrase = WalletManagerActivity.getResultExtraDataString(data)
            startActivity(MnemonicActivity.newDisplayMnemonicIntent(this, mnemonic, phrase))
          } else {
            Timber.e("fatal error: mnemonic is null")
          }
        }


        SettingsAction.SHOW_SECRET_SEED.ordinal -> {

          val decryptedPhrase = WalletManagerActivity.getResultDataString(data)
          if (decryptedPhrase != null) {
            startActivity(ViewSecretSeedActivity.newInstance(this, decryptedPhrase))
          } else {
            Timber.e("fatal error: decrypted phrase is null")
          }
        }


        SettingsAction.SHOW_ACCOUNT.ordinal -> {
          startActivityForResult(OpenAccountActivity.newInstance(this), RC_CREATE_DMC_ACCOUNT)
        }

        SettingsAction.LOG_OUT.ordinal -> {
          GlobalGraphHelper.wipeAndRestart(this)
        }

        SettingsAction.TOGGLE_PIN_ON_SENDING.ordinal -> {
          DmcApp.wallet.setShowPinOnSend(!DmcApp.wallet.getShowPinOnSend())
        }

        SettingsAction.TOGGLE_ENABLE_WEAR_APP.ordinal -> {
        }
      }
    }
  }

  private fun setSavedSettings() {
    pinOnSendPaymentsButton.isChecked = DmcApp.wallet.getShowPinOnSend()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}

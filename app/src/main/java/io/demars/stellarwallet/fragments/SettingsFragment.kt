package io.demars.stellarwallet.fragments

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.activities.*
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.DiagnosticUtils
import io.demars.stellarwallet.utils.GlobalGraphHelper
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber

class SettingsFragment : BaseFragment() {
  private lateinit var appContext: Context
  private var dmcUser: DmcUser? = null
  private val eventListener = object : ValueEventListener {
    override fun onDataChange(dataSnapshot: DataSnapshot) {
      dmcUser = dataSnapshot.getValue(DmcUser::class.java)
      dmcUser?.let {
        Firebase.removeUserListener(this)
        startActivity(CreateUserActivity.newInstance(context!!, it))
      }
    }

    override fun onCancelled(p0: DatabaseError) {
    }
  }

  enum class SettingsAction {
    SHOW_MNEMONIC, SHOW_SECRET_SEED, SHOW_ACCOUNT, LOG_OUT, TOGGLE_PIN_ON_SENDING, TOGGLE_ENABLE_WEAR_APP
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_settings, container, false)

  companion object {
    fun newInstance(): SettingsFragment = SettingsFragment()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appContext = view.context.applicationContext
    setupUI()
  }

  //region User Interface

  override fun onResume() {
    super.onResume()
    setSavedSettings()
  }

  private fun setupUI() {
    viewPhraseButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.showMnemonic(it.context), SettingsAction.SHOW_MNEMONIC.ordinal)
    }

    viewSeedButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.showSecretSeed(it.context), SettingsAction.SHOW_SECRET_SEED.ordinal)
    }

    editInfoButton.setOnClickListener {
      startActivityForResult(WalletManagerActivity.showDmcAccount(it.context), SettingsAction.SHOW_ACCOUNT.ordinal)
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
      copyToClipBoard("support@demars.io", "DMC Email",
        getString(R.string.email_copied_message))
    }

    supportWhatsAppButton.setOnClickListener {
      copyToClipBoard("+230 5 775 8837", "DMC WhatsApp",
        getString(R.string.number_copied_message))
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
        startActivity(Intent(it.context, DebugPreferenceActivity::class.java))
      }
    } else {
      debug.visibility = View.GONE
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (resultCode == Activity.RESULT_OK) {
      when (requestCode) {
        SettingsAction.SHOW_MNEMONIC.ordinal -> {
          context?.let {
            val mnemonic = WalletManagerActivity.getResultDataString(data)
            if (mnemonic != null) {
              val phrase = WalletManagerActivity.getResultExtraDataString(data)
              startActivity(MnemonicActivity.newDisplayMnemonicIntent(it, mnemonic, phrase))
            } else {
              Timber.e("fatal error: mnemonic is null")
            }
          }

        }

        SettingsAction.SHOW_SECRET_SEED.ordinal -> {
          context?.let {
            val decryptedPhrase = WalletManagerActivity.getResultDataString(data)
            if (decryptedPhrase != null) {
              startActivity(ViewSecretSeedActivity.newInstance(it, decryptedPhrase))
            } else {
              Timber.e("fatal error: decrypted phrase is null")
            }
          }
        }

        SettingsAction.SHOW_ACCOUNT.ordinal -> {
          Firebase.getCurrentUser()?.let { user ->
            Firebase.getUser(user.uid, eventListener)
          }
        }


        SettingsAction.LOG_OUT.ordinal -> {
          GlobalGraphHelper.wipeAndRestart(activity as FragmentActivity)
        }

        SettingsAction.TOGGLE_PIN_ON_SENDING.ordinal -> {
          WalletApplication.wallet.setShowPinOnSend(!WalletApplication.wallet.getShowPinOnSend())
        }

        SettingsAction.TOGGLE_ENABLE_WEAR_APP.ordinal -> {
        }
      }
    }
  }

  private fun setSavedSettings() {
    pinOnSendPaymentsButton.isChecked = WalletApplication.wallet.getShowPinOnSend()
  }

  private fun copyToClipBoard(data: String, label: String, toastMessage: String) {
    activity?.let {
      val clipboard = it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, data)
      clipboard.primaryClip = clip

      Toast.makeText(it, toastMessage, Toast.LENGTH_LONG).show()
    }
  }
}

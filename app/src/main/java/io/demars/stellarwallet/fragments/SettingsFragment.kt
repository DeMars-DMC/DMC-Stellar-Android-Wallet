package io.demars.stellarwallet.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.activities.*
import io.demars.stellarwallet.utils.DiagnosticUtils
import io.demars.stellarwallet.utils.GlobalGraphHelper
import kotlinx.android.synthetic.main.fragment_settings.*
import timber.log.Timber

class SettingsFragment : BaseFragment() {
    private lateinit var appContext : Context

    enum class SettingsAction {
        SHOW_MNEMONIC, SHOW_SECRET_SEED, CLEAR_WALLET, TOGGLE_PIN_ON_SENDING, TOGGLE_ENABLE_WEAR_APP
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

        clearWalletButton.setOnClickListener {
            startActivityForResult(WalletManagerActivity.verifyPin(it.context), SettingsAction.CLEAR_WALLET.ordinal)
        }

        pinOnSendPaymentsButton.setOnClickListener {
            startActivityForResult(WalletManagerActivity.verifyPin(it.context), SettingsAction.TOGGLE_PIN_ON_SENDING.ordinal)
        }

        diagnosticButton.setOnClickListener {
            startActivity(Intent(it.context, DiagnosticActivity::class.java))
        }

        termsOfServiceButton.setOnClickListener {
            startActivity(WebViewActivity.newIntent(it.context, getString(R.string.terms_of_service), "https://docs.google.com/document/d/1C32QmABqKBI9TZdoHCNGu0fT2Y-4QhQPHtacxj5_aYc/edit?usp=sharing"))
        }

        if (BuildConfig.DEBUG) {
            debug.visibility = View.VISIBLE
            debug.setOnClickListener {
                startActivity(Intent(it.context, DebugPreferenceActivity::class.java))
            }

        } else {
            debug.visibility = View.GONE
        }

        val appVersion = DiagnosticUtils.getAppVersion()

        @SuppressLint("SetTextI18n")
        appVersionTextView.text = "Version: $appVersion"
        appVersionTitle.setOnClickListener {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            SettingsAction.SHOW_MNEMONIC.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
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
            }

            SettingsAction.SHOW_SECRET_SEED.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    context?.let {
                        val decryptedPhrase = WalletManagerActivity.getResultDataString(data)
                        if (decryptedPhrase != null) {
                            startActivity(ViewSecretSeedActivity.newInstance(it, decryptedPhrase))
                        } else {
                            Timber.e("fatal error: decrypted phrase is null")
                        }
                    }
                }
            }

            SettingsAction.CLEAR_WALLET.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    GlobalGraphHelper.wipeAndRestart(activity as FragmentActivity)
                }
            }

            SettingsAction.TOGGLE_PIN_ON_SENDING.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    WalletApplication.wallet.setShowPinOnSend(!WalletApplication.wallet.getShowPinOnSend())
                }
            }

            SettingsAction.TOGGLE_ENABLE_WEAR_APP.ordinal -> {
                WalletApplication.wallet
            }
        }
    }

    private fun setSavedSettings() {
        pinOnSendPaymentsButton.isChecked = WalletApplication.wallet.getShowPinOnSend()
    }
}

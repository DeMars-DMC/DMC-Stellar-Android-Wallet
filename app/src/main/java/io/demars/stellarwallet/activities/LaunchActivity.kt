package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import io.demars.stellarwallet.R
import io.demars.stellarwallet.models.MnemonicType
import io.demars.stellarwallet.utils.GlobalGraphHelper
import kotlinx.android.synthetic.main.activity_launch.*

class LaunchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        if (GlobalGraphHelper.isExistingWallet()) {
            createWalletButton.visibility = View.GONE
            recoverWalletButton.visibility = View.GONE
        }

        createWalletButton.setOnClickListener {
            showCreateDialog()
        }

        recoverWalletButton.setOnClickListener {
            showRecoverDialog()
        }

        if (GlobalGraphHelper.isExistingWallet()) {
            GlobalGraphHelper.launchWallet(this)
        }
    }

    private fun showCreateDialog() {
        val builder = AlertDialog.Builder(this@LaunchActivity)
        val walletLengthList = listOf(getString(R.string.create_word_option_1), getString(R.string.create_word_option_2)).toTypedArray()
        builder.setTitle(getString(R.string.create_wallet))
                .setItems(walletLengthList) { _, which ->
                    // The 'which' argument contains the index position
                    // of the selected item

                    val walletLength = if (which == 0) {
                        MnemonicType.WORD_12
                    } else {
                        MnemonicType.WORD_24
                    }

                    startActivity(MnemonicActivity.newCreateMnemonicIntent(this, walletLength))
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showRecoverDialog() {
        val builder = AlertDialog.Builder(this@LaunchActivity)
        val walletLengthList = listOf(getString(R.string.recover_from_phrase), getString(R.string.recover_from_seed)).toTypedArray()
        builder.setTitle(getString(R.string.recover_wallet))
                .setItems(walletLengthList) { _, which ->
                    // The 'which' argument contains the index position
                    // of the selected item

                    val isPhraseRecovery = (which == 0)

                    startActivity(RecoverWalletActivity.newInstance(this, isPhraseRecovery))
                }
        val dialog = builder.create()
        dialog.show()
    }
}

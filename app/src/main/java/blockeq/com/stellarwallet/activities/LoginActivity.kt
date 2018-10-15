package blockeq.com.stellarwallet.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import blockeq.com.stellarwallet.R
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PinActivity.PIN_REQUEST_CODE) {
            finish()
        }
    }

    //region User Interface
    override fun setupUI() {

        createWalletButton.setOnClickListener {
            showCreateDialog()
        }

        recoverWalletButton.setOnClickListener {
            showRecoverDialog()
        }
    }

    private fun showCreateDialog() {
        val builder = AlertDialog.Builder(this@LoginActivity)
        val walletLengthList = listOf("Use a 12 word recovery phrase", "Use a 24 word recovery phrase").toTypedArray()
        builder.setTitle("Create Wallet")
                .setItems(walletLengthList) { _, which ->
                    // The 'which' argument contains the index position
                    // of the selected item

                    val walletLength = if (which == 0) {
                        12
                    } else {
                        24
                    }

                    val intent = Intent(this, ShowMnemonicActivity::class.java)
                    intent.putExtra("walletLength", walletLength)
                    startActivity(intent)
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showRecoverDialog() {
        val builder = AlertDialog.Builder(this@LoginActivity)
        val walletLengthList = listOf("Recover from phrase", "Recover from secret key").toTypedArray()
        builder.setTitle("Recover Wallet")
                .setItems(walletLengthList) { _, which ->
                    // The 'which' argument contains the index position
                    // of the selected item

                    val isPhraseRecovery = (which == 0)

                    val intent = Intent(this, RecoverWalletActivity::class.java)
                    intent.putExtra("isPhraseRecovery", isPhraseRecovery)
                    startActivity(intent)
                }
        val dialog = builder.create()
        dialog.show()
    }

    //endregion
}

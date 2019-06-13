package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.PassphraseDialogHelper
import io.demars.stellarwallet.models.stellar.MnemonicType
import io.demars.stellarwallet.utils.GlobalGraphHelper
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.soneso.stellarmnemonics.Wallet
import kotlinx.android.synthetic.main.activity_mnemonic.*

class MnemonicActivity : BaseActivity(), View.OnClickListener {

    companion object {
        private const val CREATE_WALLET_REQUEST = 0x01
        private const val MNEMONIC_PHRASE = "MNEMONIC_PHRASE"
        private const val PASS_PHRASE = "PASS_PHRASE"

        private const val WALLET_LENGTH = "WALLET_LENGTH"

        fun newCreateMnemonicIntent(context: Context, type : MnemonicType): Intent {
            val intent = Intent(context, MnemonicActivity::class.java)
            intent.putExtra(WALLET_LENGTH, type)
            return intent
        }

        fun newDisplayMnemonicIntent(context: Context, mnemonic: String, passphrase : String?): Intent {
            val intent = Intent(context, MnemonicActivity::class.java)
            intent.putExtra(MNEMONIC_PHRASE, mnemonic)
            intent.putExtra(PASS_PHRASE, passphrase)
            return intent
        }
    }

    private var mnemonicString : String = String()
    private var passphraseToCreate : String = String()
    private var passphraseToDisplay : String? = null
    private var walletLength : MnemonicType = MnemonicType.WORD_12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic)

        loadIntent()
        setupUI()
        setOnClickListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_WALLET_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                GlobalGraphHelper.launchWallet(this)
            }
        }
    }

    //region User Interface
    override fun onClick(v: View) {
        when (v.id) {
            R.id.confirmButton -> startActivityForResult(WalletManagerActivity.createWallet(v.context, mnemonicString, passphraseToCreate), CREATE_WALLET_REQUEST)
            R.id.passphraseButton -> {
                PassphraseDialogHelper(this, object: PassphraseDialogHelper.PassphraseDialogListener {
                    override fun onOK(phrase: String) {
                        passphraseToCreate = phrase
                        passphraseButton.text = getString(R.string.passphrase_applied)
                    }
                }).showForCreating()
            }
        }
    }

    private fun setupUI() {
        if (mnemonicString.isNotEmpty()) {
            // Show chips UI
            confirmButton.visibility = View.GONE
            passphraseButton.visibility = View.GONE
            generateQRCode(mnemonicString, qrImageView, 500)

            if (!WalletApplication.wallet.getIsRecoveryPhrase()) {
                warningPhraseTextView.text = getString(R.string.no_mnemonic_set)
                mnemonicView.visibility = View.GONE
            }
        } else {
            // Create chips UI
            qrImageView.visibility = View.GONE
        }
        setupActionBar()
        setupMnemonicView()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupMnemonicView() {
        mnemonicView.loadChips(getMnemonic())
        passphraseToDisplay?.let {
            passphraseView.loadChips(arrayListOf(it), arrayListOf("passPhrase"))
        }
    }
    //endregion

    //region Set onClick interfaces
    private fun setOnClickListeners() {
        confirmButton.setOnClickListener(this)
        passphraseButton.setOnClickListener(this)
    }
    //endregion

    //region Helper functions
    private fun getMnemonic(): List<String> {
        if (mnemonicString.isEmpty()) {
            val mnemonic = if(walletLength == MnemonicType.WORD_12) {
                Wallet.generate12WordMnemonic()
            } else {
                Wallet.generate24WordMnemonic()
            }
            mnemonicString = String(mnemonic)
        }

        return getMnemonicList(mnemonicString)
    }

    private fun loadIntent() {
        if (!intent.hasExtra(MNEMONIC_PHRASE) && !intent.hasExtra(WALLET_LENGTH)) {
           throw IllegalStateException("inconsistent intent extras, please use companion methods to create the intent")
        }

        if (intent.hasExtra(MNEMONIC_PHRASE)) {
            mnemonicString = intent.getStringExtra(MNEMONIC_PHRASE)
        }

        if (intent.hasExtra(WALLET_LENGTH)) {
            walletLength = intent.getSerializableExtra(WALLET_LENGTH) as MnemonicType
        }
    }

    private fun getMnemonicList(mnemonic : String) : List<String> {
        return mnemonic.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
    }

    private fun generateQRCode(data: String, imageView: ImageView, size: Int) {
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
        imageView.setImageBitmap(bitmap)
    }
    //endregion
}
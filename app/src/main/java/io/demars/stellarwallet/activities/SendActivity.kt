package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.ExchangeApiModel
import io.demars.stellarwallet.models.HorizonException
import io.demars.stellarwallet.mvvm.exchange.ExchangeEntity
import io.demars.stellarwallet.mvvm.exchange.ExchangeViewModel
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.getNumDecimals
import io.demars.stellarwallet.utils.StringFormat.Companion.hasDecimalPoint
import com.davidmiguel.numberkeyboard.NumberKeyboardListener
import kotlinx.android.synthetic.main.activity_send_funds.*

class SendActivity : BaseActivity(), NumberKeyboardListener, SuccessErrorCallback {

    companion object {
        private const val MAX_ALLOWED_DECIMALS = 7
        private const val ARG_ADDRESS_DATA = "ARG_ADDRESS_DATA"
        private const val REQUEST_PIN = 0x0

        fun newIntent(context: Context, address: String): Intent {
            val intent = Intent(context, SendActivity::class.java)
            intent.putExtra(ARG_ADDRESS_DATA, address)
            return intent
        }
    }

    private var amountText: String = ""
    private var amount: Double = 0.0
    private var address: String = ""
    private var exchange : ExchangeApiModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_funds)
        setupUI()
    }

    //region User Interface

    private fun setupUI() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleText.text = WalletApplication.userSession.getFormattedCurrentAvailableBalance(applicationContext)
        assetCodeTextView.text = WalletApplication.userSession.getFormattedCurrentAssetCode()

        amountTextView.text = "0"
        numberKeyboard.setListener(this)

        if (intent.hasExtra(ARG_ADDRESS_DATA)) {
            address = intent.getStringExtra(ARG_ADDRESS_DATA)
        } else {
            throw IllegalStateException("failed to parse the arguments, please use ${SendActivity::class.java.simpleName}#newIntent(...)")
        }

        addressEditText.text = address

        send_button.setOnClickListener {
            if (isAmountValid()) {
                if (WalletApplication.wallet.getShowPinOnSend()) {
                   startActivityForResult(WalletManagerActivity.verifyPin(it.context), REQUEST_PIN)
                } else {
                    sendPayment()
                }
            } else {
                val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
                amountTextView.startAnimation(shakeAnimation)
            }
        }

        val viewModel = ViewModelProviders.of(this).get(ExchangeViewModel::class.java)
        viewModel.exchangeMatching(address).observe(this, Observer {
            updateExchangeProviderText(it)
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PIN) {
            if (resultCode == Activity.RESULT_OK) {
                sendPayment()
            } else {
                finish()
            }
        }
    }

    override fun onNumberClicked(number: Int) {
        if (amountText.isEmpty() && number == 0) {
            return
        }
        updateAmount(amountText + number)
    }

    override fun onLeftAuxButtonClicked() {
        if (!hasDecimalPoint(amountText)) {
            amountText = if (amountText.isEmpty()) "0." else "$amountText."
            showAmount(amountText)
        }
    }

    override fun onRightAuxButtonClicked() {
        if (amountText.isEmpty()) {
            return
        }
        var newAmountText: String
        if (amountText.length <= 1) {
            newAmountText = ""
        } else {
            newAmountText = amountText.substring(0, amountText.length - 1)
            if (newAmountText[newAmountText.length - 1] == '.') {
                newAmountText = newAmountText.substring(0, newAmountText.length - 1)
            }
            if ("0" == newAmountText) {
                newAmountText = ""
            }
        }
        updateAmount(newAmountText)
    }

    private fun updateAmount(newAmountText: String) {
        val newAmount = if (newAmountText.isEmpty()) 0.0 else java.lang.Double.parseDouble(newAmountText)
        if (newAmount >= 0.0 && getNumDecimals(newAmountText) <= MAX_ALLOWED_DECIMALS) {
            amountText = newAmountText
            amount = newAmount
            showAmount(amountText)
        }
    }

    private fun showAmount(amount: String) {
        amountTextView.text = if (amount.isEmpty()) "0" else amount
        if (isAmountValid()) {
            amountTextView.setTextColor(ContextCompat.getColor(this, R.color.toryBlue))
        } else {
            amountTextView.setTextColor(ContextCompat.getColor(this, R.color.colorApricot))
        }
    }

    private fun isAmountValid() : Boolean {
        return amount <= WalletApplication.userSession.getAvailableBalance()!!.toDouble() && amount != 0.0
    }

    //endregion

    private fun sendPayment() {
        exchange.let {
            if (it != null && memoTextView.text.isEmpty()) {
                Toast.makeText(applicationContext, "you must write a {${it.memo}}", Toast.LENGTH_SHORT).show()
            } else {
                if (NetworkUtils(applicationContext).isNetworkAvailable()) {
                    progressBar.visibility = View.VISIBLE

                    val secretSeed = AccountUtils.getSecretSeed(applicationContext)

                    Horizon.getSendTask(this, address, secretSeed,
                            memoTextView.text.toString(), amountText).execute()
                } else {
                    NetworkUtils(applicationContext).displayNoNetwork()
                }
            }
        }
    }

    //region Horizon callbacks
    override fun onSuccess() {
        progressBar.visibility = View.GONE
        Toast.makeText(applicationContext, getString(R.string.send_success_message), Toast.LENGTH_LONG).show()
        runOnUiThread {
            if (!isFinishing) {
                finish()
            }
        }
    }

    override fun onError(error: HorizonException) {
        progressBar.visibility = View.GONE
        // Showing op_low_reserve message when is confusing to the user,
        // specially when the other account was not created and the funds sent are lower than 1 XML.
        // Let's add a generic message for now for any error sending funds.
        Toast.makeText(applicationContext, getString(HorizonException.HorizonExceptionType.SEND.value), Toast.LENGTH_LONG).show()
    }
    //endregion

    @Suppress("DEPRECATION")
    private fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    private fun updateExchangeProviderText(provider : ExchangeEntity?) {
        if (provider != null) {
            identifiedAddressTextView.text = fromHtml(getString(R.string.send_address_identified, provider.name))
            identifiedAddressTextView.visibility = View.VISIBLE
            memoTitleTextView.text = provider.memo
            memoTextView.hint = null
        } else {
            identifiedAddressTextView.visibility = View.GONE
        }
    }

}

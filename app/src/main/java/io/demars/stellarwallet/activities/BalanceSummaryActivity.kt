package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.AssetUtils
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat
import kotlinx.android.synthetic.main.activity_balance_summary.*
import kotlinx.android.synthetic.main.activity_balance_summary.toolbar

class BalanceSummaryActivity : BasePopupActivity() {

  companion object {
    private const val ARG_EXTRA_ASSET = "ARG_EXTRA_ASSET"
    private const val ARG_EXTRA_ISSUER = "ARG_EXTRA_ISSUER"

    private const val ARG_EXTRA_IS_NATIVE = "ARG_EXTRA_IS_NATIVE"

    fun newIntent(context: Context, assetCode: String, issuer: String): Intent {
      val intent = Intent(context, BalanceSummaryActivity::class.java)
      intent.putExtra(ARG_EXTRA_ASSET, assetCode)
      intent.putExtra(ARG_EXTRA_ISSUER, issuer)
      intent.putExtra(ARG_EXTRA_IS_NATIVE, false)
      return intent
    }

    fun newNativeAssetIntent(context: Context): Intent {
      val intent = Intent(context, BalanceSummaryActivity::class.java)
      intent.putExtra(ARG_EXTRA_IS_NATIVE, true)
      return intent
    }
  }

  override fun setContent(): Int = R.layout.activity_balance_summary
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { onBackPressed() }

    setupUI()
  }

  private fun setupUI() {
    val isNative = intent.getBooleanExtra(ARG_EXTRA_IS_NATIVE, false)
    val assetCode: String = intent.getStringExtra(ARG_EXTRA_ASSET) ?: "XLM"
    val issuer: String? = intent.getStringExtra(ARG_EXTRA_ISSUER)
    val decimalPlaces = AssetUtils.getDecimalPlaces(assetCode)

    totalBalanceTextView.text = StringFormat.truncateDecimalPlaces(
      AccountUtils.getTotalBalance(assetCode), decimalPlaces)
    availableBalanceTextView.text = StringFormat.truncateDecimalPlaces(
      AccountUtils.getAvailableBalance(assetCode), decimalPlaces)

    tradedAmountTextView.text = "-"
    tradedXLMTextView.text = StringFormat.truncateDecimalPlaces(
      AccountUtils.getPostedForTrade(assetCode), 2)
    if (!isNative) hideMinimumBalance() else renderNativeAsset()
  }

  private fun renderNativeAsset() {
    val minimumBalance = WalletApplication.userSession.getMinimumBalance()

    if (minimumBalance != null) {
      baseReserveAmountTextView.text = "1"
      baseReserveXLMTextView.text = Constants.BASE_RESERVE.toString()

      trustlinesAmountTextView.text = minimumBalance.trustlines.count.toString()
      trustlinesXLMTextView.text = minimumBalance.trustlines.amount.toString()

      offersAmountTextView.text = minimumBalance.offers.count.toString()
      offersXLMTextView.text = minimumBalance.offers.amount.toString()

      signersAmountTextView.text = minimumBalance.signers.count.toString()
      signersXLMTextView.text = minimumBalance.signers.amount.toString()
    } else {
      hideMinimumBalance()
    }
  }

  private fun hideMinimumBalance() {
    row1.visibility = View.GONE
    row2.visibility = View.GONE
    row3.visibility = View.GONE
    row4.visibility = View.GONE
    row5.visibility = View.GONE
  }
}

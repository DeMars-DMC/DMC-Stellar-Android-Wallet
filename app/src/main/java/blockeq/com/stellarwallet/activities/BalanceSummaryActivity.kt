package blockeq.com.stellarwallet.activities

import android.os.Bundle
import blockeq.com.stellarwallet.R
import blockeq.com.stellarwallet.WalletApplication
import blockeq.com.stellarwallet.helpers.Constants
import blockeq.com.stellarwallet.utils.AccountUtils
import kotlinx.android.synthetic.main.activity_balance_summary.*

class BalanceSummaryActivity : BasePopupActivity() {

    override fun setContent(): Int {
        return R.layout.activity_balance_summary
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
    }

    override fun setupUI() {
        val minimumBalance = WalletApplication.userSession.minimumBalance

        if (minimumBalance != null) {

            totalBalanceTextView.text = AccountUtils.getTotalBalance(Constants.LUMENS_ASSET_TYPE)
            availableBalanceTextView.text = WalletApplication.localStore!!.availableBalance

            baseReserveAmountTextView.text = "1"
            baseReserveXLMTextView.text = Constants.BASE_RESERVE.toString()

            trustlinesAmountTextView.text = minimumBalance.trustlines.count.toString()
            trustlinesXLMTextView.text = minimumBalance.trustlines.amount.toString()

            offersAmountTextView.text = minimumBalance.offers.count.toString()
            offersXLMTextView.text = minimumBalance.offers.amount.toString()

            signersAmountTextView.text = minimumBalance.signers.count.toString()
            signersXLMTextView.text = minimumBalance.signers.amount.toString()

            minimumBalanceTextView.text = minimumBalance.totalAmount.toString()
        }
    }
}

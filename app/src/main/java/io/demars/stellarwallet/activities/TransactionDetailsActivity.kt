package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.lifecycle.Observer
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.R
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.Contact
import io.demars.stellarwallet.api.horizon.model.Operation
import io.demars.stellarwallet.api.horizon.model.Trade
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.mvvm.contacts.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_transaction_details.*
import java.io.Serializable

class TransactionDetailsActivity : BaseActivity() {

  private lateinit var transaction: Serializable
  private var contacts = ArrayList<Contact>()
  private var toAddress: String? = null

  companion object {
    private const val ARG_TRANSACTION = "ARG_TRANSACTION"
    fun newInstance(context: Context, operation: Serializable): Intent {
      val intent = Intent(context, TransactionDetailsActivity::class.java)
      intent.putExtra(ARG_TRANSACTION, operation)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_transaction_details)

    transaction = intent.getSerializableExtra(ARG_TRANSACTION)

    initUI()
  }

  private fun initUI() {
    backButton.setOnClickListener { onBackPressed() }

    copyAddressIcon.visibility = View.VISIBLE
    copyAddressIcon.setOnClickListener {
      toAddress?.let {
        ViewUtils.copyToClipBoard(this, it, R.string.address_copied_message)
      }
    }

    when (transaction) {
      is Operation -> {
        initForOperation(transaction as Operation)
      }
      is Trade -> {
        initForTrade(transaction as Trade)
      }
    }
  }

  private fun initForOperation(operation: Operation) {
    secondAssetContainer.visibility = View.GONE
    arrowContainer.visibility = View.GONE

    val accountId = DmcApp.wallet.getStellarAccountId()

    dateTimeView.text = StringFormat.getFormattedDateTime(operation.createdAt,
      DateFormat.is24HourFormat(this))

    val assetCode = operation.asset ?: "XLM"
    firstAssetTitle.text = assetCode
    firstAssetLogo.setImageResource(AssetUtils.getLogo(assetCode))
    firstAmountView.text = StringFormat.formatNumber4Decimals(operation.amount, assetCode)

    val memoToUse = if (operation.memo == null) "" else operation.memo

    memoTextView.text = memoToUse
    memoContainer.visibility = if (memoToUse.isNullOrEmpty()) View.GONE else View.VISIBLE

    when (operation.type) {
      Operation.OperationType.CREATED.value -> {
        if (operation.to == accountId) {
          titleView.setText(R.string.account_created)
          toContainer.visibility = View.VISIBLE
          toPrefix.text = "Funder:"
          toAddress = operation.from
        } else {
          titleView.text = "Sent"
          toContainer.visibility = View.VISIBLE
          firstAmountView.text = String.format(getString(R.string.negative_template),
            firstAmountView.text.toString())
          toAddress = operation.to
        }
      }
      Operation.OperationType.PAYMENT.value -> {
        when (accountId) {
          operation.from -> {
            when {
              operation.to == Constants.FEE_ACCOUNT -> {
                titleView.text = getString(R.string.withdrawal_fee)
                toContainer.visibility = View.GONE
              }
              operation.to == AssetUtils.getWithdrawAccount(assetCode) -> {
                titleView.text = getString(R.string.withdrawal)
                toContainer.visibility = View.GONE
              }
              else -> {
                titleView.setText(R.string.sent)
                toContainer.visibility = View.VISIBLE
                toAddress = operation.to
              }
            }
            firstAmountView.text = String.format(getString(R.string.negative_template), firstAmountView.text.toString())
          }
          operation.to -> {
            titleView.setText(R.string.received)
            toContainer.visibility = View.VISIBLE
            toPrefix.text = "From: "
            toAddress = operation.from
          }
        }
      }
      Operation.OperationType.ALLOW_TRUST.value -> {
        titleView.setText(R.string.trustline_allowed)
        infoContainer.visibility = View.GONE
      }
      Operation.OperationType.CHANGE_TRUST.value -> {
        titleView.setText(R.string.trustline_changed)
        infoContainer.visibility = View.GONE
      }
      Operation.OperationType.PATH_PAYMENT.value,
      Operation.OperationType.MANAGE_OFFER.value,
      Operation.OperationType.PASSIVE_OFFER.value,
      Operation.OperationType.INFLATION.value,
      Operation.OperationType.MANAGE_DATA.value,
      Operation.OperationType.SET_OPTIONS.value,
      Operation.OperationType.ACCOUNT_MERGE.value -> {
      }
    }

    toText.text = toAddress
    initContacts(toAddress)
  }

  private fun initForTrade(trade: Trade) {
    secondAssetContainer.visibility = View.VISIBLE
    arrowContainer.visibility = View.VISIBLE

    contactContainer.visibility = View.GONE
    memoContainer.visibility = View.GONE

    copyAddressIcon.visibility = View.GONE

    infoContainer.setBackgroundResource(R.drawable.bg_card_transparent)

    titleView.setText(R.string.trade_details)

    val baseIsSeller = trade.baseIsSeller ?: false

    val assetSold = if (baseIsSeller) trade.baseAsset else trade.counterAsset
    val assetBought = if (baseIsSeller) trade.counterAsset else trade.baseAsset

    val amountSold = if (baseIsSeller) trade.baseAmount else trade.counterAmount
    val amountBought = if (baseIsSeller) trade.counterAmount else trade.baseAmount

    val price = if (baseIsSeller) (trade.priceN.toDouble() / trade.priceD.toDouble()) else
      (trade.priceD.toDouble() / trade.priceN.toDouble())

    firstAssetTitle.text = StringFormat.formatAssetCode(assetSold)
    firstAssetLogo.setImageResource(AssetUtils.getLogo(assetSold))
    firstAmountView.text = String.format(getString(R.string.negative_template),
      StringFormat.truncateDecimalPlaces(amountSold,
        AssetUtils.getMaxDecimals(assetSold)))

    secondAssetTitle.text = StringFormat.formatAssetCode(assetBought)
    secondAssetLogo.setImageResource(AssetUtils.getLogo(assetBought))
    secondAmountView.text = StringFormat.truncateDecimalPlaces(amountBought,
      AssetUtils.getMaxDecimals(assetBought))

    dateTimeView.text = StringFormat.getFormattedDateTime(trade.createdAt, DateFormat.is24HourFormat(this))

    toPrefix.text = "Price:"
    toText.text = "${StringFormat.truncateDecimalPlaces(price, 7)} $assetSold/$assetBought"
  }

  private fun initContacts(address: String?) {
    if (address == null) return

    if (contacts.isEmpty()) {
      ContactsRepositoryImpl(this).getContactsListLiveData(true)
        .observe(this, Observer {
          contacts = ArrayList(it.stellarContacts)
          checkContacts(address)
        })
    } else {
      checkContacts(address)
    }
  }

  private fun checkContacts(address: String) {
    var name = ""

    for (contact in contacts) {
      // Check contacts now
      if (contact.stellarAddress == address) {
        name = contact.name
        break
      }
    }

    if (name.isNotEmpty()) {
      contactContainer?.visibility = View.VISIBLE
      contactTextView?.text = name
    } else {
      contactContainer?.visibility = View.GONE
    }
  }
}
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
import io.demars.stellarwallet.models.stellar.Operation
import io.demars.stellarwallet.models.stellar.Trade
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_transaction_details.*
import java.io.Serializable

class TransactionDetailsActivity : BaseActivity() {

  private lateinit var operation: Serializable
  private var contacts = ArrayList<Contact>()
  private var toAddress: String? = null

  companion object {
    private const val ARG_OPERATION = "ARG_OPERATION"
    fun newInstance(context: Context, operation: Serializable): Intent {
      val intent = Intent(context, TransactionDetailsActivity::class.java)
      intent.putExtra(ARG_OPERATION, operation)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_transaction_details)
    checkIntent()
    initUI()
  }

  private fun checkIntent() {
    operation = intent.getSerializableExtra(ARG_OPERATION)
  }

  private fun initUI() {
    backButton.setOnClickListener { onBackPressed() }

    copyAddressIcon.visibility = View.VISIBLE
    copyAddressIcon.setOnClickListener {
      toAddress?.let {
        ViewUtils.copyToClipBoard(this, it,
          "Stellar address", R.string.address_copied_message)
      }
    }

    when (operation) {
      is Operation -> {
        initForOperation(operation as Operation)
      }
      is Trade -> {
        initForTrade(operation as Trade)
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
          titleView.text = "Account created"
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
                titleView.text = "Sent"
                toContainer.visibility = View.VISIBLE
                toAddress = operation.to
              }
            }
            firstAmountView.text = String.format(getString(R.string.negative_template), firstAmountView.text.toString())
          }
          operation.to -> {
            titleView.text = "Received"
            toContainer.visibility = View.VISIBLE
            toPrefix.text = "From: "
            toAddress = operation.from
          }
        }
      }
      Operation.OperationType.ALLOW_TRUST.value -> {
//        viewHolder.transactionType.text = "Trustline allowed"
      }
      Operation.OperationType.CHANGE_TRUST.value -> {
//        viewHolder.transactionType.text = "Trustline changed"
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

    titleView.text = "Exchanged"

    firstAssetTitle.text = StringFormat.formatAssetCode(trade.baseAsset)
    firstAssetLogo.setImageResource(AssetUtils.getLogo(trade.baseAsset))

    secondAssetTitle.text = StringFormat.formatAssetCode(trade.counterAsset)
    secondAssetLogo.setImageResource(AssetUtils.getLogo(trade.counterAsset))

    firstAmountView.text = String.format(getString(R.string.negative_template),
      StringFormat.truncateDecimalPlaces(trade.baseAmount, AssetUtils.getMaxDecimals(trade.baseAsset)))

    secondAmountView.text = StringFormat.truncateDecimalPlaces(trade.counterAmount,
      AssetUtils.getMaxDecimals(trade.counterAsset))

    dateTimeView.text = StringFormat.getFormattedDateTime(trade.createdAt, DateFormat.is24HourFormat(this))

    toPrefix.text = "Price:"
    toText.text = StringFormat.truncateDecimalPlaces(trade.price, 7)
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
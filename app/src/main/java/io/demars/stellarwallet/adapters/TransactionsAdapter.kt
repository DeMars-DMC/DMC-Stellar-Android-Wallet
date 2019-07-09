package io.demars.stellarwallet.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.StringFormat.Companion.getFormattedDateTime
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import android.text.format.DateFormat
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.stellar.*
import io.demars.stellarwallet.utils.AssetUtils


class TransactionsAdapter(var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var contacts: ArrayList<Contact>? = null
  private var items: ArrayList<Any>? = null

  enum class TransactionViewType(val value: Int) {
    TRANSACTION(0), TRADE(1), OPERATION(2)
  }

  fun setContacts(contacts: ArrayList<Contact>) {
    this.contacts = contacts
  }

  fun updateData(list: ArrayList<Any>) {
    items = list
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)

    return when (viewType) {
      TransactionViewType.OPERATION.value -> {
        val v = inflater.inflate(R.layout.item_transaction, parent, false)
        OperationViewHolder(v)
      }
      TransactionViewType.TRANSACTION.value -> {
        val v = inflater.inflate(R.layout.item_transaction, parent, false)
        OperationViewHolder(v)
      }
      TransactionViewType.TRADE.value -> {
        val v = inflater.inflate(R.layout.item_transaction, parent, false)
        OperationViewHolder(v)
      }
      else -> {
        val v = inflater.inflate(R.layout.item_transaction, parent, false)
        OperationViewHolder(v)
      }
    }
  }

  override fun getItemCount(): Int = if (items == null) 0 else items!!.size
  override fun getItemViewType(position: Int): Int {
    items?.let {
      return when {
        it[position] is Transaction -> TransactionViewType.TRANSACTION.value
        it[position] is Trade -> TransactionViewType.TRADE.value
        it[position] is Operation -> TransactionViewType.OPERATION.value
        else -> 0
      }
    }

    return 0
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder.itemViewType) {
      TransactionViewType.TRANSACTION.value -> {
        val vh = holder as OperationViewHolder
        configureTransactionViewHolder(vh, position)
      }
      TransactionViewType.TRADE.value -> {
        val vh = holder as OperationViewHolder
        configureTradeViewHolder(vh, position)
      }
      TransactionViewType.OPERATION.value -> {
        val vh = holder as OperationViewHolder
        configureOperationViewHolder(vh, position)
      }
    }
  }

  //region View Holders
  class OperationViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var amount: TextView = v.findViewById(R.id.amountTextView)
    var date: TextView = v.findViewById(R.id.dateTextView)
    var transactionType: TextView = v.findViewById(R.id.transactionTypeTextView)
    var info: TextView = v.findViewById(R.id.transactionInfoTextView)
    var dot: ImageView = v.findViewById(R.id.dotImageView)
  }

  private fun formatNumber4Decimals(amount: String?, asset: String): String? =
    if (amount == null) "--" else truncateDecimalPlaces(amount, AssetUtils.getMaxDecimals(asset))

  private fun formatAddress(address: String?): String {
    if (address == null) return "--"
    contacts?.forEach {
      // Check contacts now
      if (it.stellarAddress == address) return it.name
    }

    val length = address.length
    return if (length < 12) address else
      "${address.substring(0, 4)}...${address.substring(length - 5, length - 1)}"
  }

  @SuppressLint("SetTextI18n")
  private fun configureTransactionViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val transaction = items!![position] as Transaction

    viewHolder.transactionType.text = "Transaction"
    viewHolder.amount.text = truncateDecimalPlaces(transaction.amount,
      AssetUtils.getMaxDecimals(transaction.assetCode ?: "XLM"))
    viewHolder.date.text = getFormattedDateTime(transaction.createdAt,
      DateFormat.is24HourFormat(context))

    when {
      transaction.memo != null -> {
        viewHolder.info.visibility = VISIBLE
        viewHolder.info.text = transaction.memo
      }
      else -> viewHolder.info.visibility = GONE
    }
  }

  @SuppressLint("SetTextI18n")
  private fun configureTradeViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val trade = items!![position] as Trade

    viewHolder.transactionType.text = String.format(context.getString(R.string.exchange_item_template),
      StringFormat.formatAssetCode(trade.baseAsset), StringFormat.formatAssetCode(trade.counterAsset))
    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)
    if (WalletApplication.userSession.getSessionAsset().assetCode == trade.baseAsset) {
      viewHolder.amount.text = String.format(context.getString(R.string.negative_template),
        truncateDecimalPlaces(trade.baseAmount, AssetUtils.getMaxDecimals(trade.baseAsset)))
    } else {
      viewHolder.amount.text = truncateDecimalPlaces(trade.counterAmount,
        AssetUtils.getMaxDecimals(trade.counterAsset))
    }
    viewHolder.date.text = getFormattedDateTime(trade.createdAt, DateFormat.is24HourFormat(context))

    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
    viewHolder.info.visibility = VISIBLE
    viewHolder.info.text = context.getString(R.string.pattern_price,
      truncateDecimalPlaces(trade.price, 7))
  }

  @SuppressLint("SetTextI18n")
  private fun configureOperationViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val operation = items!![position] as Operation
    val accountId = WalletApplication.wallet.getStellarAccountId()
    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)

    viewHolder.amount.text = formatNumber4Decimals(operation.amount, operation.asset ?: "XLM")
    viewHolder.date.text = getFormattedDateTime(operation.createdAt,
      DateFormat.is24HourFormat(context))

    val memoToUse = if (operation.memo == null) ""
    else context.getString(R.string.pattern_memo, operation.memo)

    viewHolder.info.text = memoToUse
    viewHolder.info.visibility = if (memoToUse.isNullOrEmpty()) GONE else VISIBLE

    when (operation.type) {
      Operation.OperationType.CREATED.value -> {
        if (operation.to == accountId) {
          viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorMantis), PorterDuff.Mode.SRC_IN)
          viewHolder.transactionType.text = "Account created"
          viewHolder.info.visibility = VISIBLE
          viewHolder.info.text = "Funder: " + formatAddress(operation.from)
        } else {
          viewHolder.transactionType.text = "Sent to ${formatAddress(operation.to)}"
          viewHolder.amount.text = String.format(context.getString(R.string.negative_template), viewHolder.amount.text.toString())
          viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorTerracotta), PorterDuff.Mode.SRC_IN)
        }
      }
      Operation.OperationType.PAYMENT.value -> {
        val assetCode = operation.asset?.toUpperCase() ?: "XLM"

        when (accountId) {
          operation.from -> {
            when {
              operation.to == Constants.FEE_ACCOUNT -> {
                viewHolder.transactionType.text = context.getString(R.string.withdrawal_fee)
                viewHolder.info.visibility = GONE
              }
              operation.to == AssetUtils.getWithdrawAccount(assetCode) -> {
                viewHolder.transactionType.text = context.getString(R.string.withdrawal)
                viewHolder.info.visibility = GONE
              }
              else -> viewHolder.transactionType.text = "Sent to ${formatAddress(operation.to)}"
            }
            viewHolder.amount.text = String.format(context.getString(R.string.negative_template), viewHolder.amount.text.toString())
            viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorTerracotta), PorterDuff.Mode.SRC_IN)
          }
          operation.to -> {
            viewHolder.transactionType.text = "Received from ${formatAddress(operation.from)}"
            viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorMantis), PorterDuff.Mode.SRC_IN)
          }
        }
      }
      Operation.OperationType.PATH_PAYMENT.value -> {
      }
      Operation.OperationType.MANAGE_OFFER.value -> {
        viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_IN)

        viewHolder.transactionType.text = String.format(context.getString(R.string.exchange_item_template),
          StringFormat.formatAssetCode(operation.asset!!), StringFormat.formatAssetCode(operation.counterAsset!!))

        val amountDouble = operation.amount?.toDouble()
        val priceDouble = operation.price?.toDouble()
        val totalAmount = (amountDouble!! * priceDouble!!).toString()
        val sessionAsset = WalletApplication.userSession.getSessionAsset().assetCode
        val decimalPlaces = AssetUtils.getMaxDecimals(sessionAsset)
        if (sessionAsset == operation.asset) {
          viewHolder.amount.text = String.format(context.getString(R.string.negative_template),
            truncateDecimalPlaces(totalAmount, decimalPlaces))
        } else {
          viewHolder.amount.text = truncateDecimalPlaces(totalAmount, decimalPlaces)
        }

        viewHolder.info.visibility = VISIBLE
        viewHolder.info.text = "Amount: ${truncateDecimalPlaces(operation.amount, decimalPlaces)}" +
          "\nPrice: ${truncateDecimalPlaces(operation.price, decimalPlaces)}"
      }
      Operation.OperationType.PASSIVE_OFFER.value -> {
      }
      Operation.OperationType.ALLOW_TRUST.value -> {
        viewHolder.transactionType.text = "Trustline allowed"
      }
      Operation.OperationType.CHANGE_TRUST.value -> {
        viewHolder.transactionType.text = "Trustline changed"
      }
      Operation.OperationType.INFLATION.value -> {
      }
      Operation.OperationType.MANAGE_DATA.value -> {
      }
      Operation.OperationType.SET_OPTIONS.value -> {
      }
      Operation.OperationType.ACCOUNT_MERGE.value -> {
      }
    }
  }
  //endregion
}

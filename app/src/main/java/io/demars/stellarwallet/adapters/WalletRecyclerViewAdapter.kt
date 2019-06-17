package io.demars.stellarwallet.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.StringFormat.Companion.getFormattedDateTime
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.DateFormat
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.stellar.*
import io.demars.stellarwallet.utils.AssetUtils


class WalletRecyclerViewAdapter(var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var onAssetsDropdownListener: OnAssetDropdownListener? = null
  private var onLearnMoreListener: OnLearnMoreButtonListener? = null
  private var contacts: ArrayList<Contact>? = null
  private var items: ArrayList<Any>? = null

  interface OnAssetDropdownListener {
    fun onAssetDropdownClicked(view: View, position: Int)
  }

  interface OnLearnMoreButtonListener {
    fun onLearnMoreButtonClicked(view: View, assetCode: String, issuer: String?, position: Int)
  }

  enum class TransactionViewType(val value: Int) {
    TOTAL(0), AVAILABLE(1), HEADER(2),
    ACCOUNT_EFFECT(3), TRADE_EFFECT(4),
    TRANSACTION(5), TRADE(6), OPERATION(7)
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
      TransactionViewType.TOTAL.value -> {
        val v = inflater.inflate(R.layout.item_total_balance, parent, false)
        TotalBalanceViewHolder(v)
      }
      TransactionViewType.AVAILABLE.value -> {
        val v = inflater.inflate(R.layout.item_available_balance, parent, false)
        AvailableBalanceViewHolder(v)
      }
      TransactionViewType.HEADER.value -> {
        val v = inflater.inflate(R.layout.item_header_transaction_list, parent, false)
        TransactionHeaderViewHolder(v)
      }
      TransactionViewType.OPERATION.value -> {
        val v = inflater.inflate(R.layout.item_account_activity, parent, false)
        OperationViewHolder(v)
      }
      TransactionViewType.TRANSACTION.value -> {
        val v = inflater.inflate(R.layout.item_account_activity, parent, false)
        OperationViewHolder(v)
      }
      TransactionViewType.TRADE.value -> {
        val v = inflater.inflate(R.layout.item_account_activity, parent, false)
        OperationViewHolder(v)
      }
      TransactionViewType.TRADE_EFFECT.value -> {
        val v = inflater.inflate(R.layout.item_account_activity, parent, false)
        OperationViewHolder(v)
      }
      else -> {
        val v = inflater.inflate(R.layout.item_account_activity, parent, false)
        OperationViewHolder(v)
      }
    }
  }

  override fun getItemCount(): Int = if (items == null) 0 else items!!.size
  override fun getItemViewType(position: Int): Int {
    items?.let {
      return when {
        it[position] is TotalBalance -> TransactionViewType.TOTAL.value
        it[position] is AvailableBalance -> TransactionViewType.AVAILABLE.value
        it[position] is Pair<*, *> -> TransactionViewType.HEADER.value
        it[position] is Effect -> TransactionViewType.ACCOUNT_EFFECT.value
        it[position] is TradeEffect -> TransactionViewType.TRADE_EFFECT.value
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
      TransactionViewType.TOTAL.value -> {
        val vh = holder as TotalBalanceViewHolder
        configureTotalBalanceViewHolder(vh, position)
      }
      TransactionViewType.AVAILABLE.value -> {
        val vh = holder as AvailableBalanceViewHolder
        configureAvailableBalanceViewHolder(vh, position)
      }
      TransactionViewType.HEADER.value -> {
        val vh = holder as TransactionHeaderViewHolder
        configureTransactionHeaderViewHolder(vh, position)
      }
      TransactionViewType.ACCOUNT_EFFECT.value -> {
        val vh = holder as OperationViewHolder
        configureAccountEffectViewHolder(vh, position)
      }
      TransactionViewType.TRADE_EFFECT.value -> {
        val vh = holder as OperationViewHolder
        configureTradeEffectViewHolder(vh, position)
      }
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


  fun setOnAssetDropdownListener(listener: OnAssetDropdownListener) {
    onAssetsDropdownListener = listener
  }

  fun setOnLearnMoreButtonListener(listener: OnLearnMoreButtonListener) {
    onLearnMoreListener = listener
  }

  //region View Holders
  inner class TotalBalanceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var root: View = v.findViewById(R.id.balanceRoot)
    var balance: TextView = v.findViewById(R.id.balanceTextView)
    var assetName: TextView = v.findViewById(R.id.assetNameTextView)
    var assetsButton: ImageView = v.findViewById(R.id.assetsButton)
    var progressBar: ProgressBar = v.findViewById(R.id.progressRefreshingWallet)

    init {
      assetsButton.setOnClickListener {
        onAssetsDropdownListener?.let { listener ->
          val position = adapterPosition
          if (position != RecyclerView.NO_POSITION) {
            listener.onAssetDropdownClicked(v, position)
          }
        }
      }
    }
  }

  inner class AvailableBalanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var balance: TextView = view.findViewById(R.id.availableBalanceTextView)
    var learnMoreButton: TextView = view.findViewById(R.id.learnMoreButton)
  }

  class TransactionHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var activityTextView: TextView = v.findViewById(R.id.activityHeaderTextView)
    var amountTextView: TextView = v.findViewById(R.id.amountHeaderTextView)
  }

  class OperationViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var amount: TextView = v.findViewById(R.id.amountTextView)
    var date: TextView = v.findViewById(R.id.dateTextView)
    var transactionType: TextView = v.findViewById(R.id.transactionTypeTextView)
    var info: TextView = v.findViewById(R.id.transactionInfoTextView)
    var dot: ImageView = v.findViewById(R.id.dotImageView)
    var line: View = v.findViewById(R.id.lineView)
  }

  private fun configureTotalBalanceViewHolder(viewHolder: TotalBalanceViewHolder, position: Int) {
    val totalBalance = items!![position] as TotalBalance

    viewHolder.balance.text = totalBalance.balance
    val code = getVisibleAssetCode(totalBalance.assetCode)

    if (code.isEmpty()) {
      tintProgressBar(viewHolder.progressBar, ContextCompat.getColor(context, R.color.white))
      viewHolder.progressBar.visibility = VISIBLE
      viewHolder.assetsButton.visibility = GONE
      viewHolder.assetName.text = totalBalance.assetName
    } else {
      viewHolder.progressBar.visibility = GONE
      viewHolder.assetsButton.visibility = VISIBLE
      viewHolder.assetName.text = getVisibleAssetCode(totalBalance.assetCode)
    }

    when (totalBalance.state) {
      WalletState.ERROR, WalletState.NOT_FUNDED -> {
        viewHolder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.blueDark))
        viewHolder.progressBar.visibility = GONE
      }
      WalletState.ACTIVE, WalletState.UPDATING -> {
        viewHolder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
      }
      else -> { //nothing
      }
    }
  }

  private fun tintProgressBar(progressBar: ProgressBar, @ColorInt color: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      val wrapDrawable = DrawableCompat.wrap(progressBar.indeterminateDrawable)
      DrawableCompat.setTint(wrapDrawable, color)
      progressBar.indeterminateDrawable = DrawableCompat.unwrap<Drawable>(wrapDrawable)
    } else {
      progressBar.indeterminateDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
  }

  private fun configureAvailableBalanceViewHolder(viewHolder: AvailableBalanceViewHolder, position: Int) {
    val availableBalance = items!![position] as AvailableBalance
    @SuppressLint("SetTextI18n")
    viewHolder.balance.text = context.getString(R.string.pattern_available, availableBalance.balance)
    viewHolder.learnMoreButton.setOnClickListener { view ->
      onLearnMoreListener?.onLearnMoreButtonClicked(view, availableBalance.assetCode, availableBalance.issuer, position)
    }
  }

  private fun getVisibleAssetCode(assetCode: String): String {
    return if (assetCode != "native") {
      assetCode
    } else {
      "XLM"
    }
  }

  private fun configureTransactionHeaderViewHolder(viewHolder: TransactionHeaderViewHolder, position: Int) {
    val pair = items!![position] as Pair<*, *>
    viewHolder.activityTextView.text = pair.first.toString()
    viewHolder.amountTextView.text = pair.second.toString()
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
      "${address.substring(0, 5)}...${address.substring(length - 6, length - 1)}"
  }

  @SuppressLint("SetTextI18n")
  private fun configureAccountEffectViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val transaction = items!![position] as Effect

    viewHolder.amount.text = formatNumber4Decimals(transaction.amount,
      transaction.assetCode ?: "XLM")
    viewHolder.date.text = getFormattedDateTime(transaction.createdAt,
      DateFormat.is24HourFormat(context))


    viewHolder.transactionType.text = when (transaction.type) {
      EffectType.RECEIVED.value -> "Received"
      EffectType.SENT.value -> "Sent"
      EffectType.CREATED.value -> "Account created"
      EffectType.REMOVED.value -> "Account Removed"
      EffectType.ACCOUNT_HOME_DOMAIN_UPDATED.value -> "Account home domain updated"
      EffectType.ACCOUNT_FLAGS_UPDATED.value -> "Account flags updated"
      EffectType.ACCOUNT_INFLATION_DESTINATION_UPDATED.value -> "Account inflation destination updated"
      EffectType.SIGNER_CREATED.value -> "Signer created"
      EffectType.SIGNER_REMOVED.value -> "Signer removed"
      EffectType.SIGNER_UPDATED.value -> "Signer updated"
      EffectType.TRUSTLINE_CREATED.value -> "Trustline created"
      EffectType.TRUSTLINE_REMOVED.value -> "Trustline removed"
      EffectType.TRUSTLINE_UPDATED.value -> "Trustline updated"
      EffectType.TRUSTLINE_AUTHORIZED.value -> "Trustline authorized"
      EffectType.TRUSTLINE_DEAUTHORIZED.value -> "Trustline deauthorized"
      EffectType.OFFER_CREATED.value -> "Offer created"
      EffectType.OFFER_REMOVED.value -> "Offer removed"
      EffectType.OFFER_UPDATED.value -> "Offer updated"
      EffectType.DATA_CREATED.value -> "Data created"
      EffectType.DATA_REMOVED.value -> "Data removed"
      EffectType.DATA_UPDATED.value -> "Data updated"
      EffectType.SEQUENCE_BUMPED.value -> "Sequence bumped"
      else -> "Error"
    }

    when {
      transaction.type == EffectType.RECEIVED.value -> viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorMantis), PorterDuff.Mode.SRC_IN)
      transaction.type == EffectType.SENT.value -> {
        viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorApricot), PorterDuff.Mode.SRC_IN)
        viewHolder.amount.text = String.format(context.getString(R.string.bracket_template), viewHolder.amount.text.toString())
      }
      else -> viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)
    }

    viewHolder.line.visibility = if (viewHolder.adapterPosition != itemCount - 1) VISIBLE else GONE
    viewHolder.info.visibility = GONE
  }

  private fun configureTradeEffectViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val trade = items!![position] as TradeEffect

    viewHolder.transactionType.text = String.format(context.getString(R.string.exchange_item_template),
      StringFormat.formatAssetCode(trade.soldAsset), StringFormat.formatAssetCode(trade.boughtAsset))
    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)
    if (WalletApplication.userSession.getSessionAsset().assetCode == trade.boughtAsset) {
      viewHolder.amount.text = truncateDecimalPlaces(trade.boughtAmount,
        AssetUtils.getMaxDecimals(trade.boughtAsset))
    } else {
      viewHolder.amount.text = String.format(context.getString(R.string.bracket_template),
        truncateDecimalPlaces(trade.soldAmount,
          AssetUtils.getMaxDecimals(trade.soldAsset)))
    }
    viewHolder.date.text = getFormattedDateTime(trade.createdAt,
      DateFormat.is24HourFormat(context))

    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
    viewHolder.line.visibility = if (viewHolder.adapterPosition != itemCount - 1) VISIBLE else GONE
    viewHolder.info.visibility = GONE
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

    viewHolder.line.visibility = if (viewHolder.adapterPosition != itemCount - 1) VISIBLE else GONE
  }

  @SuppressLint("SetTextI18n")
  private fun configureTradeViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val trade = items!![position] as Trade

    viewHolder.transactionType.text = String.format(context.getString(R.string.exchange_item_template),
      StringFormat.formatAssetCode(trade.baseAsset), StringFormat.formatAssetCode(trade.counterAsset))
    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)
    if (WalletApplication.userSession.getSessionAsset().assetCode == trade.baseAsset) {
      viewHolder.amount.text = String.format(context.getString(R.string.bracket_template),
        truncateDecimalPlaces(trade.baseAmount, AssetUtils.getMaxDecimals(trade.baseAsset)))
    } else {
      viewHolder.amount.text = truncateDecimalPlaces(trade.counterAmount,
        AssetUtils.getMaxDecimals(trade.counterAsset))
    }
    viewHolder.date.text = getFormattedDateTime(trade.createdAt, DateFormat.is24HourFormat(context))

    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
    viewHolder.line.visibility = if (viewHolder.adapterPosition != itemCount - 1) VISIBLE else GONE
    viewHolder.info.visibility = VISIBLE
    viewHolder.info.text = context.getString(R.string.pattern_price,
      truncateDecimalPlaces(trade.price, 7))
  }

  @SuppressLint("SetTextI18n")
  private fun configureOperationViewHolder(viewHolder: OperationViewHolder, position: Int) {
    val operation = items!![position] as Operation
    val accountId = WalletApplication.wallet.getStellarAccountId()
    viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorPaleSky), PorterDuff.Mode.SRC_IN)
    viewHolder.line.visibility = if (viewHolder.adapterPosition != itemCount - 1) VISIBLE else GONE

    viewHolder.amount.text = formatNumber4Decimals(operation.amount, operation.asset ?: "XLM")
    viewHolder.date.text = getFormattedDateTime(operation.createdAt,
      DateFormat.is24HourFormat(context))

    val memoToUse = when {
      operation.memo != null -> operation.memo
      else -> ""
    }

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
          viewHolder.amount.text = String.format(context.getString(R.string.bracket_template), viewHolder.amount.text.toString())
          viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorApricot), PorterDuff.Mode.SRC_IN)
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
            viewHolder.amount.text = String.format(context.getString(R.string.bracket_template), viewHolder.amount.text.toString())
            viewHolder.dot.setColorFilter(ContextCompat.getColor(context, R.color.colorApricot), PorterDuff.Mode.SRC_IN)
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
          viewHolder.amount.text = String.format(context.getString(R.string.bracket_template),
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

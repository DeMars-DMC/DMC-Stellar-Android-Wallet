package io.demars.stellarwallet.fragments.tabs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.*
import io.demars.stellarwallet.models.AssetUtil
import io.demars.stellarwallet.models.Currency
import io.demars.stellarwallet.models.SelectionModel
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import kotlinx.android.synthetic.main.fragment_tab_trade.*
import org.stellar.sdk.Asset
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber
import java.text.DecimalFormat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.lifecycle.Observer
import io.demars.stellarwallet.models.BalanceAvailability
import io.demars.stellarwallet.mvvm.balance.BalanceRepository
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.views.ConfirmTradeDialog


class TradeTabFragment : Fragment(), View.OnClickListener, OnUpdateTradeTab, ConfirmTradeDialog.DialogListener {
  private lateinit var appContext: Context
  private lateinit var parentListener: OnTradeCurrenciesChanged
  private lateinit var selectedSellingCurrency: SelectionModel
  private lateinit var selectedBuyingCurrency: SelectionModel
  private lateinit var toolTip: PopupWindow

  private var sellingCurrencies = mutableListOf<SelectionModel>()
  private var buyingCurrencies = mutableListOf<SelectionModel>()
  private var holdingsAmount: Double = 0.0
  private var addedCurrencies: ArrayList<Currency> = ArrayList()
  private var latestBid: OrderBookResponse.Row? = null
  //  private var orderType: OrderType = OrderType.MARKET
  private var isShowingAdvanced = false
  private val ZERO_VALUE = "0.0"
  private val decimalFormat = DecimalFormat("0.#######")
  private var balance: BalanceAvailability? = null
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_tab_trade, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appContext = view.context.applicationContext
    toolTip = PopupWindow(view.context)
    refreshAddedCurrencies()
    setupListeners()
    updateView()
  }

  private fun setupListeners() {
    toggleMarket.setOnClickListener(this)
    toggleLimit.setOnClickListener(this)
    tenth.setOnClickListener(this)
    quarter.setOnClickListener(this)
    half.setOnClickListener(this)
    threeQuarters.setOnClickListener(this)
    all.setOnClickListener(this)
    placeTrade.setOnClickListener(this)

    sellingCustomSelector.editText.isEnabled = false
    buyingCustomSelector.editText.isEnabled = false

    sellingCustomSelector.editText.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        updateBuyingValueIfNeeded()
        refreshSubmitTradeButton()
      }
    })


    buyingCustomSelector.editText.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        refreshSubmitTradeButton()
      }
    })


    BalanceRepository.loadBalance().observe(this, Observer {
      if (it != null) {
        balance = it
        refreshAddedCurrencies()
        setupSpinners()
        Timber.d("new balance")
        if (::selectedSellingCurrency.isInitialized) {
          sellingCurrencies.forEach { selection ->
            if (selection.label == selectedSellingCurrency.label) {
              refreshBalance(selection.holdings)
            }
          }
          buyingCustomSelector.editText.setText("")
          sellingCustomSelector.editText.setText("")
          refreshSubmitTradeButton()
          updateBuyingValueIfNeeded()
        }
      }
    })
  }

  private fun setupSpinners() {
    sellingCustomSelector.setSelectionValues(sellingCurrencies)
    sellingCustomSelector.spinner.onItemSelectedListener = object : OnItemSelected() {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (sellingCurrencies.size <= position) return // safety

        selectedSellingCurrency = sellingCurrencies[position]
        refreshBalance(selectedSellingCurrency.holdings)

        sellingCustomSelector.imageView.setImageResource(
          Constants.getLogo(selectedSellingCurrency.label))

        resetBuyingCurrencies()
        buyingCurrencies.removeAt(position)

        buyingCustomSelector.setSelectionValues(buyingCurrencies)

        onSelectorChanged()
      }
    }

    buyingCustomSelector.setSelectionValues(buyingCurrencies)
    buyingCustomSelector.spinner.onItemSelectedListener = object : OnItemSelected() {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (buyingCurrencies.size <= position) return // safety

        selectedBuyingCurrency = buyingCurrencies[position]

        buyingCustomSelector.imageView.setImageResource(
          Constants.getLogo(selectedBuyingCurrency.label))

        onSelectorChanged()
      }
    }
  }

  private fun refreshBalance(holding: Double) {
    var availableForTrading = holding
    if (selectedSellingCurrency.label == "XLM") {
      availableForTrading = applyNativeFees(holding)
    }

    var string = String.format(getString(R.string.holdings_amount),
      decimalFormat.format(availableForTrading),
      selectedSellingCurrency.label)

    holdings.text = string

    holdingsAmount = availableForTrading

    val hasHoldings = holdingsAmount > 0.0

    holdings.text = String.format(getString(R.string.holdings_amount),
      decimalFormat.format(holdingsAmount),
      selectedSellingCurrency.label)

    all.visibility = if (hasHoldings) View.VISIBLE else View.GONE
    sellingCustomSelector.editText.isEnabled = hasHoldings
    buyingCustomSelector.editText.isEnabled = false
  }

  private fun applyNativeFees(amount: Double): Double {
    // it will reserve double the network fee to be able to cancel the 100% open offer
    val value = amount - 0.5 - 0.0002
    if (value < 0) return 0.00
    return value
  }

  private fun updateView() {
    if (isShowingAdvanced) {
      tenth.visibility = View.VISIBLE
      quarter.visibility = View.VISIBLE
      half.visibility = View.VISIBLE
      threeQuarters.visibility = View.VISIBLE
    } else {
      tenth.visibility = View.GONE
      quarter.visibility = View.GONE
      half.visibility = View.GONE
      threeQuarters.visibility = View.GONE
    }
  }

  private fun onSelectorChanged() {
    latestBid = null
    if (::selectedBuyingCurrency.isInitialized && ::selectedSellingCurrency.isInitialized) {
      notifyParent(selectedSellingCurrency, selectedBuyingCurrency)
    }
    refreshSubmitTradeButton()
    updateBuyingValueIfNeeded()
  }

  private fun refreshSubmitTradeButton() {
    val sellingValue = sellingCustomSelector.editText.text.toString()
    val buyingValue = buyingCustomSelector.editText.text.toString()

    var numberFormatValid = true
    var sellingValueDouble = 0.0
    var buyingValueDouble = 0.0
    try {
      sellingValueDouble = sellingValue.toDouble()
      buyingValueDouble = buyingValue.toDouble()
    } catch (e: NumberFormatException) {
      Timber.d("selling or buying value have not a valid format")
      numberFormatValid = false
    }

    if (sellingValue.isEmpty() || buyingValue.isEmpty() || !numberFormatValid ||
      sellingValueDouble.compareTo(0) == 0 || buyingValueDouble.compareTo(0) == 0) {
      placeTrade.isEnabled = false
    } else if (::selectedSellingCurrency.isInitialized) {
      placeTrade.isEnabled = sellingValue.toDouble() <= selectedSellingCurrency.holdings
    }
  }

  private fun updateBuyingValueIfNeeded() {
    if (sellingCustomSelector == null) return
    val sellingString = sellingCustomSelector.editText.text.toString()
    if (sellingString.isEmpty()) {
      buyingCustomSelector.editText.text.clear()
      buyingCustomSelector.editText.isEnabled = false
      return
    }

    buyingCustomSelector.editText.isEnabled = true

    if (sellingString.replace(",", ".").toDouble() > holdingsAmount) {
      sellingCustomSelector.editText.setText(holdingsAmount.toString())
    }

    if (latestBid != null) {
      val value = sellingCustomSelector.editText.text.toString().toFloatOrNull()
      val price = latestBid?.price?.toFloatOrNull()
      if (value != null && price != null) {
        val floatValue: Float = value.toFloat()
        val floatPrice: Float = price.toFloat()
        val stringValue = decimalFormat.format(floatValue * floatPrice)
        buyingCustomSelector.editText.setText(stringValue)
      }
    }
  }

  private fun updatePrices() {
    prices?.let {
      val price = latestBid?.price?.toFloatOrNull()
      val ssBuilder = SpannableStringBuilder("")
      if (price != null) {
        val priceString = "Rate: 1\u00A0${selectedSellingCurrency.label}\u00A0=" +
          "\u00A0$price\u00A0${selectedBuyingCurrency.label}\n"
        val invertedString = "1 ${selectedBuyingCurrency.label} = ${1F / price} ${selectedSellingCurrency.label}"

        val indexBold = priceString.length

        ssBuilder.append(priceString)
        ssBuilder.setSpan(
          StyleSpan(Typeface.BOLD),
          0, indexBold,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val transparentString = "Rate: "
        val indexTransparent = indexBold + transparentString.length

        ssBuilder.append(transparentString)
        ssBuilder.setSpan(
          ForegroundColorSpan(Color.TRANSPARENT),
          indexBold, indexTransparent,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ssBuilder.append(invertedString)
        ssBuilder.setSpan(RelativeSizeSpan(0.7f),
          indexTransparent, indexTransparent + invertedString.length,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      } else {
        val noOffersString = getString(R.string.no_offers).toUpperCase()
        ssBuilder.append(noOffersString)
      }

      it.text = ssBuilder
    }
  }

  private fun notifyParent(selling: SelectionModel?, buying: SelectionModel?) {
    if (selling != null && buying != null) {
      parentListener.onCurrencyChange(selling, buying)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (toolTip.isShowing) {
      toolTip.dismiss()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.tenth -> sellingCustomSelector.editText.setText(decimalFormat.format(0.1 * holdingsAmount).toString())
      R.id.quarter -> sellingCustomSelector.editText.setText(decimalFormat.format(0.25 * holdingsAmount).toString())
      R.id.half -> sellingCustomSelector.editText.setText(decimalFormat.format(0.5 * holdingsAmount).toString())
      R.id.threeQuarters -> sellingCustomSelector.editText.setText(decimalFormat.format(0.75 * holdingsAmount).toString())
      R.id.all -> sellingCustomSelector.editText.setText(decimalFormat.format(holdingsAmount))
      R.id.placeTrade -> onPlaceTrade()
    }
  }

  private fun onPlaceTrade() {
    val sellingText = sellingCustomSelector.editText.text.toString().replace(",", ".")
    val buyingText = buyingCustomSelector.editText.text.toString().replace(",", ".")

    val sellingCode = selectedSellingCurrency.label
    val buyingCode = selectedBuyingCurrency.label

    ConfirmTradeDialog(context!!, sellingText, buyingText,
      sellingCode, buyingCode, this).show()
  }

  override fun onConfirmed(sellingText: String, buyingText: String) {
    proceedWithTrade(sellingText, buyingText,
      selectedSellingCurrency.asset!!, selectedBuyingCurrency.asset!!)
  }

  private fun createSnackBar(text: CharSequence, duration: Int): Snackbar? {
    activity?.let {
      return Snackbar.make(it.findViewById(R.id.content_container), text, duration)
    }
    return null
  }

  private fun proceedWithTrade(sellingAmount: String, buyingAmount: String,
                               sellingAsset: Asset, buyingAsset: Asset) {
    val snackBar = createSnackBar("Submitting order", Snackbar.LENGTH_INDEFINITE)
    val snackView = snackBar?.view as Snackbar.SnackbarLayout
    val progress = ProgressBar(context)
    val height = resources.getDimensionPixelOffset(R.dimen.progress_snackbar_height)
    val width = resources.getDimensionPixelOffset(R.dimen.progress_snackbar_width)

    val params = FrameLayout.LayoutParams(height, width)
    params.gravity = Gravity.END or Gravity.RIGHT or Gravity.CENTER_VERTICAL
    val margin = resources.getDimensionPixelOffset(R.dimen.progress_snackbar_margin)
    progress.setPadding(margin, margin, margin, margin)
    snackView.addView(progress, params)
    snackBar.show()

    placeTrade.isEnabled = false

    sellingCustomSelector.editText.isEnabled = false
    buyingCustomSelector.editText.isEnabled = false

    WalletApplication.userSession.getAvailableBalance()

    val sellingAmountFormatted: String
    val priceFormatted: String
    try {
      sellingAmountFormatted = decimalFormat.format(sellingAmount.toDouble())
      priceFormatted = decimalFormat.format(buyingAmount.toDouble() / sellingAmount.toDouble())
    } catch (ex: NumberFormatException) {
      ViewUtils.showToast(activity, "Wrong numbers format")
      return
    }

    Horizon.getCreateMarketOffer(object : Horizon.OnMarketOfferListener {
      override fun onExecuted() {
        snackBar.dismiss()
        createSnackBar("Order executed", Snackbar.LENGTH_SHORT)?.show()

        placeTrade.isEnabled = true
        sellingCustomSelector.editText.isEnabled = true
        buyingCustomSelector.editText.isEnabled = true
      }

      override fun onFailed(errorMessage: String) {
        snackBar.dismiss()

        createSnackBar("Order failed: $errorMessage", Snackbar.LENGTH_SHORT)?.show()

        placeTrade.isEnabled = true
        sellingCustomSelector.editText.isEnabled = false
        buyingCustomSelector.editText.isEnabled = false
      }
    }, AccountUtils.getSecretSeed(appContext), sellingAsset, buyingAsset,
      sellingAmountFormatted, priceFormatted)
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    try {
      parentListener = parentFragment as OnTradeCurrenciesChanged
    } catch (e: ClassCastException) {
      Timber.e("the parent must implement: %s", OnTradeCurrenciesChanged::class.java.simpleName)
    }
  }

  private fun resetBuyingCurrencies() {
    buyingCurrencies.clear()
    addedCurrencies.forEach {
      buyingCurrencies.add(it)
    }
  }

  private fun refreshAddedCurrencies() {
    if (balance == null) {
      return
    }
    addedCurrencies.clear()
    var i = 0
    var native: Currency? = null
    balance?.let {
      it.getAllBalances().forEach { that ->
        val currency = if (that.assetCode == "XLM") {
          native = Currency(i, AssetUtil.NATIVE_ASSET_CODE, "LUMEN", that.totalAvailable.toDouble(), that.asset)
          native as Currency
        } else {
          Currency(i, that.assetCode, that.assetCode, that.totalAvailable.toDouble(), that.asset)
        }
        addedCurrencies.add(currency)
        i++
      }

      native?.let { currency ->
        addedCurrencies.remove(currency)
        addedCurrencies.add(0, currency)
      }

      sellingCurrencies.clear()
      buyingCurrencies.clear()
      addedCurrencies.forEach { added ->
        sellingCurrencies.add(added)
        buyingCurrencies.add(added)
      }
    }
  }

  override fun onLastOrderBookUpdated(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
    if (bids.isNotEmpty()) {
      latestBid = bids[0]
      updateBuyingValueIfNeeded()
    } else {
      latestBid = null
    }

    updatePrices()
  }
}
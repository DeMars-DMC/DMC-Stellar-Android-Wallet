package io.demars.stellarwallet.fragments.tabs

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.interfaces.*
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.models.Currency
import io.demars.stellarwallet.models.SelectionModel
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import kotlinx.android.synthetic.main.fragment_tab_trade.*
import org.stellar.sdk.Asset
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.lifecycle.Observer
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.views.ConfirmTradeDialog


class TradeTabFragment(val assetCode: String, val assetIssuer: String) : Fragment(), View.OnClickListener, OnUpdateTradeTab, ConfirmTradeDialog.DialogListener {

  private lateinit var appContext: Context
  private lateinit var parentListener: OnTradeCurrenciesChanged
  private lateinit var selectedSellingCurrency: SelectionModel
  private lateinit var selectedBuyingCurrency: SelectionModel
  private lateinit var toolTip: PopupWindow

  private var sellingCurrencies = mutableListOf<SelectionModel>()
  private var buyingCurrencies = mutableListOf<SelectionModel>()
  private var availableAmount: Double = 0.0
  private var addedCurrencies: ArrayList<Currency> = ArrayList()
  private var latestBid: OrderBookResponse.Row? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_tab_trade, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appContext = view.context.applicationContext
    toolTip = PopupWindow(view.context)
    refreshAddedCurrencies()
    setupListeners()
    setupSpinners()
    setupAsset()
  }

  override fun onResume() {
    super.onResume()
    updateAvailableBalance()
  }

  private fun setupListeners() {
    sellAll.setOnClickListener(this)
    placeTrade.setOnClickListener(this)

    sellingCustomSelector.editText.isEnabled = false
    buyingCustomSelector.editText.isEnabled = false

    swapCurrenciesButton.setOnClickListener {
      swapCurrencies()
    }

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
  }

  private fun setupSpinners() {
    sellingCustomSelector.setSelectionValues(sellingCurrencies)
    sellingCustomSelector.spinner.onItemSelectedListener = object : OnItemSelected() {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onSellingCurrencyChanged(position)
      }
    }

    buyingCustomSelector.setSelectionValues(buyingCurrencies)
    buyingCustomSelector.spinner.onItemSelectedListener = object : OnItemSelected() {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onBuyingCurrencyChanged(position)
      }
    }
  }

  private fun setupAsset() {
    val position = addedCurrencies.indexOfFirst { it.code == assetCode }
    sellingCustomSelector.spinner.setSelection(position)
    onSellingCurrencyChanged(position)
  }

  private fun onSellingCurrencyChanged(position: Int) {
    if (sellingCurrencies.size <= position || position == -1) return // safety

    selectedSellingCurrency = sellingCurrencies[position]
    updateAvailableBalance()

    sellingCustomSelector.imageView.setImageResource(
      AssetUtils.getLogo(selectedSellingCurrency.label))

    resetBuyingCurrencies()
    buyingCurrencies.removeAt(position)
    buyingCustomSelector.setSelectionValues(buyingCurrencies)

    onSelectorChanged()
  }

  private fun onBuyingCurrencyChanged(position: Int) {
    if (buyingCurrencies.size <= position || position == -1) return // safety

    selectedBuyingCurrency = buyingCurrencies[position]

    updateAvailableBalance()

    buyingCustomSelector.imageView.setImageResource(
      AssetUtils.getLogo(selectedBuyingCurrency.label))

    onSelectorChanged()
  }

  private fun swapCurrencies() {
    if (!isCurrenciesInitialized()) return
    if (!isUiReady()) return

    val sellingBefore = selectedSellingCurrency
    val buyingBefore = selectedBuyingCurrency
    val sellingPosition = sellingCurrencies.indexOf(buyingBefore)
    sellingCustomSelector.spinner.setSelection(sellingPosition)
    onSellingCurrencyChanged(sellingPosition)
    val buyingPosition = buyingCurrencies.indexOf(sellingBefore)
    buyingCustomSelector.spinner.setSelection(buyingCurrencies.indexOf(sellingBefore))
    onBuyingCurrencyChanged(buyingPosition)
  }

  fun updateAvailableBalance() {
    if (!isCurrenciesInitialized()) return
    if (!isUiReady()) return

    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      val assetCode = selectedSellingCurrency.label
      val available = StringFormat.truncateDecimalPlaces(
        AccountUtils.getAvailableBalance(assetCode), AssetUtils.getMaxDecimals(assetCode))
      val decimalPlaces = AssetUtils.getMaxDecimals(assetCode)

      availableAmount = available.toDouble()

      val hasHoldings = availableAmount > 0.0

      val availableStr = String.format(getString(R.string.holdings_amount), StringFormat.truncateDecimalPlaces(
        availableAmount.toString(), decimalPlaces), assetCode)

      availableToSell.text = availableStr

      val buyingAssetCode = selectedBuyingCurrency.label
      val buyingDecimalPlaces = AssetUtils.getMaxDecimals(buyingAssetCode)
      buyingBalance.text = String.format(getString(R.string.balance_amount), StringFormat.truncateDecimalPlaces(
        AccountUtils.getTotalBalance(buyingAssetCode), buyingDecimalPlaces), buyingAssetCode)

      sellAll.visibility = if (hasHoldings) View.VISIBLE else View.INVISIBLE
      sellAll.isEnabled = hasHoldings
      sellingCustomSelector.editText.isEnabled = hasHoldings
      buyingCustomSelector.editText.isEnabled = hasHoldings
    })
  }

  private fun onSelectorChanged() {
    latestBid = null
    if (isCurrenciesInitialized()) {
      notifyParent(selectedSellingCurrency, selectedBuyingCurrency)
    }
    refreshSubmitTradeButton()
    updateBuyingValueIfNeeded()
  }

  private fun refreshSubmitTradeButton() {
    if (!isCurrenciesInitialized()) return
    if (!isUiReady()) return
    if (placeTrade == null) return

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

    if (sellingString.replace(",", ".").toDoubleOrNull() ?: 0.0 > availableAmount) {
      sellingCustomSelector.editText.setText(
        StringFormat.truncateDecimalPlaces(availableAmount.toString(),
          AssetUtils.getMaxDecimals(selectedSellingCurrency.label)))
    }

    var stringValue = buyingCustomSelector.editText.text.toString().toDoubleOrNull() ?: 0.0
    if (latestBid != null) {
      val value = sellingCustomSelector.editText.text.toString().toFloatOrNull()
      val price = latestBid?.price?.toFloatOrNull()
      if (value != null && price != null) {
        val floatValue: Double = value.toDouble()
        val floatPrice: Double = price.toDouble()
        stringValue = floatValue * floatPrice
      }
    }

    buyingCustomSelector.editText.setText(StringFormat.truncateDecimalPlaces(stringValue.toString(),
      AssetUtils.getMaxDecimals(selectedBuyingCurrency.label)))
  }

  private fun updatePrices() {
    prices?.let {
      val price = latestBid?.price?.toDoubleOrNull()
      val ssBuilder = SpannableStringBuilder("")
      if (price != null) {
        val formattedPrice = StringFormat.truncateDecimalPlaces(price, 7)
        val revertedPrice = StringFormat.truncateDecimalPlaces((1.0 / price), 7)
        val priceString = "Price: 1\u00A0${selectedSellingCurrency.label}\u00A0=" +
          "\u00A0$formattedPrice\u00A0${selectedBuyingCurrency.label}\n"
        val invertedString = "1 ${selectedBuyingCurrency.label} = $revertedPrice ${selectedSellingCurrency.label}"

        val indexBold = priceString.length

        ssBuilder.append(priceString)
        ssBuilder.setSpan(
          StyleSpan(Typeface.BOLD),
          0, indexBold,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val transparentString = "Price: "
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
      R.id.sellAll -> sellingCustomSelector.editText.setText(
        StringFormat.truncateDecimalPlaces(availableAmount.toString(),
          AssetUtils.getMaxDecimals(selectedSellingCurrency.label)))
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
      return Snackbar.make(it.findViewById(R.id.rootView), text, duration)
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
    params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
    val margin = resources.getDimensionPixelOffset(R.dimen.progress_snackbar_margin)
    progress.setPadding(margin, margin, margin, margin)
    snackView.addView(progress, params)
    snackBar.show()

    placeTrade.isEnabled = false

    sellingCustomSelector.editText.isEnabled = false
    buyingCustomSelector.editText.isEnabled = false

    DmcApp.userSession.getAvailableBalance()

    val sellingAmountFormatted: String
    val priceFormatted: String
    try {
      sellingAmountFormatted = StringFormat.truncateDecimalPlaces(sellingAmount,
        AssetUtils.getMaxDecimals(selectedSellingCurrency.label))
      val priceString = (buyingAmount.toDouble() / sellingAmount.toDouble()).toString()
      priceFormatted = StringFormat.truncateDecimalPlaces(priceString,
        AssetUtils.getMaxDecimals(selectedBuyingCurrency.label))
    } catch (ex: NumberFormatException) {
      ViewUtils.showToast(activity, "Wrong numbers format")
      return
    }

    Horizon.getCreateMarketOffer(object : Horizon.OnMarketOfferListener {
      override fun onExecuted() {
        snackBar.dismiss()
        createSnackBar("Order executed", Snackbar.LENGTH_SHORT)?.show()
        updateAvailableBalance()
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
      parentListener = activity as OnTradeCurrenciesChanged
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
    val accounts = DmcApp.wallet.getBalances()
    addedCurrencies.clear()
    var i = 0
    var native: Currency? = null
    accounts.forEach {
      val currency = if (it.assetType != "native") {
        Currency(i, it.assetCode, it.assetCode, it.balance.toDouble(), it.asset)
      } else {
        native = Currency(i, Constants.LUMENS_ASSET_CODE, "LUMEN", it.balance.toDouble(), it.asset)
        native as Currency
      }
      addedCurrencies.add(currency)
      i++
    }

    native?.let {
      addedCurrencies.remove(it)
      addedCurrencies.add(0, it)
    }

    sellingCurrencies.clear()
    buyingCurrencies.clear()
    addedCurrencies.forEach {
      sellingCurrencies.add(it)
      buyingCurrencies.add(it)
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

  private fun isCurrenciesInitialized(): Boolean =
    ::selectedSellingCurrency.isInitialized &&
      ::selectedBuyingCurrency.isInitialized

  private fun isUiReady(): Boolean = sellingCustomSelector != null && buyingCustomSelector != null
}
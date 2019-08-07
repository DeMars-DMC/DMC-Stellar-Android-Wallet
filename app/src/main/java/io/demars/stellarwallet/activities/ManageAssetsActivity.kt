package io.demars.stellarwallet.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.interfaces.AssetListener
import kotlinx.android.synthetic.main.activity_manage_assets.*
import org.stellar.sdk.Asset
import android.content.Intent
import android.graphics.PorterDuff
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.helpers.Preferences
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.interfaces.OnAssetSelected
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.*
import kotlinx.android.synthetic.main.activity_manage_assets.addressEditText
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber

class ManageAssetsActivity : BaseActivity(), AssetListener, OnAssetSelected, ValueEventListener {

  //region Properties
  companion object {
    const val RC_ADD_CUSTOM = 111
    const val RC_CREATE_ACC = 222
  }

  private lateinit var adapter: AssetsAdapter
  private lateinit var reportingAsset: DataAsset
  private lateinit var bottomSheet: BottomSheetDialog
  private var isCustomizing = false
  private var calculationQueue = 0
  private var totalBalance = 0.0
  //endregion

  //region Init
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_manage_assets)

    Firebase.getUserStellarAddress(this)

    reportingAsset = AssetUtils.getDataAssetFromPrefs(this)

    initUI()
  }

  private fun initUI() {
    reportingCurrency?.text = reportingAsset.code
    reportingCurrencyLogo?.setImageResource(AssetUtils.getLogo(reportingAsset.code))

    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      refreshAssets()
    }

    accountButton.setOnClickListener {
      openSettings()
    }

    totalBalanceProgress.indeterminateDrawable.setColorFilter(
      ContextCompat.getColor(this, R.color.whiteMain), PorterDuff.Mode.MULTIPLY)

    initAssetsView()
    initBottomSheet()

    checkInflation()
  }

  private fun initAssetsView() {
    adapter = AssetsAdapter(this)
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    assetsRecyclerView.adapter = adapter

    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> { response ->
      when (response.httpCode) {
        200 -> {
          //Funded account
          updateViewForActive()
          calculateTotalBalance()
          if (checkPreloadedAsset()) {
            adapter.refreshAdapter()
          }
        }
        404 -> {
          // Not Funded
          updateViewForFunding()
        }
        else -> {
          // Error
          updateViewForError()
        }
      }
    })
  }

  private fun initBottomSheet() {
    bottomSheet = BottomSheetDialog(this).apply {
      val sheetView = layoutInflater.inflate(R.layout.dialog_add_asset, rootView, false)
      val codeView = sheetView.findViewById<EditText>(R.id.customAssetCode)
      val issuerView = sheetView.findViewById<EditText>(R.id.customAssetIssuer)
      sheetView.findViewById<Button>(R.id.addAssetButton)?.setOnClickListener {
        val code = codeView?.text.toString()
        val issuer = issuerView?.text.toString()
        if (code.isNotEmpty() && issuer.isNotEmpty()) {
          try {
            val asset = AssetUtils.createNonNativeAsset(code, issuer)
            changeTrustline(asset, false)
            bottomSheet.dismiss()
          } catch (ex: Exception) {
            toast("Invalid input for code or issuer")
          }
        } else {
          toast("Fields cannot be empty")
        }
      }

      setContentView(sheetView)
    }
  }

  private fun openSettings() {
    startActivity(SettingsActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun refreshAssets() {
    swipeRefresh.isRefreshing = true

    AccountRepository.refresh()
  }

  private fun checkPreloadedAsset(): Boolean {
    val ngnt = DmcApp.wallet.getBalances().find {
      it.assetCode.equals(Constants.NGNT_ASSET_CODE, true) &&
        it.assetIssuer.accountId == Constants.NGNT_ASSET_ISSUER
    }

    val btc = DmcApp.wallet.getBalances().find {
      it.assetCode.equals(Constants.BTC_ASSET_CODE, true) &&
        it.assetIssuer.accountId == Constants.BTC_ASSET_ISSUER
    }

    return when {
      ngnt == null -> {
        changeTrustline(Asset.createNonNativeAsset(Constants.NGNT_ASSET_CODE,
          KeyPair.fromAccountId(Constants.NGNT_ASSET_ISSUER)), false)
        false
      }
      btc == null -> {
        changeTrustline(Asset.createNonNativeAsset(Constants.BTC_ASSET_CODE,
          KeyPair.fromAccountId(Constants.BTC_ASSET_ISSUER)), false)
        false
      }
      else -> true
    }
  }

  private fun updateViewForActive() {
    swipeRefresh?.isRefreshing = false

    fundingState.visibility = View.GONE
    assetsRecyclerView.visibility = View.VISIBLE
  }

  private fun updateViewForFunding() {
    swipeRefresh?.isRefreshing = false

    totalBalance = 0.0
    updateTotalBalanceView()

    assetsRecyclerView.visibility = View.GONE
    fundingState.visibility = View.VISIBLE

    val publicId = DmcApp.wallet.getStellarAccountId() ?: ""
    addressEditText.text = publicId
    addressCopyButton.setOnClickListener {
      ViewUtils.copyToClipBoard(this, publicId, "Account Id",
        R.string.address_copied_message)
    }

    generateQRCode(publicId, qrCode, 500)

    if (DmcApp.wallet.isRegistered()) {
      openAccountButton?.visibility = View.GONE
    } else {
      openAccountButton?.visibility = View.VISIBLE
      openAccountButton?.setOnClickListener {
        startActivityForResult(OpenAccountActivity.newInstance(this), RC_CREATE_ACC)
      }
    }
  }

  private fun updateViewForError() {
    swipeRefresh?.isRefreshing = false
    totalBalanceView?.setText(R.string.error)
  }
  //endregion


  //region Assets Callbacks
  override fun changeTrustline(asset: Asset, isRemove: Boolean) {
    swipeRefresh?.isRefreshing = true
    val secretSeed = AccountUtils.getSecretSeed(this)
    if (NetworkUtils(this).isNetworkAvailable()) {
      Horizon.getChangeTrust(object : SuccessErrorCallback {
        override fun onSuccess() {
          refreshAssets()
        }

        override fun onError(error: HorizonException) {
          toast(error.message(this@ManageAssetsActivity))
          swipeRefresh?.isRefreshing = false
        }
      }, asset, isRemove, secretSeed).execute()
    } else {
      NetworkUtils(this).displayNoNetwork()
      swipeRefresh?.isRefreshing = false
    }
  }

  override fun assetSelected(assetCode: String, assetIssuer: String) {
    startActivity(AssetActivity.newInstance(this, assetCode, assetIssuer))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  override fun customizeWallet() {
    enableCustomization()
  }

  private fun enableCustomization() {
    isCustomizing = true
    adapter.enableCustomization(true)
    assetsRecyclerView.smoothScrollBy(0, 0)
    totalBalanceLabel.setText(R.string.reporting_currency)
    totalBalanceView.visibility = View.GONE
    reportingCurrency.visibility = View.VISIBLE
    reportingCurrency.text = reportingAsset.code
    reportingCurrency.setOnClickListener {
      ViewUtils.showReportingCurrencyDialog(this, this)
    }

    accountButton.setImageResource(R.drawable.ic_check_white)
    swipeRefresh.isEnabled = false
    accountButton.setOnClickListener {
      disableCustomization()
    }
  }

  private fun disableCustomization() {
    isCustomizing = false
    swipeRefresh.isEnabled = true
    adapter.enableCustomization(false)
    totalBalanceLabel.setText(R.string.total_balance)
    totalBalanceView.visibility = View.VISIBLE
    reportingCurrency.visibility = View.GONE
    accountButton.setImageResource(R.drawable.ic_settings)
    accountButton.setColorFilter(ContextCompat.getColor(this, R.color.whiteMain))
    accountButton.setOnClickListener {
      openSettings()
    }
  }

  private fun generateQRCode(data: String, imageView: ImageView, size: Int) {
    val barcodeEncoder = BarcodeEncoder()
    val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
    imageView.setImageBitmap(bitmap)
  }

  override fun tradeAssets() {
    startActivity(ExchangeActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  override fun addCustomAsset() {
    bottomSheet.show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == RC_ADD_CUSTOM && resultCode == RESULT_OK) {
      refreshAssets()
    } else if (requestCode == RC_CREATE_ACC && resultCode == RESULT_OK) {
      refreshAssets()
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onBackPressed() {
    if (isCustomizing) {
      disableCustomization()
    } else {
      super.onBackPressed()
    }
  }
  //endregion

  override fun onAssetSelected(asset: DataAsset) {
    reportingCurrency?.text = asset.code
    reportingCurrencyLogo?.setImageResource(AssetUtils.getLogo(asset.code))

    AssetUtils.saveDateAssetToPrefs(this, asset)

    reportingAsset = asset

    calculateTotalBalance()
  }

  private fun calculateTotalBalance() {
    // Some requests are pending so skip it
    if (calculationQueue != 0) return

    // Reporting currency balance
    totalBalance = DmcApp.wallet.getBalances().find {
      AssetUtils.isReporting(this, it)
    }?.balance?.toDouble() ?: 0.0

    if (reportingAsset.type == "native") {
      // If reporting currency is Lumen XLM, we need to convert all the other balances
      // directly with the latest bid price
      DmcApp.wallet.getBalances().filterNot {
        AssetUtils.isReporting(this, it)
      }.forEach {
        val dataAsset = AssetUtils.toDataAssetFrom(it.asset)
        Timber.d("Loading order book %s %s", it.assetCode, reportingAsset)

        calculationQueue++

        Horizon.getOrderBook(object : Horizon.OnOrderBookListener {
          override fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
            if (bids.isNotEmpty()) {
              Timber.d("${bids.size} bids in the order book")
              val reportingBalance = (it.balance?.toDouble() ?: 0.0) * bids[0].price.toDouble()
              totalBalance += reportingBalance
            } else {
              Timber.d("No bids in the order book")
            }

            decreaseCalculationQueue()
          }

          override fun onFailed(errorMessage: String) {
            Timber.d("failed to load the order book %s", errorMessage)
            decreaseCalculationQueue()
          }

        }, reportingAsset, dataAsset!!)
      }
    } else {
      // If reporting currency is not XLM, we need to know price of the asset in XLM
      // so then we can convert all other assets into XLM and then into reporting currency
      DmcApp.wallet.getBalances().find { it.assetType == "native" }?.let { xlmBalance ->
        Timber.d("Loading order book %s %s", reportingAsset.code, xlmBalance.assetCode)

        calculationQueue++

        val xlmDataAsset = AssetUtils.toDataAssetFrom(xlmBalance.asset)
        Horizon.getOrderBook(object : Horizon.OnOrderBookListener {
          var priceInXlm = 0.0
          override fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
            if (bids.isNotEmpty()) {
              Timber.d("${bids.size} bids in the order book")
              priceInXlm = bids[0].price.toDouble()
              val reportingBalance = (xlmBalance.balance?.toDouble() ?: 0.0) * priceInXlm
              totalBalance += reportingBalance

              // Okay now we have a price in XLM for reporting currency
              // and converted XLM balance already and we can go through all the other
              // assets and convert them to XLM and then to reporting currency
              DmcApp.wallet.getBalances().filter { filter ->
                !AssetUtils.isReporting(this@ManageAssetsActivity, filter) &&
                  filter.assetType != "native"
              }.forEach { balance ->
                val dataAsset = AssetUtils.toDataAssetFrom(balance.asset)

                Timber.d("Loading order book %s %s", dataAsset, xlmBalance)

                calculationQueue++

                Horizon.getOrderBook(object : Horizon.OnOrderBookListener {
                  override fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
                    if (bids.isNotEmpty()) {
                      Timber.d("${bids.size} bids in the order book")
                      // Now we have balance of currency converted to XLM
                      val balanceVal = (balance.balance?.toDouble()
                        ?: 0.0) * bids[0].price.toDouble()
                      // And now we convert it to reporting currency with price in XLM
                      totalBalance += (balanceVal * priceInXlm)
                    } else {
                      Timber.d("No bids in the order book")
                    }

                    decreaseCalculationQueue()
                  }

                  override fun onFailed(errorMessage: String) {
                    Timber.d("failed to load the order book %s", errorMessage)
                    decreaseCalculationQueue()
                  }
                }, xlmDataAsset!!, dataAsset!!)
              }
            } else {
              Timber.d("No bids in the order book")
            }

            decreaseCalculationQueue()
          }

          override fun onFailed(errorMessage: String) {
            Timber.d("failed to load the order book %s", errorMessage)
            decreaseCalculationQueue()
          }
        }, reportingAsset, xlmDataAsset!!)
      }
    }

    updateTotalBalanceView()
  }

  private fun decreaseCalculationQueue() {
    calculationQueue--
    if (calculationQueue == 0) {
      // Ok all balances are calculated - now we can update the view
      updateTotalBalanceView()
    }
  }

  private fun updateTotalBalanceView() {
    runOnUiThread {
      when {
        isCustomizing -> {
          totalBalanceProgress?.visibility = View.GONE
          totalBalanceView?.visibility = View.GONE
        }
        calculationQueue > 0 -> {
          // Calculating in process
          totalBalanceProgress?.visibility = View.VISIBLE
          totalBalanceView?.visibility = View.GONE
        }
        else -> {
          totalBalanceProgress?.visibility = View.GONE
          totalBalanceView?.visibility = View.VISIBLE

          val newBalanceAmount = "${StringFormat.truncateDecimalPlaces(totalBalance, 2)} ${reportingAsset.code}"
          totalBalanceView?.text = newBalanceAmount
        }
      }
    }
  }
  //endregion

  //region Stellar address listener
  override fun onCancelled(error: DatabaseError) = Unit

  override fun onDataChange(data: DataSnapshot) {
    // TODO: MOVE EARLIER TO LOGIN
    Firebase.removeStellarAddressListener(this)
    val address = DmcApp.wallet.getStellarAccountId()
    if (address.isNullOrEmpty()) {
      finishWithToast("Stellar address is NULL")
    } else if (!data.exists() || data.getValue(String::class.java).isNullOrBlank()) {
      // User doesn't have stellar address attached yet so we add it to Firebase
      Firebase.updateStellarAddress(address)
    } else if (address != data.getValue(String::class.java)) {
      // Address not matching show error and restart
      ViewUtils.showWrongWalletDialog(this)
    }
  }

  // Sets inflation pool if haven't set before
  private fun checkInflation() {
    if (Preferences.isInflationSet(this)) {
      Timber.d("Inflation destination is set already")
      return
    }

    if (!NetworkUtils(this).isNetworkAvailable()) {
      Timber.d("Inflation destination cannot be set - No internet")
      return
    }

    val secretSeed = AccountUtils.getSecretSeed(this)

    Horizon.getJoinInflationDestination(object : SuccessErrorCallback {
      override fun onSuccess() {
        Timber.d("Inflation destination set successfully")
        Preferences.inflationSet(this@ManageAssetsActivity)
      }

      override fun onError(error: HorizonException) {
        // Nothing we will try it another time
        Timber.e("Inflation destination set failed")
      }
    }, secretSeed, Constants.LUMENAUT_POOL).execute()

  }
  //endregion
}
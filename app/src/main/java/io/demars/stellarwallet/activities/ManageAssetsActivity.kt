package io.demars.stellarwallet.activities

import android.app.Activity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.interfaces.AssetListener
import kotlinx.android.synthetic.main.activity_manage_assets.*
import org.stellar.sdk.Asset
import android.content.Intent
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.ViewUtils

class ManageAssetsActivity : BaseActivity(), AssetListener {

  //region Properties
  companion object {
    const val RC_ADD_CUSTOM = 111
    const val RC_CREATE_ACC = 222
  }

  private lateinit var adapter: AssetsAdapter
  private var isCustomizing = false
  private var totalBalance = "36,544.85"
  private var reportingAsset = "XLM"
  //endregion

  //region Init
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_manage_assets)
    initUI()
    initAssets()
    refreshAssets()
  }

  private fun initUI() {
    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      refreshAssets()
    }

    accountButton.setOnClickListener {
      openSettings()
    }

    totalBalanceLabel.setText(R.string.total_balance)
    totalBalanceView.text = String.format("%s%s", totalBalance, reportingAsset)
  }

  private fun initAssets() {
    adapter = AssetsAdapter(this)
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    assetsRecyclerView.adapter = adapter
  }

  private fun openSettings() {
    startActivity(SettingsActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun refreshAssets() {
    swipeRefresh.isRefreshing = true
    totalBalanceView.setText(R.string.refreshing)
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> { response ->
      swipeRefresh?.isRefreshing = false
      when (response.httpCode) {
        200 -> {
          //Funded account
          updateViewForActive()
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

  private fun updateViewForActive() {
    totalBalance = DmcApp.wallet.getBalances()
      .find { it.assetType == "native" }?.balance ?: "0.00" + "XLM"
    totalBalanceView.text = totalBalance

    fundingState.visibility = View.GONE
    assetsRecyclerView.visibility = View.VISIBLE

    adapter.refreshAdapter()
  }

  private fun updateViewForFunding() {
    totalBalance = "0.00XLM"
    totalBalanceView.text = totalBalance

    assetsRecyclerView.visibility = View.GONE
    fundingState.visibility = View.VISIBLE

    val publicId = DmcApp.wallet.getStellarAccountId() ?: ""
    addressTextView.text = publicId
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
        startActivityForResult(CreateUserActivity.newInstance(this), RC_CREATE_ACC)
      }
    }
  }

  private fun updateViewForError() {
    totalBalance = "ERROR"
    totalBalanceView.text = totalBalance
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
          if (isRemove) {
            toast(R.string.asset_removed)
          } else {
            toast(R.string.asset_added)
          }
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

  override fun assetSelected(assetCode: String) {
    startActivity(AssetActivity.newInstance(this, assetCode))
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
    totalBalanceView.text = ""
    reportingCurrency.visibility = View.VISIBLE
    reportingCurrency.text = reportingAsset
    accountButton.setImageResource(R.drawable.ic_check_white)
    swipeRefresh.isEnabled = false
    accountButton.setOnClickListener {
      disableCustomization()
    }
  }

  private fun disableCustomization() {
    isCustomizing = false
    adapter.enableCustomization(false)
    totalBalanceLabel.setText(R.string.total_balance)
    totalBalanceView.text = String.format("%s%s", totalBalance, reportingAsset)
    reportingCurrency.visibility = View.GONE
    accountButton.setImageResource(R.drawable.ic_settings)
    accountButton.setColorFilter(ContextCompat.getColor(this, R.color.whiteMain))
    accountButton.setOnClickListener {
      openSettings()
    }
    swipeRefresh.isEnabled = true
  }

  private fun generateQRCode(data: String, imageView: ImageView, size: Int) {
    val barcodeEncoder = BarcodeEncoder()
    val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
    imageView.setImageBitmap(bitmap)
  }

  override fun addCustomAsset() {
    startActivityForResult(AddAssetActivity.newInstance(this), RC_ADD_CUSTOM)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == RC_ADD_CUSTOM && resultCode == RESULT_OK) {
      refreshAssets()
    } else if (requestCode == RC_CREATE_ACC && resultCode == Activity.RESULT_OK) {
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
}
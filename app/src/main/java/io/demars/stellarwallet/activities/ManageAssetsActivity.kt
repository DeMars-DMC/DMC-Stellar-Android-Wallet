package io.demars.stellarwallet.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.interfaces.AssetListener
import kotlinx.android.synthetic.main.activity_manage_assets.*
import org.stellar.sdk.Asset
import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import kotlinx.android.synthetic.main.activity_manage_assets.assetsRecyclerView

class ManageAssetsActivity : BaseActivity(), AssetListener {

  //region Properties
  companion object {
    const val RC_ADD_CUSTOM = 111
  }

  private lateinit var adapter: AssetsAdapter
  private var isCustomizing = false
  private var totalBalanceStr = "36,544.85"
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
    totalBalanceView.text = String.format("%s%s", totalBalanceStr, reportingAsset)
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
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      adapter.refreshAdapter()
      swipeRefresh?.isRefreshing = false
    })
  }
  //endregion

  //region Supported Assets

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
    assetsRecyclerView.smoothScrollBy(0,0)
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
    totalBalanceView.text = String.format("%s%s", totalBalanceStr, reportingAsset)
    reportingCurrency.visibility = View.GONE
    accountButton.setImageResource(R.drawable.ic_settings)
    accountButton.setColorFilter(ContextCompat.getColor(this, R.color.whiteMain))
    accountButton.setOnClickListener {
      openSettings()
    }
    swipeRefresh.isEnabled = true
  }

  override fun addCustomAsset() {
    startActivityForResult(AddAssetActivity.newInstance(this), RC_ADD_CUSTOM)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == RC_ADD_CUSTOM && resultCode == RESULT_OK) {
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
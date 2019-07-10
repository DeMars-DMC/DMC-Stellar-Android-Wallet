package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import kotlinx.android.synthetic.main.activity_manage_assets.*
import org.stellar.sdk.Asset
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Observer
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.DefaultAsset
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_manage_assets.assetsRecyclerView

class ManageAssetsActivity : BaseActivity(), AssetListener {

  //region Properties
  companion object {
    const val RC_ADD_CUSTOM = 111
  }

  private var appBarOffset = 0
  private lateinit var adapter: AssetsAdapter
  //endregion

  //region Init
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_manage_assets)
    initAppBar()
    initUI()
    initAssets()
    fetchAssets()
  }

  private fun initAppBar() {
    appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
      appBarOffset = -offset
      val scrollRange = appBar.totalScrollRange
      val bottomAlpha = 1f - (appBarOffset * 2f) / scrollRange

      walletDivider.scaleX = 1f + 0.2f * (appBarOffset.toFloat() / scrollRange)
      totalBalanceContainer.translationY = appBarOffset.toFloat()
    })
  }

  private fun initUI() {
    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      fetchAssets()
    }

    accountButton.setOnClickListener {
      startActivity(SettingsActivity.newInstance(this))
      overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
    }
  }

  private fun initAssets() {
    adapter = AssetsAdapter(this)
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    assetsRecyclerView.adapter = adapter
  }


  private fun fetchAssets() {
    swipeRefresh.isRefreshing = true
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      updateAdapter()
    })
  }

  private fun updateAdapter() {
    adapter.updateAdapter()
    swipeRefresh?.isRefreshing = false
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
          fetchAssets()
          if (isRemove) {
            toast(R.string.asset_removed)
          } else {
            toast(R.string.asset_added)
          }
          swipeRefresh?.isRefreshing = false
          if (isRemove) {
            DmcApp.userSession.setSessionAsset(DefaultAsset())
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

  override fun deposit(assetCode: String) {
    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://bit.ly/XLMCEX")))
      }
      Constants.DMC_ASSET_TYPE -> {
        // TODO: TRADE DMC

      }
      else -> {
        if (DmcApp.wallet.isVerified()) {
          startActivity(DepositActivity.newInstance(
            this, DepositActivity.Mode.DEPOSIT, assetCode))
        } else {
          ViewUtils.showToast(this, R.string.deposit_not_verified)
        }
      }
    }

  }

  override fun withdraw(assetCode: String) {
    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        if (DmcApp.wallet.getBalances().isNotEmpty() &&
          AccountUtils.getTotalBalance(Constants.LUMENS_ASSET_CODE).toDouble() > 1.0) {
          startActivity(Intent(this, InflationActivity::class.java))
        } else {
          showBalanceErrorDialog()
        }
      }
      Constants.DMC_ASSET_TYPE -> {
        // TODO: LEARN DMC
      }
      else -> {
        if (DmcApp.wallet.isVerified()) {
          startActivity(DepositActivity.newInstance(
            this, DepositActivity.Mode.WITHDRAW, assetCode))
        } else {
          ViewUtils.showToast(this, R.string.withdraw_not_verified)
        }

      }
    }
  }

  override fun addCustomAsset() {
    startActivityForResult(AddAssetActivity.newInstance(this), RC_ADD_CUSTOM)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == RC_ADD_CUSTOM && resultCode == RESULT_OK) {
      fetchAssets()
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }
  //endregion

  private fun showBalanceErrorDialog() {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.no_balance_dialog_title))
      .setMessage(getString(R.string.no_balance_text_message))
      .setPositiveButton(getString(R.string.ok)) { _, _ -> }.show()
  }
}
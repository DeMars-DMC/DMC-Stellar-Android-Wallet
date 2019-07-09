package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import io.demars.stellarwallet.models.SessionAsset
import io.demars.stellarwallet.models.SupportedAsset
import io.demars.stellarwallet.models.SupportedAssetType
import kotlinx.android.synthetic.main.activity_manage_assets.*
import org.stellar.sdk.Asset
import org.stellar.sdk.responses.AccountResponse
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Observer
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.ViewUtils

class ManageAssetsActivity : BaseActivity(), AssetListener {

  //region Properties
  private var map: LinkedHashMap<String, SupportedAsset> = LinkedHashMap()
  private var assetsList: ArrayList<Any> = ArrayList()
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
      val toolbarHeight = toolbar.height
      val scrollRange = appBar.totalScrollRange
      if (appBarOffset in 0..toolbarHeight) {
        walletLogo.alpha = 1f - (appBarOffset * 1.2f) / toolbarHeight
        val balanceScale = 1f - 0.3f * (appBarOffset.toFloat() / toolbarHeight)
        totalBalanceContainer.scaleX = balanceScale
        totalBalanceContainer.scaleY = balanceScale
        totalBalanceContainer.translationY = 0f
      } else {
        walletLogo.alpha = 0f
        totalBalanceContainer.translationY = appBarOffset.toFloat() - toolbarHeight
      }

      val bottomAlpha = 1f - (appBarOffset * 2f) / scrollRange
      arrayOf(tradeButton, tradeLabel, reportingCurrency,
        reportingCurrencyLabel, detailsButton, detailsLabel).forEach {
        it.alpha = bottomAlpha
      }

      walletDivider.scaleX = 1f + 0.2f * (appBarOffset.toFloat() / scrollRange)
    })
  }

  private fun initUI() {
    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      fetchAssets()
    }

    receiveButton.setOnClickListener {
      startActivity(ReceiveActivity.newInstance(this))
      overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
    }

    accountButton.setOnClickListener {
      startActivity(SettingsActivity.newInstance(this))
      overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
    }
  }

  private fun initAssets() {
    adapter = AssetsAdapter(this, this, assetsList)
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    assetsRecyclerView.adapter = adapter
  }


  private fun fetchAssets() {
    swipeRefresh.isRefreshing = true
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      loadSupportedAssets()
    })
  }

  private fun updateAdapter() {
    assetsList.clear()
    assetsList.addAll(convertBalanceToSupportedAsset(WalletApplication.wallet.getBalances(), map))
    val filteredList = getFilteredSupportedAssets(map)
    if (filteredList.isNotEmpty()) {
      assetsList.add(getString(R.string.supported_assets_header))
      assetsList.addAll(filteredList)
    }

    adapter.notifyDataSetChanged()
    swipeRefresh?.isRefreshing = false
  }
  //endregion

  //region Supported Assets
  private fun convertBalanceToSupportedAsset(balances: Array<AccountResponse.Balance>,
                                             supportedAssetsMap: Map<String, SupportedAsset>): List<SupportedAsset> {

    val lumenSupportedAsset = SupportedAsset(0, Constants.LUMENS_ASSET_CODE, Constants.LUMENS_IMAGE_RES,
      "", "", Constants.LUMENS_ASSET_CODE, "", "",
      "0", SupportedAssetType.ADDED, null)

    val list = ArrayList<SupportedAsset>()
    list.add(lumenSupportedAsset)

    if (balances.isNotEmpty()) {
      val nullableAssets = balances.map {
        when {
          it.assetType == Constants.LUMENS_ASSET_TYPE -> {
            list[0].amount = it.balance
            return@map null
          }
          supportedAssetsMap.containsKey(it.assetCode.toLowerCase()) -> {
            supportedAssetsMap[it.assetCode.toLowerCase()]?.let { asset ->
              asset.amount = it.balance
              asset.type = SupportedAssetType.ADDED
              asset.asset = it.asset
              return@map asset
            }
          }
          else -> {
            return@map SupportedAsset(0, it.assetCode.toLowerCase(), 0,
              it.assetIssuer.accountId, it.limit, it.assetCode, "",
              "", it.balance, SupportedAssetType.ADDED, it.asset)
          }
        }
      }

      // This cast is guaranteed to succeed
      list.addAll((nullableAssets.filterNotNull()))
    }

    return list
  }

  private fun getFilteredSupportedAssets(map: Map<String, SupportedAsset>): List<SupportedAsset> {
    return map.values.filter { asset ->
      asset.code.toUpperCase() !in WalletApplication.wallet.getBalances().map { it.assetCode }
    }
  }

  private fun loadSupportedAssets() {
    map.clear()

    val dmc = SupportedAsset(0, Constants.DMC_ASSET_TYPE, Constants.DMC_IMAGE_RES,
      Constants.DMC_ASSET_ISSUER, "100000000000",
      Constants.DMC_ASSET_NAME, "", "", null, null, null)

    val zar = SupportedAsset(1, Constants.ZAR_ASSET_TYPE, Constants.ZAR_IMAGE_RES,
      Constants.ZAR_ASSET_ISSUER, "100000000000",
      Constants.ZAR_ASSET_NAME, "", "", null, null, null)

    val ngnt = SupportedAsset(2, Constants.NGNT_ASSET_TYPE, Constants.NGNT_IMAGE_RES,
      Constants.NGNT_ASSET_ISSUER, "100000000000",
      Constants.NGNT_ASSET_NAME, "", "", null, null, null)

    map[Constants.DMC_ASSET_TYPE] = dmc
    map[Constants.ZAR_ASSET_TYPE] = zar
    map[Constants.NGNT_ASSET_TYPE] = ngnt

    updateAdapter()
  }
  //endregion

  //region Assets Callbacks
  override fun changeTrustline(asset: Asset, isRemoveAsset: Boolean) {

  }

  override fun assetSelected(sessionAsset: SessionAsset, image: View, code: View, balance: View) {
    val assetCode = if (sessionAsset.assetCode == "native") "XLM" else sessionAsset.assetCode
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
        if (WalletApplication.wallet.isVerified()) {
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
        if (WalletApplication.wallet.getBalances().isNotEmpty() &&
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
        if (WalletApplication.wallet.isVerified()) {
          startActivity(DepositActivity.newInstance(
            this, DepositActivity.Mode.WITHDRAW, assetCode))
        } else {
          ViewUtils.showToast(this, R.string.withdraw_not_verified)
        }

      }
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
package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.adapters.AssetsRecyclerViewAdapter
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import io.demars.stellarwallet.interfaces.OnLoadAccount
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_assets.*
import org.jetbrains.anko.above
import org.stellar.sdk.Asset
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse


class AssetsActivity : BaseActivity(), AssetListener {
  private var map: LinkedHashMap<String, SupportedAsset> = LinkedHashMap()
  private var assetsList: ArrayList<Any> = ArrayList()
  private lateinit var adapter: AssetsRecyclerViewAdapter

  companion object {
    const val RC_OPEN_ACCOUNT = 111
    fun newInstance(context: Context): Intent {
      return Intent(context, AssetsActivity::class.java)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_assets)

    setupUI()
    loadSupportedAssets()
  }

  private fun setupUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    progressBar.visibility = View.VISIBLE
    bindAdapter()
    updateOpenAccountButton()

    openAccountButton.setOnClickListener {
      startActivityForResult(CreateUserActivity.newInstance(this), RC_OPEN_ACCOUNT)
    }

    manuallyAddAssetButton.setOnClickListener {
      startActivity(Intent(this, AddAssetActivity::class.java))
    }

    setInflationButton.setOnClickListener {
      if (WalletApplication.wallet.getBalances().isNotEmpty() &&
        AccountUtils.getTotalBalance(Constants.LUMENS_ASSET_CODE).toDouble() > 1.0) {
        startActivity(Intent(this, InflationActivity::class.java))
      } else {
        showBalanceErrorDialog()
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when {
      requestCode == RC_OPEN_ACCOUNT && resultCode == RESULT_OK -> updateOpenAccountButton()
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun updateOpenAccountButton() {
    val isRegistered = WalletApplication.wallet.isRegistered()
    openAccountButton.visibility = if (isRegistered) View.GONE else View.VISIBLE
    (assetsRecyclerView.layoutParams as RelativeLayout.LayoutParams)
      .above(if (isRegistered) R.id.manuallyAddAssetButton else R.id.openAccountButton)
  }

  private fun showBalanceErrorDialog() {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.no_balance_dialog_title))
      .setMessage(getString(R.string.no_balance_text_message))
      .setPositiveButton(getString(R.string.ok)) { _, _ -> }.show()
  }

  //region User Interface

  private fun bindAdapter() {
    adapter = AssetsRecyclerViewAdapter(this, this, assetsList)
    assetsRecyclerView.adapter = adapter
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
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
    progressBar.visibility = View.GONE
  }

  override fun onRestart() {
    super.onRestart()
    reloadDataForAdapter()
  }
  //endregion

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
      @Suppress("UNCHECKED_CAST")
      list.addAll((nullableAssets.filter { it != null }) as List<SupportedAsset>)
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

    val rtgs = SupportedAsset(2, Constants.RTGS_ASSET_TYPE, Constants.RTGS_IMAGE_RES,
      Constants.RTGS_ASSET_ISSUER, "100000000000",
      Constants.RTGS_ASSET_NAME, "", "", null, null, null)

    val ngnt = SupportedAsset(3, Constants.NGNT_ASSET_TYPE, Constants.NGNT_IMAGE_RES,
      Constants.NGNT_ASSET_ISSUER, "100000000000",
      Constants.NGNT_ASSET_NAME, "", "", null, null, null)

    map[Constants.DMC_ASSET_TYPE] = dmc
    map[Constants.ZAR_ASSET_TYPE] = zar
    map[Constants.RTGS_ASSET_TYPE] = rtgs
    map[Constants.NGNT_ASSET_TYPE] = ngnt

    updateAdapter()
  }

  //region Call backs
  override fun assetSelected(sessionAsset: SessionAsset) {
    WalletApplication.userSession.setSessionAsset(sessionAsset)
    finish()
  }

  override fun deposit(assetCode: String) {
    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://bit.ly/XLMCEX")))
      }
      Constants.DMC_ASSET_TYPE -> {
        setResult(RESULT_OK)
        finish()
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
    if (WalletApplication.wallet.isVerified()) {
      startActivity(DepositActivity.newInstance(
        this, DepositActivity.Mode.WITHDRAW, assetCode))
    } else {
      ViewUtils.showToast(this, R.string.withdraw_not_verified)
    }
  }

  override fun changeTrustline(asset: Asset, isRemoveAsset: Boolean) {
    progressBar.visibility = View.VISIBLE
    val secretSeed = AccountUtils.getSecretSeed(this)
    changeTrustLine(secretSeed, asset, isRemoveAsset)
  }

  private fun changeTrustLine(secretSeed: CharArray, assetToChange: Asset, isRemove: Boolean) {
    if (NetworkUtils(this).isNetworkAvailable()) {
      Horizon.getChangeTrust(object : SuccessErrorCallback {
        override fun onSuccess() {
          reloadDataForAdapter()
          if (isRemove) {
            Toast.makeText(this@AssetsActivity, getString(R.string.asset_removed), Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this@AssetsActivity, getString(R.string.success_trustline_changed), Toast.LENGTH_SHORT).show()
          }
          progressBar.visibility = View.GONE
          if (isRemove) {
            WalletApplication.userSession.setSessionAsset(DefaultAsset())
          }
        }

        override fun onError(error: HorizonException) {
          Toast.makeText(this@AssetsActivity, error.message(this@AssetsActivity), Toast.LENGTH_SHORT).show()
          progressBar.visibility = View.GONE
        }
      }, assetToChange, isRemove, secretSeed).execute()
    } else {
      NetworkUtils(this).displayNoNetwork()
      progressBar.visibility = View.GONE
    }
  }

  fun reloadDataForAdapter() {
    if (NetworkUtils(this).isNetworkAvailable()) {
      Horizon.getLoadAccountTask(object : OnLoadAccount {

        override fun onLoadAccount(result: AccountResponse?) {
          if (result != null) {
            WalletApplication.wallet.setBalances(result.balances)
            updateAdapter()
          }
        }

        override fun onError(error: ErrorResponse) {
          ViewUtils.showToast(this@AssetsActivity, R.string.error_supported_assets_message)
        }
      }).execute()
    }
  }
  //endregion
}

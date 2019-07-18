package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.adapters.AssetsAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import io.demars.stellarwallet.interfaces.OnLoadAccount
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.models.ui.DmcAsset
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
  private var map: LinkedHashMap<String, DmcAsset> = LinkedHashMap()
  private var assetsList: ArrayList<Any> = ArrayList()
  private lateinit var adapter: AssetsAdapter

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
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when {
      requestCode == RC_OPEN_ACCOUNT && resultCode == RESULT_OK -> updateOpenAccountButton()
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun updateOpenAccountButton() {
    val isRegistered = DmcApp.wallet.isRegistered()
    openAccountButton.visibility = if (isRegistered) View.GONE else View.VISIBLE
    (assetsRecyclerView.layoutParams as RelativeLayout.LayoutParams)
      .above(if (isRegistered) R.id.manuallyAddAssetButton else R.id.openAccountButton)
  }


  //region User Interface

  private fun bindAdapter() {
    adapter = AssetsAdapter(this)
    assetsRecyclerView.adapter = adapter
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
  }

  private fun updateAdapter() {
    assetsList.clear()
    assetsList.addAll(convertBalanceToSupportedAsset(DmcApp.wallet.getBalances(), map))
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
                                             dmcAssetsMap: Map<String, DmcAsset>): List<DmcAsset> {

    val lumenSupportedAsset = DmcAsset(Constants.LUMENS_ASSET_CODE, Constants.LUMENS_IMAGE_RES,
      "", "", Constants.LUMENS_ASSET_CODE, true, null)

    val list = ArrayList<DmcAsset>()
    list.add(lumenSupportedAsset)

    if (balances.isNotEmpty()) {
      val nullableAssets = balances.map {
        when {
          it.assetType == Constants.LUMENS_ASSET_TYPE -> {
            list[0].amount = it.balance
            return@map null
          }
          dmcAssetsMap.containsKey(it.assetCode.toLowerCase()) -> {
            dmcAssetsMap[it.assetCode.toLowerCase()]?.let { asset ->
              asset.amount = it.balance
              asset.isAdded = true
              asset.asset = it.asset
              return@map asset
            }
          }
          else -> {
            return@map DmcAsset(it.assetCode.toLowerCase(), 0,
              it.assetIssuer.accountId, it.assetCode, it.balance, true, it.asset)
          }
        }
      }

      // This cast is guaranteed to succeed
      list.addAll((nullableAssets.filterNotNull()))
    }

    return list
  }

  private fun getFilteredSupportedAssets(map: Map<String, DmcAsset>): List<DmcAsset> {
    return map.values.filter { asset ->
      asset.code.toUpperCase() !in DmcApp.wallet.getBalances().map { it.assetCode }
    }
  }

  private fun loadSupportedAssets() {
    map.clear()

    val dmc = DmcAsset(Constants.DMC_ASSET_CODE, Constants.DMC_IMAGE_RES,
      Constants.DMC_ASSET_ISSUER, Constants.DMC_ASSET_NAME, "", false, null)

    val zar = DmcAsset(Constants.ZAR_ASSET_CODE, Constants.ZAR_IMAGE_RES,
      Constants.ZAR_ASSET_ISSUER,
      Constants.ZAR_ASSET_NAME, "", false, null)

    val ngnt = DmcAsset(Constants.NGNT_ASSET_CODE, Constants.NGNT_IMAGE_RES,
      Constants.NGNT_ASSET_ISSUER,
      Constants.NGNT_ASSET_NAME, "", false, null)

    map[Constants.DMC_ASSET_CODE] = dmc
    map[Constants.ZAR_ASSET_CODE] = zar
    map[Constants.NGNT_ASSET_CODE] = ngnt

    updateAdapter()
  }

  //region Call backs
  override fun assetSelected(assetCode: String) {
//    DmcApp.userSession.setSessionAsset(sessionAsset)
    finish()
  }

  override fun customizeWallet() {}

  override fun addCustomAsset() {}

  override fun changeTrustline(asset: Asset, isRemove: Boolean) {
    progressBar.visibility = View.VISIBLE
    val secretSeed = AccountUtils.getSecretSeed(this)
    changeTrustLine(secretSeed, asset, isRemove)
  }

  private fun changeTrustLine(secretSeed: CharArray, assetToChange: Asset, isRemove: Boolean) {
    if (NetworkUtils(this).isNetworkAvailable()) {
      Horizon.getChangeTrust(object : SuccessErrorCallback {
        override fun onSuccess() {
          reloadDataForAdapter()
          if (isRemove) {
            Toast.makeText(this@AssetsActivity, getString(R.string.asset_removed), Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this@AssetsActivity, getString(R.string.asset_added), Toast.LENGTH_SHORT).show()
          }
          progressBar.visibility = View.GONE
          if (isRemove) {
            DmcApp.userSession.setSessionAsset(DefaultAsset())
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
            DmcApp.wallet.setBalances(result.balances)
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

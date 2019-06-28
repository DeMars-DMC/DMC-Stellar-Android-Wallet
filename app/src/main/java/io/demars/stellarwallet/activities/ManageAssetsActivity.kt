package io.demars.stellarwallet.activities

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.adapters.AssetsRecyclerViewAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import io.demars.stellarwallet.models.SessionAsset
import io.demars.stellarwallet.models.SupportedAsset
import io.demars.stellarwallet.models.SupportedAssetType
import kotlinx.android.synthetic.main.activity_assets.*
import org.stellar.sdk.Asset
import org.stellar.sdk.responses.AccountResponse

class ManageAssetsActivity : BaseActivity(), AssetListener {

  //region Properties
  private var map: LinkedHashMap<String, SupportedAsset> = LinkedHashMap()
  private var assetsList: ArrayList<Any> = ArrayList()
  private lateinit var adapter: AssetsRecyclerViewAdapter
  //endregion

  //region Init
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_manage_assets)
    setupAssetsRecyclerView()
  }

  private fun setupAssetsRecyclerView() {
    adapter = AssetsRecyclerViewAdapter(this, this, assetsList)
    assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    assetsRecyclerView.adapter = adapter
    loadSupportedAssets()
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

  override fun assetSelected(sessionAsset: SessionAsset) {
  }

  override fun deposit(assetCode: String) {
  }

  override fun withdraw(assetCode: String) {
  }
  //endregion
}
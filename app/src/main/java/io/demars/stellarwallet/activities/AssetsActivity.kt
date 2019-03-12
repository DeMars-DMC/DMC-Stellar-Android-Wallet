package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.adapters.AssetsRecyclerViewAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.ChangeTrustlineListener
import io.demars.stellarwallet.interfaces.OnLoadAccount
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.DefaultAsset
import io.demars.stellarwallet.models.HorizonException
import io.demars.stellarwallet.models.SupportedAsset
import io.demars.stellarwallet.models.SupportedAssetType
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import kotlinx.android.synthetic.main.content_assets_activity.*
import org.stellar.sdk.Asset
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse


class AssetsActivity : BaseActivity(), ChangeTrustlineListener {

    private var map: LinkedHashMap<String, SupportedAsset> = LinkedHashMap()
    private var assetsList: ArrayList<Any> = ArrayList()
    private lateinit var context : Context
    private lateinit var adapter : AssetsRecyclerViewAdapter

    companion object {
        fun newInstance(context: Context) : Intent {
            return Intent(context, AssetsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_assets_activity)

        setupUI()
        loadSupportedAssets()
        context = applicationContext
    }

    private fun setupUI() {
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressBar.visibility = View.VISIBLE
        bindAdapter()
        manuallyAddAssetButton.setOnClickListener {
            startActivity(Intent(this@AssetsActivity, AddAssetActivity::class.java))
        }
    }

    //region User Interface

    private fun bindAdapter() {
        adapter = AssetsRecyclerViewAdapter(this, this, assetsList)
        assetsRecyclerView.adapter = adapter
        assetsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateAdapter() {
        assetsList.clear()
        assetsList.addAll(convertBalanceToSupportedAsset(WalletApplication.wallet.getBalances(), map!!))
        val filteredList = getFilteredSupportedAssets(map!!)
        if (!filteredList.isEmpty()) {
            assetsList.add(getString(R.string.supported_assets_header))
            assetsList.addAll(filteredList)
        }

        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            if (item.itemId == android.R.id.home) {
                finish()
                return true
            }
        }
        return false
    }

    override fun onRestart() {
        super.onRestart()
        reloadDataForAdapter()
    }

    //endregion

    private fun convertBalanceToSupportedAsset(balances: Array<AccountResponse.Balance>,
                                               supportedAssetsMap: Map<String, SupportedAsset>) : List<SupportedAsset> {

        val lumenSupportedAsset = SupportedAsset(0, Constants.LUMENS_ASSET_CODE, Constants.LUMENS_IMAGE_RES,
                "", "", Constants.LUMENS_ASSET_NAME, "", "",
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
                       val asset = supportedAssetsMap[it.assetCode.toLowerCase()]!!
                       asset.amount = it.balance
                       asset.type = SupportedAssetType.ADDED
                       asset.asset = it.asset
                       return@map asset
                   }
                   else -> {
                       val asset = SupportedAsset(0, it.assetCode.toLowerCase(), 0,
                               it.assetIssuer.accountId, it.limit, it.assetCode, "",
                               "", it.balance, SupportedAssetType.ADDED, it.asset)
                       return@map asset
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
        return map.values.filter {
            it.code.toUpperCase() !in WalletApplication.wallet.getBalances().map { it.assetCode }
        }
    }

    private fun loadSupportedAssets() {
        map.clear()

        val rand = SupportedAsset(1, Constants.RAND_ASSET_TYPE, Constants.RAND_IMAGE_RES,
          Constants.RAND_ASSET_ISSUER, "100000000000",
          Constants.RAND_ASSET_NAME, "", "", null, null, null)

        val nkls = SupportedAsset(2, Constants.NKLS_ASSET_TYPE, Constants.NKLS_IMAGE_RES,
          Constants.NKLS_ASSET_ISSUER, "100000000000",
          Constants.NKLS_ASSET_NAME, "", "", null, null, null)

        val rgts = SupportedAsset(3, Constants.RGTS_ASSET_TYPE, Constants.RGTS_IMAGE_RES,
          Constants.RGTS_ASSET_ISSUER, "100000000000",
          Constants.RGTS_ASSET_NAME, "", "", null, null, null)

        map["RAND"] = rand
        map["NKLS"] = nkls
        map["RGTS"] = rgts

        updateAdapter()
    }

    //region Call backs

    override fun changeTrustline(asset: Asset, isRemoveAsset: Boolean) {
        progressBar.visibility = View.VISIBLE
        val secretSeed = AccountUtils.getSecretSeed(context)
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
            Horizon.getLoadAccountTask(object: OnLoadAccount {

                override fun onLoadAccount(result: AccountResponse?) {
                    if (result != null) {
                        WalletApplication.wallet.setBalances(result.balances)
                        updateAdapter()
                    }
                }

                override fun onError(error: ErrorResponse) {
                    //TODO: please address GH-170 and remove the handler here
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, getString(R.string.error_supported_assets_message), Toast.LENGTH_SHORT).show()
                    }
                }
            }).execute()
        }
    }
    //endregion
}

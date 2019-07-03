package io.demars.stellarwallet.activities

import android.os.Bundle
import androidx.core.view.ViewCompat
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.utils.AssetUtils
import kotlinx.android.synthetic.main.activity_asset.*

class AssetActivity : BaseActivity() {

  private lateinit var assetCode: String
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_asset)
    initTransition()
    setupAppBar()
    setupTransactions()
  }

  private fun initTransition() {
    intent.extras?.getString("ARG_ASSET_CODE")?.let {
      assetCode = it
      ViewCompat.setTransitionName(assetLogo, "${assetCode}IMAGE")
      ViewCompat.setTransitionName(assetName, "${assetCode}CODE")
      ViewCompat.setTransitionName(assetBalance, "${assetCode}BALANCE")
    } ?: finishWithToast("Asset code cannot be NULL")
  }

  private fun setupAppBar() {
    assetLogo.setImageResource(AssetUtils.getLogo(assetCode))
    assetBalance.text = AssetUtils.getBalance(assetCode)
    assetName.text = assetCode
  }

  private fun setupTransactions() {

  }
}
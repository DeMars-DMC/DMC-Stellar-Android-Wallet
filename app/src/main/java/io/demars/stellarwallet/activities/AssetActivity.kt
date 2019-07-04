package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.demars.stellarwallet.R
import io.demars.stellarwallet.utils.AssetUtils
import kotlinx.android.synthetic.main.activity_asset.*

class AssetActivity : BaseActivity() {

  companion object {
    const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    fun newInstance(context: Context, assetCode: String): Intent {
      val intent = Intent(context, AssetActivity::class.java)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  private lateinit var assetCode: String
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_asset)
    checkIntent()
    initAppBar()
    initTransactions()
  }

  private fun checkIntent() {
    intent.extras?.getString(ARG_ASSET_CODE)?.let {
      assetCode = it
    } ?: finishWithToast("Asset code cannot be NULL")
  }

  private fun initAppBar() {
    backButton.setOnClickListener { onBackPressed() }
    leftButton.setOnClickListener { }
    assetLogo.setImageResource(AssetUtils.getLogo(assetCode))
    assetBalance.text = AssetUtils.getBalance(assetCode)
    assetName.text = assetCode
  }

  private fun initTransactions() {

  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}
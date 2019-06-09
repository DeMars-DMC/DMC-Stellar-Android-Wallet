package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.demars.stellarwallet.R

class WithdrawActivity: BaseActivity() {
  companion object {
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val MAX_DECIMALS = 2
    fun newInstance(context: Context, assetCode: String): Intent {
      val intent = Intent(context, WithdrawActivity::class.java)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_withdraw)
  }
}
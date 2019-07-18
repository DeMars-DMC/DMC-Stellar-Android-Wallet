package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.demars.stellarwallet.R
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import kotlinx.android.synthetic.main.activity_inflation.*

class InflationActivity : BaseActivity() {

  companion object {
    fun newInstance(context: Context): Intent =
      Intent(context, InflationActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_inflation)

    setupUI()
  }

  private fun setupUI() {
    backButton.setOnClickListener {
      onBackPressed()
    }

    addressEditText.setText(Constants.INFLATION_DESTINATION)

    saveButton.setOnClickListener {

      progressBar.visibility = View.VISIBLE
      val secretSeed = AccountUtils.getSecretSeed(it.context.applicationContext)

      if (NetworkUtils(this).isNetworkAvailable()) {
        Horizon.getJoinInflationDestination(object : SuccessErrorCallback {
          override fun onSuccess() {
            Toast.makeText(this@InflationActivity, getString(R.string.inflation_set_success), Toast.LENGTH_SHORT).show()
            finish()
          }

          override fun onError(error: HorizonException) {
            progressBar.visibility = View.GONE
            Toast.makeText(this@InflationActivity, error.message(this@InflationActivity), Toast.LENGTH_SHORT).show()
          }
        }, secretSeed, addressEditText.text.toString()).execute()
      } else {
        progressBar.visibility = View.GONE
        NetworkUtils(this).displayNoNetwork()
      }
    }


  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}

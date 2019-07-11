package io.demars.stellarwallet.activities

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inflation)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        addressTextView.setText(Constants.INFLATION_DESTINATION)

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
                }, secretSeed, addressTextView.text.toString()).execute()
            } else {
                progressBar.visibility = View.GONE
                NetworkUtils(this).displayNoNetwork()
            }
        }
    }
}

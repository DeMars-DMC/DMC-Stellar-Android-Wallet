package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_bank_account.*

class BankAccountActivity: BaseActivity() {
  companion object {
    fun newInstance(context: Context): Intent = Intent(context, BankAccountActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bank_account)
    setupUI()
  }

  private fun setupUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }
}
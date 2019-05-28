package io.demars.stellarwallet.activities

import android.os.Bundle
import android.view.MenuItem
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_deposit.*

class DepositActivity : BaseActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_deposit)
    setupUI()
  }

  private fun setupUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }
}
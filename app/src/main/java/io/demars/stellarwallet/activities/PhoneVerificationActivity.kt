package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.demars.stellarwallet.R

class PhoneVerificationActivity : AppCompatActivity() {
  companion object {
    fun newInstance(context: Context): Intent {
      return Intent(context, PhoneVerificationActivity::class.java)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_verification_phone)

  }
}
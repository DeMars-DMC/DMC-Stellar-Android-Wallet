package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.R
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_deposit.*
import kotlinx.android.synthetic.main.activity_deposit.toolbar

class DepositActivity : BaseActivity() {
  companion object {
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val RC_BANK_ACCOUNT = 111
    fun newInstance(context: Context, assetCode: String): Intent {
      val intent = Intent(context, DepositActivity::class.java)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  private var dmcUser: DmcUser? = null
  private val userListener = object : ValueEventListener {
    override fun onDataChange(dataSnapshot: DataSnapshot) {
      dataSnapshot.getValue(DmcUser::class.java)?.let { user ->
        onUserFetched(user)
      }?: onError("User is NULL")
    }

    override fun onCancelled(error: DatabaseError) {
      onError(error.message)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_deposit)
    setupToolbar()

    Firebase.getCurrentUser()?.let { _ ->
      Firebase.getUser(userListener)
    }?: onError("User is NULL")
  }

  private fun onError(message: String) {
    ViewUtils.showToast(this, message)
    finish()
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  private fun onUserFetched(user: DmcUser) {
    dmcUser = user
    val fullNameString = "${user.first_name} ${user.last_name}"
    nameText?.text = fullNameString
    phoneText?.text = user.phone
    bankAccountPicker?.setOnClickListener {
      openBankActivity()
    }
  }

  private fun openBankActivity() {
    startActivityForResult(BankAccountActivity.newInstance(this), RC_BANK_ACCOUNT)
  }

  override fun onDestroy() {
    super.onDestroy()
    Firebase.removeUserListener(userListener)
  }
}
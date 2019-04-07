package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import io.demars.stellarwallet.R

class CreateUserActivity: BaseActivity() {
  var uid = ""
  var phone = ""

  companion object {
    private const val REQUEST_CODE_CAMERA = 111
    private const val ARG_UID = "ARG_UID"
    private const val ARG_PHONE = "ARG_PHONE"

    fun newInstance(context: Context, uid : String, phoneNumber: String): Intent {
      val intent = Intent(context, CreateUserActivity::class.java)
      intent.putExtra(ARG_UID, uid)
      intent.putExtra(ARG_PHONE, phoneNumber)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create_user)

    uid = intent.getStringExtra(ARG_UID)
    phone = intent.getStringExtra(ARG_PHONE)
  }

  private fun openCameraActivity() {
    val useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    startActivityForResult(if (useCamera2) Camera2Activity.newInstance(this)
    else CameraActivity.newInstance(this), REQUEST_CODE_CAMERA)
  }
}
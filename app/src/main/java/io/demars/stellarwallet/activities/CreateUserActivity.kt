package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import io.demars.stellarwallet.R
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_create_user.*

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
    ViewUtils.setTransparentStatusBar(this)
    ViewUtils.setActivityKeyboardHidden(this)
    setContentView(R.layout.activity_create_user)

    uid = intent.getStringExtra(ARG_UID)
    phone = intent.getStringExtra(ARG_PHONE)

    initUI()
  }

  private fun initUI(){
    dialogButton.setOnClickListener {
      hideWelcomeDialog()
      ViewUtils.showKeyboard(this, firstNameInput)
    }

    personalIdContainer.setOnClickListener {
      openCameraActivity(REQUEST_CODE_CAMERA)
    }
  }
  private fun hideWelcomeDialog() {
    val padding = resources.getDimension(R.dimen.padding_vertical)
    dialogBg.animate().alpha(0F)
    welcomeDialog.animate().translationY(-padding).setDuration(100).withEndAction {
      welcomeDialog.animate().translationY(padding * 100).setDuration(500).withEndAction {
        welcomeDialog.visibility = GONE
        dialogBg.visibility = GONE
      }
    }
  }

  private fun openCameraActivity(requestCode: Int) {
    val useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    startActivityForResult(if (useCamera2) Camera2Activity.newInstance(this)
    else CameraActivity.newInstance(this), requestCode)
  }


}
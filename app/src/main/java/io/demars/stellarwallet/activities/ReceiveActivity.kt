package io.demars.stellarwallet.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.demars.stellarwallet.helpers.Constants
import kotlinx.android.synthetic.main.activity_receive.*

class ReceiveActivity : BaseActivity() {
  private var assetCode = ""
  private var assetIssuer = ""

  companion object {
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val ARG_ASSET_ISSUER = "ARG_ASSET_ISSUER"

    fun newInstance(context: Context, assetCode: String, assetIssuer: String): Intent {
      return Intent(context, ReceiveActivity::class.java).apply {
        putExtra(ARG_ASSET_CODE, assetCode)
        putExtra(ARG_ASSET_ISSUER, assetIssuer)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_receive)
    checkIntent()
    initUI()
    initBankDepositButton()
  }

  private fun checkIntent() {
    assetCode = intent?.getStringExtra(ARG_ASSET_CODE) ?: ""
    assetIssuer = intent?.getStringExtra(ARG_ASSET_ISSUER) ?: ""
  }

  private fun initUI() {
    backButton.setOnClickListener { onBackPressed() }
    titleView.text = getString(R.string.pattern_deposit, assetCode)

    val publicId = DmcApp.wallet.getStellarAccountId()
    addressTextView.text = publicId
    generateQRCode(publicId!!, qrImageView, 500)
    addressCopyButton.setOnClickListener { copyAddressToClipBoard(publicId) }
  }

  private fun initBankDepositButton() {
    val bankDepositSupported = (assetCode == Constants.ZAR_ASSET_CODE && assetIssuer == Constants.ZAR_ASSET_ISSUER)
      || (assetCode == Constants.NGNT_ASSET_CODE && assetIssuer == Constants.NGNT_ASSET_ISSUER)
    bankDepositButton.visibility = if (bankDepositSupported) View.VISIBLE else View.GONE
    bankDepositButton.setOnClickListener {
      startActivity(DepositActivity.newInstance(this,
        DepositActivity.Mode.DEPOSIT, assetCode, assetIssuer))
    }
  }

  private fun generateQRCode(data: String, imageView: ImageView, size: Int) {
    val barcodeEncoder = BarcodeEncoder()
    val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
    imageView.setImageBitmap(bitmap)
  }

  private fun copyAddressToClipBoard(data: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("DMC Address", data)
    clipboard.primaryClip = clip

    Toast.makeText(this, getString(R.string.address_copied_message), Toast.LENGTH_LONG).show()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}

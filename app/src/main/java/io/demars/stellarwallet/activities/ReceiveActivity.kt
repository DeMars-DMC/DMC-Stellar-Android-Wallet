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
import io.demars.stellarwallet.utils.AssetUtils
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
    generateQRCode(publicId!!, qrImageView)
    addressCopyButton.setOnClickListener { copyAddressToClipBoard(publicId) }
    shareButton.setOnClickListener { shareAddress(publicId) }
  }

  private fun initBankDepositButton() {
    val bankDepositSupported = AssetUtils.isZar(assetCode, assetIssuer)
      || AssetUtils.isNgnt(assetCode, assetIssuer) || AssetUtils.isEurt(assetCode, assetIssuer)

    val directDepositSupported = AssetUtils.isBtc(assetCode, assetIssuer)
      || AssetUtils.isEth(assetCode, assetIssuer)

    when {
      bankDepositSupported -> {
        depositButton.visibility = View.VISIBLE
        depositTitle.setText(R.string.bank_deposit)
      }
      directDepositSupported -> {
        depositButton.visibility = View.VISIBLE
        depositTitle.setText(R.string.direct_deposit)
      }
      else -> depositButton.visibility = View.GONE
    }

    depositButton.setOnClickListener {
      startActivity(DepositActivity.newInstance(this,
        DepositActivity.Mode.DEPOSIT, assetCode, assetIssuer))
    }
  }

  private fun generateQRCode(data: String, imageView: ImageView, size: Int = 500) {
    val barcodeEncoder = BarcodeEncoder()
    val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
    imageView.setImageBitmap(bitmap)
  }

  private fun copyAddressToClipBoard(address: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("DMC Address", address)
    clipboard.primaryClip = clip

    Toast.makeText(this, getString(R.string.address_copied_message), Toast.LENGTH_LONG).show()
  }

  private fun shareAddress(address: String) {
    val sharingIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Stellar Address")
      putExtra(Intent.EXTRA_TEXT, address)
    }

    startActivity(Intent.createChooser(sharingIntent, resources.getString(R.string.share_address)))
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}

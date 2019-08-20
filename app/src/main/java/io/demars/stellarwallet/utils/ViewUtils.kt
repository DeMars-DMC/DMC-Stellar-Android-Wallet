package io.demars.stellarwallet.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.R
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.interfaces.OnAssetSelected
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream


object ViewUtils {

  //region Toast
  @JvmStatic
  fun showToast(context: Context?, message: String) {
    if (context == null) return
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
  }

  @JvmStatic
  fun showToast(context: Context?, messageRes: Int) {
    if (context == null) return
    Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
  }
  //endregion

  //region Keyboard
  @JvmStatic
  fun hideKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = activity.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
      view = View(activity)
    }

    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }

  @JvmStatic
  fun showKeyboard(context: Context, editText: View?) {
    editText?.let {
      it.requestFocus()
      val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
      imm!!.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
    }
  }
  //endregion

  //region Status bar
  @JvmStatic
  fun setTransparentStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= 19) {
      activity.window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    if (Build.VERSION.SDK_INT in 19..20) {
      activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    if (Build.VERSION.SDK_INT >= 21) {
      activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      activity.window.statusBarColor = Color.TRANSPARENT
    }
  }
  //endregion

  //region Dialogs
  @JvmStatic
  fun showWrongWalletDialog(activity: FragmentActivity) {
    val builder = AlertDialog.Builder(activity)
    builder.setTitle(R.string.non_matching_wallet_title)
      .setMessage(R.string.non_matching_wallet_message)
      .setPositiveButton(R.string.try_again) { _, _ ->
        GlobalGraphHelper.wipeAndRestart(activity)
      }
      .setNeutralButton(R.string.contact_us) { _, _ ->
        val uid = Firebase.getCurrentUserUid()
        GlobalGraphHelper.wipeAndRestart(activity)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@demars.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Cannot access to Stellar Wallet: UID - $uid")
        activity.startActivity(Intent.createChooser(intent, "Send email to DMC support"))
      }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
  }

  @JvmStatic
  fun showReportingCurrencyDialog(activity: FragmentActivity, onAssetSelected: OnAssetSelected) {
    val balances = DmcApp.wallet.getBalances()
      .filter { !AssetUtils.isReporting(activity, it) }

    val codes = balances
      .map { it.assetCode ?: "XLM" }.toTypedArray()
    AlertDialog.Builder(activity)
      .setTitle(R.string.select_reporting_currency)
      .setItems(codes) { _, which ->
        AssetUtils.toDataAssetFrom(balances[which].asset)?.let {
          onAssetSelected.onAssetSelected(it)
        }
      }.show()
  }

  @JvmStatic
  fun showDialog(context: Context?, title: Int, message: Int,
                 negativeText: Int, positiveText: Int,
                 onPositive: DialogInterface.OnClickListener) {
    if (context == null) return

    val builder = AlertDialog.Builder(context)
    if (title != 0) {
      builder.setTitle(title)
    }

    if (message != 0) {
      builder.setMessage(message)
    }

    if (positiveText != 0) {
      builder.setPositiveButton(positiveText, onPositive)

    }

    if (negativeText != 0) {
      builder.setNegativeButton(negativeText) { dialog, _ ->
        dialog.dismiss()
      }
    }

    builder.show()
  }
  //endregion

  //region Clipboard
  @JvmStatic
  fun copyToClipBoard(context: Context, data: String, label: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, data)
    clipboard.primaryClip = clip
    showToast(context, toastMessage)
  }

  @JvmStatic
  fun copyToClipBoard(context: Context, data: String, label: String, toastMessage: Int) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, data)
    clipboard.primaryClip = clip
    showToast(context, toastMessage)
  }
  //endregion

  //region Bitmap
  fun handleBytes(source: ByteArray, angle: Float): Bitmap {
    val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size)
    val matrix = Matrix()

    matrix.postRotate(angle)
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return makeSquareBitmap(rotatedBitmap)
  }

  private fun makeSquareBitmap(source: Bitmap): Bitmap =
    if (source.width > source.height)
      Bitmap.createBitmap(source, 0, 0, source.height, source.height)
    else Bitmap.createBitmap(source, 0, 0, source.width, source.width)

  fun bitmapToBytes(bitmap: Bitmap) : ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
  }
  //endregion
}
package io.demars.stellarwallet.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.demars.stellarwallet.activities.CreateUserActivity

object ViewUtils {

  //region Toast
  @JvmStatic
  fun showToast(context: Context?, message: String) {
    if (context == null) return
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JvmStatic
  fun showToast(context: Context?, messageRes: Int) {
    if (context == null) return
    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
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
  fun showKeyboard(context: Context, editText: EditText) {
    editText.requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm!!.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
  }
  //endregion

  //region Status bar
  @JvmStatic
  fun setTransparentStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT in 19..20) {
      activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    if (Build.VERSION.SDK_INT >= 19) {
      activity.window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    if (Build.VERSION.SDK_INT >= 21) {
      activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      activity.window.statusBarColor = Color.TRANSPARENT
    }
  }
  //endregion

  //region Dialogs
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
}
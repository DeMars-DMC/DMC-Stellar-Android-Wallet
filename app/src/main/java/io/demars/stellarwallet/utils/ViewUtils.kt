package io.demars.stellarwallet.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.demars.stellarwallet.activities.CreateUserActivity

object ViewUtils {
//  @JvmStatic
//  fun showFragment(activity: FragmentActivity, fragment: Fragment) {
//    ViewUtils.showFragment(activity, fragment, fromBottom = false, addToStack = true)
//  }
//
//  @JvmStatic
//  fun showFragment(activity: FragmentActivity, fragment: Fragment, sharedView: View?) {
//    ViewUtils.showFragment(activity, fragment, false, true, sharedView)
//  }
//
//  @JvmStatic
//  fun showFragment(
//    activity: FragmentActivity,
//    fragment: Fragment,
//    fromBottom: Boolean,
//    addToStack: Boolean,
//    sharedView: View? = null
//  ) {
//    val fragmentManager = activity.supportFragmentManager
//    val fragmentTransaction = fragmentManager.beginTransaction()
//
//    if (fromBottom) {
//      fragmentTransaction
//        .setCustomAnimations(
//          R.anim.slide_in_bottom, R.anim.alpha_out,
//          R.anim.alpha_in, R.anim.slide_out_bottom
//        )
//    } else {
//      fragmentTransaction
//        .setCustomAnimations(
//          R.anim.slide_in_right, R.anim.slide_out_left,
//          R.anim.slide_in_left, R.anim.slide_out_right
//        )
//    }
//
//    if (addToStack) {
//      fragmentTransaction.addToBackStack(fragment.tag)
//    }
//
//    sharedView?.let {
//      fragmentTransaction.addSharedElement(sharedView, sharedView.transitionName)
//    }
//
//    fragmentTransaction.replace(R.id.mainContainer, fragment).commit()
//  }
//
//  @JvmStatic
//  fun showDialogFragment(activity: FragmentActivity, dialogFragment: DialogFragment) {
//    dialogFragment.show(activity.supportFragmentManager, dialogFragment.tag)
//  }

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

  @JvmStatic
  fun setActivityKeyboardHidden(activity: CreateUserActivity) {
    activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
  }

//  @JvmStatic
//  fun getScreenWidth(context: Context?, includePadding: Boolean): Int {
//    val paddingSum = if (includePadding) 0 else
//      (context?.resources?.getDimensionPixelOffset(R.dimen.padding_horizontal) ?: 0) * 2
//
//    val displayMetrics = DisplayMetrics()
//    (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
//    return displayMetrics.widthPixels - paddingSum
//  }
//
//  @JvmStatic
//  fun getScreenHeight(context: Context?, includePadding: Boolean): Int {
//    val paddingSum = if (includePadding) 0 else
//      (context?.resources?.getDimensionPixelOffset(R.dimen.padding_vertical) ?: 0) * 2
//
//    val displayMetrics = DisplayMetrics()
//    (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
//    return displayMetrics.heightPixels - paddingSum
//  }
//
//  @JvmStatic
//  fun dp2px(dpVal: Float): Int {
//    return TypedValue.applyDimension(
//      TypedValue.COMPLEX_UNIT_DIP,
//      dpVal, Resources.getSystem().displayMetrics
//    ).toInt()
//  }
//
//  @JvmStatic
//  fun setBlackActivity(activity: Activity) {
//    activity.window.statusBarColor = Color.BLACK
//    activity.window.navigationBarColor = Color.BLACK
//
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//      activity.window.decorView.systemUiVisibility = 0
//    }
//  }
//
//  @JvmStatic
//  fun setWhiteActivity(activity: Activity) {
//    activity.window.statusBarColor = Color.WHITE
//    activity.window.navigationBarColor = Color.WHITE
//
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//      activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//    }
//
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//      activity.window.decorView.systemUiVisibility =
//        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//    }
//  }
//
//  @JvmStatic
//  fun setStatusBarColor(activity: Activity, colorRes: Int) {
//    activity.window.statusBarColor = ContextCompat.getColor(activity, colorRes)
//  }
//
//  @JvmStatic
//  fun setNavigationBarColor(activity: Activity, colorRes: Int) {
//    activity.window.navigationBarColor = ContextCompat.getColor(activity, colorRes)
//  }
}
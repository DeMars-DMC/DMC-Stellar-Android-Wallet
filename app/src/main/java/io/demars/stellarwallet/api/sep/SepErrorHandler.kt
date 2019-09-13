package io.demars.stellarwallet.api.sep

import android.app.Activity
import com.google.gson.Gson
import io.demars.stellarwallet.utils.ViewUtils
import okhttp3.ResponseBody

object SepErrorHandler {
  fun handleError(activity: Activity, errorBody: ResponseBody?) {
    if (errorBody == null) {
      ViewUtils.showToast(activity, "UNKNOWN ERROR")
      return
    }

    when (val parsedError = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)) {
      null -> ViewUtils.showToast(activity, "PARSING ERROR FAILED")
      else -> {
        when {
          //Need to complete Customer info form on Sep6 so open in Default Browser
          parsedError.type == "interactive_customer_info_needed" && parsedError.url.isNotEmpty() ->
            Sep6.showInteractiveDialogConfirmation(activity, parsedError.url)
          // Other Errors
          else -> ViewUtils.showToast(activity, parsedError.error)
        }
      }
    }
  }
}
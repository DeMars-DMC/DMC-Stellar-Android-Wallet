package io.demars.stellarwallet.api.sep

import com.google.gson.annotations.SerializedName

class ErrorResponse {
  @SerializedName("type")
  var type = ""
  @SerializedName("url")
  var url = ""
  @SerializedName("error")
  var error = ""
}

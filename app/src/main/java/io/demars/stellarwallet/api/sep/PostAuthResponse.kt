package io.demars.stellarwallet.api.sep

import com.google.gson.annotations.SerializedName

class PostAuthResponse {
  @SerializedName("token")
  var token = ""
  @SerializedName("error")
  var error = ""
}
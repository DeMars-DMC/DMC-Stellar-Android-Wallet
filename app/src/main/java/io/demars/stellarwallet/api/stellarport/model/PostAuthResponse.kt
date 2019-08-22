package io.demars.stellarwallet.api.stellarport.model

import com.google.gson.annotations.SerializedName

class PostAuthResponse {
  @SerializedName("token")
  var token = ""
  @SerializedName("error")
  var error = ""
}
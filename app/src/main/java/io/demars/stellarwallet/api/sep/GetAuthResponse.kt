package io.demars.stellarwallet.api.sep

import com.google.gson.annotations.SerializedName

class GetAuthResponse {
  @SerializedName("transaction")
  var transaction = ""
  @SerializedName("error")
  var error = ""
}
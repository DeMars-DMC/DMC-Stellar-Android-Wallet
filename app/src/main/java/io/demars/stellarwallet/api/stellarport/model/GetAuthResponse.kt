package io.demars.stellarwallet.api.stellarport.model

import com.google.gson.annotations.SerializedName

class GetAuthResponse {
  @SerializedName("transaction")
  var transaction = ""
  @SerializedName("error")
  var error = ""
}
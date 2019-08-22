package io.demars.stellarwallet.api.stellarport.model

import com.google.gson.annotations.SerializedName

class ErrorResponse {
  @SerializedName("type")
  var type = ""
  @SerializedName("url")
  var url = ""
  @SerializedName("interactive_deposit")
  var interactiveDeposit = ""
  @SerializedName("error")
  var error = ""
}

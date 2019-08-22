package io.demars.stellarwallet.api.stellarport.model

import com.google.gson.annotations.SerializedName

class DepositResponse {
  @SerializedName("how")
  var how = ""
  @SerializedName("min_amount")
  var minAmount = -1.0
  @SerializedName("max_amount")
  var maxAmount = -1.0
  @SerializedName("fee_fixed")
  var feeFixed = 0
  @SerializedName("fee_percent")
  var feePercent = 0
  @SerializedName("eta")
  var eta = -1.0
  @SerializedName("extra_info")
  var extraInfo = ExtraInfo()

  class ExtraInfo{
    @SerializedName("message")
    var message = ""
  }
}
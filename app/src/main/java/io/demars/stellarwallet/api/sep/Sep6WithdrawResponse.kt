package io.demars.stellarwallet.api.sep

import com.google.gson.annotations.SerializedName

class  Sep6WithdrawResponse {
  @SerializedName("account_id")
  var accountId = ""
  @SerializedName("memo_type")
  var memoType = ""
  @SerializedName("memo")
  var memo : String? = ""
  @SerializedName("min_amount")
  var minAmount = -1.0
  @SerializedName("fee_fixed")
  var feeFixed = 0.0
  @SerializedName("fee_percent")
  var feePercent = 0.0
  @SerializedName("eta")
  var eta = ""
  @SerializedName("extra_info")
  var extraInfo = ExtraInfo()

  class ExtraInfo{
    @SerializedName("message")
    var message = ""
  }
}
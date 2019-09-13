package io.demars.stellarwallet.api.sep

import com.google.gson.annotations.SerializedName

class Sep6DepositResponse {
  @SerializedName("how")
  var how = ""
  @SerializedName("min_amount")
  var minAmount = -1.0
  @SerializedName("max_amount")
  var maxAmount = -1.0
  @SerializedName("fee_fixed")
  var feeFixed = 0.0
  @SerializedName("fee_percent")
  var feePercent = 0.0
  @SerializedName("eta")
  var eta = ""
  @SerializedName("extra_info")
  var extraInfo = ExtraInfo()

  class ExtraInfo {
    @SerializedName("message")
    var message = ""
    @SerializedName("bank_name")
    var bankName = ""
    @SerializedName("bank_account_name")
    var bankAccountName = ""
    @SerializedName("bank_account_number")
    var bankAccountNumber = ""
    @SerializedName("deposit_ref")
    var depositRef = ""
  }
}
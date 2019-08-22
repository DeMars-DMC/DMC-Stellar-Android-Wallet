package io.demars.stellarwallet.api.cowrie.model

import com.google.gson.annotations.SerializedName

class WithdrawResponse {
  @SerializedName("pair")
  var pair = ""
  @SerializedName("exchange_rate")
  var exchangeRate = 0
  @SerializedName("fee")
  var fee = 0
  @SerializedName("meta")
  var meta = ""
  @SerializedName("address")
  var address = ""
  @SerializedName("eta")
  var eta = ""
}
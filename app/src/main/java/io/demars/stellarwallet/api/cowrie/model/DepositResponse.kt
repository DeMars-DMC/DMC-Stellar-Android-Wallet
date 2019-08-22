package io.demars.stellarwallet.api.cowrie.model

import com.google.gson.annotations.SerializedName

class DepositResponse {
  @SerializedName("pair")
  var pair = ""
  @SerializedName("exchange_rate")
  var exchangeRate = 0
  @SerializedName("fee")
  var fee = 0
  @SerializedName("deposit_ref")
  var depositRef = ""
  @SerializedName("account_number")
  var accountNumber = ""
  @SerializedName("account_name")
  var accountName = ""
  @SerializedName("bank_name")
  var bankName = ""
  @SerializedName("eta")
  var eta = ""
}
package io.demars.stellarwallet.models

data class Operation(var id: String, var sourceAccount: String, var type: String,
                     var createdAt: String, var transactionSuccessful: Boolean,
                     var transactionHash: String?, var transactionLink: String?,
                     var amount: String?, var from: String?, var to: String?,
                     var asset: String?, var counterAsset: String?,
                     var price: String?, var memo: String?) {

   enum class OperationType(val value : String) {
    CREATED("create_account"),
    PAYMENT("payment"),
    PATH_PAYMENT("path_payment"),
    MANAGE_OFFER("manage_offer"),
    PASSIVE_OFFER("create_passive_offer"),
    INFLATION("inflation"),
    MANAGE_DATA("manage_data"),
    ALLOW_TRUST("allow_trust"),
    CHANGE_TRUST("change_trust"),
    SET_OPTIONS("set_options"),
    ACCOUNT_MERGE("account_merge")
  }
}
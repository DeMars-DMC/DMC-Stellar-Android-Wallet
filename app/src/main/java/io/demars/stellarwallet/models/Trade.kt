package io.demars.stellarwallet.models

data class Trade(var activeAssetCode:String, var createdAt: String,
                 var offerId: String?, var baseIsSeller: Boolean?, var price: String?,
                 var baseOfferId: String, var counterOfferId: String,
                 var baseAsset: String, var counterAsset: String,
                 var baseAssetType: String?, var counterAssetType: String?,
                 var baseAmount: String?, var counterAmount: String?,
                 var baseAccount: String?, var counterAccount: String?)
package io.demars.stellarwallet.models.stellar

data class TradeEffect (var activeAssetCode:String, var type: String, var createdAt: String,
                        var boughtAsset: String, var soldAsset: String,
                        var boughtAmount: String?, var soldAmount: String?)

package io.demars.stellarwallet.api.horizon.model

data class TradeEffect (var activeAssetCode:String, var type: String, var createdAt: String,
                        var boughtAsset: String, var soldAsset: String,
                        var boughtAmount: String?, var soldAmount: String?)

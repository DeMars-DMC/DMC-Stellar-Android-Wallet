package io.demars.stellarwallet.api.horizon.model

data class Transaction(var type: String, var createdAt: String,
                       var assetCode: String?, var amount: String?,
                       var memo: String?, var sourceAccount: String?,
                       var fee: String?, var operationCount: Int = 0,
                       var successful: Boolean = false)
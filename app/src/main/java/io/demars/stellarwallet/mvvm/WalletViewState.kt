package io.demars.stellarwallet.mvvm

import io.demars.stellarwallet.models.AvailableBalance
import io.demars.stellarwallet.models.TotalBalance
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse

data class WalletViewState(var status: AccountStatus, var accountId:String?, var activeAssetCode : String,
                           var availableBalance : AvailableBalance?, var totalBalance : TotalBalance?,
                           var operationList: ArrayList<Pair<OperationResponse, String?>>?,
                           var tradesList: ArrayList<TradeResponse>?) {

    enum class AccountStatus {
        UNKNOWN,
        UNFUNDED,
        ACTIVE,
        ERROR
    }
}
package io.demars.stellarwallet.mvvm.effects

import io.demars.stellarwallet.models.AvailableBalance
import io.demars.stellarwallet.models.TotalBalance
import org.stellar.sdk.responses.effects.EffectResponse

data class WalletViewState(var status: AccountStatus, var accountId:String, var activeAssetCode : String,
                           var availableBalance : AvailableBalance?, var totalBalance : TotalBalance?,
                           var effectList: ArrayList<EffectResponse>?) {

    enum class AccountStatus {
        UNKNOWN,
        UNFUNDED,
        ACTIVE,
        ERROR
    }
}
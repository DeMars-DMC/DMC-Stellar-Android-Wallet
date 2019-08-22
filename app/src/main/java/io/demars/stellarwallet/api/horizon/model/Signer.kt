package io.demars.stellarwallet.api.horizon.model

import io.demars.stellarwallet.helpers.Constants
import org.stellar.sdk.responses.AccountResponse

class Signer (signers: Array<AccountResponse.Signer>?) {
    var count = signers?.size ?: 0

    var amount = count * Constants.MINIMUM_BALANCE_INCREMENT
}
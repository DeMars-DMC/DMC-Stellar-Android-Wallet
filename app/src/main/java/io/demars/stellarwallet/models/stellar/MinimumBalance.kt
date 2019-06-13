package io.demars.stellarwallet.models.stellar

import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.stellar.Offer
import io.demars.stellarwallet.models.stellar.Signer
import io.demars.stellarwallet.models.stellar.TrustLine
import org.stellar.sdk.responses.AccountResponse

data class MinimumBalance(val accountResponse: AccountResponse) {

    var trustlines = TrustLine(accountResponse.balances.size)
    var offers = Offer(accountResponse.subentryCount - trustlines.count)
    var signers = Signer(accountResponse.signers)

    var totalAmount = trustlines.amount + offers.amount + signers.amount + Constants.BASE_RESERVE

}

package io.demars.stellarwallet.models

import io.demars.stellarwallet.interfaces.StellarAccount
import org.stellar.sdk.responses.AccountResponse

data class StellarAccountImpl(private val accountResponse: AccountResponse) : StellarAccount {
    override fun getAccountId() : String {
        return accountResponse.keypair.accountId
    }

    override  fun getInflationDestination() : String? {
        return accountResponse.inflationDestination
    }

    override  fun getSequenceNumber() : Long {
        return accountResponse.sequenceNumber
    }

    override fun getSubEntryCount() : Int? {
        return accountResponse.subentryCount
    }

    override fun getAccountResponse(): AccountResponse {
        return accountResponse
    }
}

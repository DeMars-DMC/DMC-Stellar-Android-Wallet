package io.demars.stellarwallet.models

import org.apache.commons.lang.builder.HashCodeBuilder
import org.stellar.sdk.responses.AccountResponse

data class StellarAccount(val accountId: String?, val inflationDestination: String?,
                          var sequenceNumber: Long, var subEntryCount: Int?) {
  private var accountResponse: AccountResponse? = null
  constructor(accountResponse: AccountResponse) : this(accountResponse.keypair.accountId,
    accountResponse.inflationDestination, accountResponse.sequenceNumber, accountResponse.subentryCount) {
    this.accountResponse = accountResponse
  }

  /**
   * This is was inspired by {@link <a href=]//github.com/Block-Equity/stellar-ios-wallet/blob/ee2414061e309e2b97c4883541ca1cd335994487/StellarHub/Objects/StellarAccount.swift.L253">stellar ios wallet"</a>}
   */
  fun basicHashCode(): Int {
    val builder = HashCodeBuilder(17, 37)
      .append(accountId)
      .append(inflationDestination)
      .append(sequenceNumber)
      .append(subEntryCount)

    // let's check the balance of each entry since sequenceNumber does not
    accountResponse?.balances?.forEach {
      builder.append(it.balance)
    }

    return builder.toHashCode()
  }

  fun basicEquals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as StellarAccount

    /**
     * for now let's use the hashCode.
     */
    if (basicHashCode() != other.basicHashCode()) return false

    return true
  }
}

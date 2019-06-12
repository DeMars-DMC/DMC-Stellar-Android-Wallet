package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.AvailableBalance
import org.stellar.sdk.responses.AccountResponse

interface LocalStore {
    fun getEncryptedPhrase(): String?
    fun setEncryptedPhrase(encryptedPassphrase: String?)
    fun getEncryptedPassphrase(): String?
    fun setEncryptedPassphrase(encryptedPassphrase: String)
    fun getStellarAccountId(): String?
    fun setStellarAccountId(accountId: String)
    fun getBalances(): Array<AccountResponse.Balance>
    fun setBalances(balances: Array<AccountResponse.Balance>?)
    fun getAvailableBalances(): Array<AvailableBalance>
    fun setAvailableBalances(availableBalances: Array<AvailableBalance>?)
    fun getIsRecoveryPhrase(): Boolean
    fun setIsRecoveryPhrase(isRecoveryPhrase: Boolean)
    fun getUserState(): Int
    fun setUserState(state: Int)
    fun setShowPinOnSend(showPinOnSend: Boolean)
    fun getShowPinOnSend(): Boolean
    fun clearLocalStore():  Boolean
    fun isRegistered(): Boolean
    fun isVerified(): Boolean
}
package io.demars.stellarwallet

import io.demars.stellarwallet.interfaces.LocalStore
import io.demars.stellarwallet.interfaces.WalletStore
import io.demars.stellarwallet.models.AvailableBalance
import io.demars.stellarwallet.models.BasicBalance
import org.stellar.sdk.responses.AccountResponse

class DmcWallet(private val localStore: LocalStore) : WalletStore {
  override fun getUserState(): Int {
    return localStore.getUserState()
  }

  override fun setUserState(state: Int) {
    localStore.setUserState(state)
  }

  override fun getEncryptedPhrase(): String? {
    return localStore.getEncryptedPhrase()
  }

  override fun setEncryptedPhrase(encryptedPassphrase: String?) {
    localStore.setEncryptedPhrase(encryptedPassphrase)
  }

  override fun getEncryptedPassphrase(): String? {
    return localStore.getEncryptedPassphrase()
  }

  override fun setEncryptedPassphrase(encryptedPassphrase: String) {
    localStore.setEncryptedPassphrase(encryptedPassphrase)
  }

  override fun getStellarAccountId(): String? {
    return localStore.getStellarAccountId()
  }

  override fun setStellarAccountId(accountId: String) {
    localStore.setStellarAccountId(accountId)
  }

  override fun getBalances(): Array<AccountResponse.Balance> {
    return localStore.getBalances()
  }

  override fun setBalances(balances: Array<AccountResponse.Balance>?) {
    localStore.setBalances(balances)
  }

  override fun getAvailableBalances(): Array<AvailableBalance> {
    return localStore.getAvailableBalances()
  }

  override fun setAvailableBalances(availableBalances: Array<AvailableBalance>?) {
    localStore.setAvailableBalances(availableBalances)
  }

  override fun getIsRecoveryPhrase(): Boolean {
    return localStore.getIsRecoveryPhrase()
  }

  override fun setIsRecoveryPhrase(isRecoveryPhrase: Boolean) {
    localStore.setIsRecoveryPhrase(isRecoveryPhrase)
  }

  override fun setShowPinOnSend(showPinOnSend: Boolean) {
    localStore.setShowPinOnSend(showPinOnSend)
  }

  override fun getShowPinOnSend(): Boolean {
    return localStore.getShowPinOnSend()
  }

  override fun clearLocalStore(): Boolean {
    return localStore.clearLocalStore()
  }

  override fun isRegistered(): Boolean = localStore.getUserState() > 0
  override fun isVerified(): Boolean = localStore.getUserState() > 1
}

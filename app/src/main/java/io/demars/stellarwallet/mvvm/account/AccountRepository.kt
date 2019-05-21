package io.demars.stellarwallet.mvvm.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.interfaces.OnLoadAccount
import io.demars.stellarwallet.interfaces.StellarAccount
import io.demars.stellarwallet.models.BasicStellarAccount
import io.demars.stellarwallet.models.MinimumBalance
import io.demars.stellarwallet.models.StellarAccountImpl
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse
import timber.log.Timber

/**
 * Tried to implement (https://github.com/JoaquimLey/transport-eta/blob/26ce1a7f4b2dff12c6efa2292531035e70bfc4ae/app/src/main/java/com/joaquimley/buseta/repository/BusRepository.java)
 * While at the same time only using remote, and not local or Room db
 */
object AccountRepository {
  private var liveData = MutableLiveData<AccountEvent>()
  private var accountLiveData = MutableLiveData<AccountResponse>()

  private var accountResponse: AccountResponse? = null
  private var isBusy = false
  /**
   * Returns an observable for ALL the effects table changes
   */
  fun loadAccountEvent(forceRefresh: Boolean = true): LiveData<AccountEvent> {
    fetch(forceRefresh)
    return liveData
  }

  fun loadAccount(): MutableLiveData<AccountResponse> {
    fetch()
    return accountLiveData
  }

  fun refresh() {
    fetch(true)
  }

  private fun fetch(forceRefresh: Boolean = true) {
    val account = accountResponse
    if (!forceRefresh && account != null) {
      liveData.postValue(AccountEvent(200, StellarAccountImpl(account)))
      accountLiveData.postValue(accountResponse)
    } else {
      if (!isBusy) {
        Horizon.getLoadAccountTask(object : OnLoadAccount {
          override fun onLoadAccount(result: AccountResponse?) {
            if (result != null) {
              Timber.d("loadAccountEvent successfully")
              accountResponse = result

              WalletApplication.wallet.setBalances(result.balances)
              WalletApplication.userSession.setMinimumBalance(MinimumBalance(result))
              WalletApplication.wallet.setAvailableBalance(AccountUtils.calculateAvailableBalance())

              liveData.postValue(AccountEvent(200, StellarAccountImpl(result)))

              accountLiveData.postValue(accountResponse)
            }
            isBusy = false
          }

          override fun onError(error: ErrorResponse) {
            val stellarAccountid = WalletApplication.wallet.getStellarAccountId()

            Timber.e("(${error.code}) Error Loading account")
            liveData.postValue(AccountEvent(error.code, BasicStellarAccount(stellarAccountid, null, 0L, null)))
            isBusy = false
          }

        }).execute()
      } else {
        Timber.d("it is busy, loading account cancelled")
      }
    }
  }

  data class AccountEvent(val httpCode: Int, val stellarAccount: StellarAccount)

  fun clear() {
    accountResponse = null
    liveData = MutableLiveData()
    accountLiveData = MutableLiveData()
  }
}

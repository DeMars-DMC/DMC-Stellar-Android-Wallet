package io.demars.stellarwallet.mvvm.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.interfaces.OnLoadAccount
import io.demars.stellarwallet.models.StellarAccount
import io.demars.stellarwallet.api.horizon.model.MinimumBalance
import io.demars.stellarwallet.api.horizon.Horizon
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

  private var accountResponse: AccountResponse? = null
  private var isBusy = false

  /**
   * Returns an observable for ALL the effects table changes
   */
  fun refreshData(): LiveData<AccountEvent> {
    fetch()
    return liveData
  }

  fun refresh() {
    fetch()
  }

  private fun fetch() {
    if (!isBusy) {
      Horizon.getLoadAccountTask(object : OnLoadAccount {
        override fun onLoadAccount(result: AccountResponse?) {
          if (result != null) {
            Timber.d("refreshData successfully")
            accountResponse = result

            DmcApp.wallet.setBalances(result.balances)
            DmcApp.userSession.setMinimumBalance(MinimumBalance(result))
            DmcApp.wallet.setAvailableBalances(
              AccountUtils.calculateAvailableBalances(result))

            liveData.postValue(AccountEvent(200, StellarAccount(result)))
          }
          isBusy = false
        }

        override fun onError(error: ErrorResponse) {
          val stellarAccountId = DmcApp.wallet.getStellarAccountId()

          Timber.e("(${error.code}) Error Loading account")
          liveData.postValue(AccountEvent(error.code, StellarAccount(stellarAccountId, null, 0L, null)))
          isBusy = false
        }

      }).execute()
    } else {
      Timber.d("it is busy, loading account cancelled")
    }
  }

  fun clear() {
    accountResponse = null
    liveData = MutableLiveData()
  }

  data class AccountEvent(val httpCode: Int, val stellarAccount: StellarAccount)
}

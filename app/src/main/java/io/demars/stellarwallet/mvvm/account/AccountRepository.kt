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
class AccountRepository {
    private var liveData = MutableLiveData<AccountEvent>()

    /**
     * Returns an observable for ALL the effects table changes
     */
    fun loadAccount(): LiveData<AccountEvent> {
        Horizon.getLoadAccountTask(object : OnLoadAccount {
            override fun onLoadAccount(result: AccountResponse?) {
                if (result != null) {
                    Timber.d("loadAccount successfully")

                    WalletApplication.wallet.setBalances(result.balances)
                    WalletApplication.userSession.setMinimumBalance(MinimumBalance(result))
                    WalletApplication.wallet.setAvailableBalance(AccountUtils.calculateAvailableBalance())

                    liveData.postValue(AccountEvent(200, StellarAccountImpl(result)))
                }
            }

            override fun onError(error: ErrorResponse) {
                Timber.e("(${error.code}) Error Loading account")

                liveData.postValue(AccountEvent(error.code, BasicStellarAccount(WalletApplication.wallet.getStellarAccountId()!!, null, 0L, null)))
            }

        }).execute()
        return liveData
    }

    data class AccountEvent(val httpCode:Int, val stellarAccount: StellarAccount)

}
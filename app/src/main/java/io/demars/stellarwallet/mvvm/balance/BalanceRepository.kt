package io.demars.stellarwallet.mvvm.balance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.StellarAccount
import io.demars.stellarwallet.models.BalanceAvailability
import io.demars.stellarwallet.models.BalanceAvailabilityImpl
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.trading.OffersRepository
import org.stellar.sdk.responses.OfferResponse
import timber.log.Timber

object BalanceRepository {
  private var liveData = MutableLiveData<BalanceAvailability>()
  private var account : StellarAccount? = null
  private var offerList : ArrayList<OfferResponse>? = null

  fun init(){
    AccountRepository.loadAccountEvent().observeForever {
      var newStellarAccount = false
      it?.let { newAccount ->
        if (newAccount.httpCode == 200) {
          if (account == null) {
            newStellarAccount = true
          } else {
            account?.let{ existingAccount ->
              if (existingAccount.basicHashCode() != newAccount.stellarAccount.basicHashCode()) {
                newStellarAccount = true
              }
            }
          }
        }
        if (newStellarAccount) {
          account = it.stellarAccount
          offerList = null
          OffersRepository.refresh()
        } else if(offerList == null) {
          OffersRepository.refresh()
        }
      }
    }

    OffersRepository.loadOffers().observeForever {
      if (it != null && offerList == null) {
        offerList = it
        notifyIfNeeded()
      }
    }
  }

  private fun notifyIfNeeded() {
    account?.let { offerList?.let { that ->
      Timber.d("notifying new balance")
      liveData.postValue(BalanceAvailabilityImpl(it.getAccountResponse(), that)) }
    }
  }

  fun loadBalance(forceRefresh:Boolean = false): LiveData<BalanceAvailability> {
    if (forceRefresh) {
      refresh()
    }
    return liveData
  }

  fun refresh() {
    account = null
    offerList = null
    AccountRepository.refresh()
  }

  fun clear(){
    liveData = MutableLiveData()
    offerList = null
    account = null
  }
}
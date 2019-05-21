package io.demars.stellarwallet.mvvm.trading

import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.remote.Horizon
import org.stellar.sdk.responses.OfferResponse
import timber.log.Timber

object OffersRepository {
  private var liveData = MutableLiveData<ArrayList<OfferResponse>>()
  fun loadOffers(): MutableLiveData<ArrayList<OfferResponse>> {
    return liveData
  }

  fun refresh(){
    Timber.d("refreshing offers")
    Horizon.getOffers(object: Horizon.OnOffersListener {
      override fun onOffers(offers: ArrayList<OfferResponse>) {
        liveData.postValue(offers)
      }

      override fun onFailed(errorMessage: String) {
        Timber.e(errorMessage)
        liveData.postValue(null)
      }
    })
  }

  fun clear() {
    liveData = MutableLiveData()
  }
}
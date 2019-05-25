package io.demars.stellarwallet.mvvm.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.OnLoadTrades
import io.demars.stellarwallet.mvvm.remote.RemoteRepository
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.TradeResponse
import timber.log.Timber

class TradesRepository private constructor(private val remoteRepository: RemoteRepository) {
  private val ENABLE_STREAM = false
  private var tradesList: ArrayList<TradeResponse> = ArrayList()
  private var tradeListLiveData = MutableLiveData<ArrayList<TradeResponse>>()
  private var eventSource: SSEStream<TradeResponse>? = null
  private var isBusy = false
  private var currentCursor: String = ""

  fun listLiveData(): LiveData<ArrayList<TradeResponse>> = tradeListLiveData
  private fun notifyLiveData(data: ArrayList<TradeResponse>) {
    Timber.d("notifyLiveData size {${data.size}}")
    tradeListLiveData.postValue(data)
  }

  @Synchronized
  fun forceRefresh() {
    Timber.d("Force refresh trades")
    if (isBusy) {
      Timber.d("ignoring force refresh, it is busy.")
      return
    }
    isBusy = true
    fetch()
  }

  fun clear() {
    tradesList.clear()
  }


  /**
   * Makes a call to the webservice. Keep it private since the view/viewModel should be 100% abstracted
   * from the data sources implementation.
   */
  private fun fetch() {
    tradesList.clear()
    remoteRepository.getTrades("", 100, object : OnLoadTrades {
      override fun onError(errorMessage: String) {
        isBusy = false
      }

      override fun onLoadTrades(result: ArrayList<TradeResponse>?, cursor: String) {
        Timber.d("fetched ${result?.size} trades from cursor $cursor")
        if (result != null) {
          if (result.isNotEmpty()) {
            tradesList.addAll(result)
          } else {
            notifyLiveData(tradesList)
            isBusy = false

            if (cursor != currentCursor) {
              openStream()
              currentCursor = cursor
            }
          }
        }
      }
    })
  }

  fun openStream() {
    if (!ENABLE_STREAM) return

    closeStream()
    Timber.d("Opening the stream")
    eventSource = remoteRepository.registerForTrades("now", EventListener {
      Timber.d("Stream response {$it}")
      tradesList.add(0, it)
      notifyLiveData(tradesList)
    })
  }

  fun closeStream() {
    if (!ENABLE_STREAM) return
    Timber.d("trying to close the stream {$eventSource}")
    eventSource?.let {
      Timber.d("Closing the stream")
      it.close()
    }
  }

  companion object {

    private var instance: TradesRepository? = null

    fun getInstance(): TradesRepository {
      if (instance == null) {
        instance = TradesRepository(RemoteRepository())
      }
      return instance as TradesRepository
    }
  }
}
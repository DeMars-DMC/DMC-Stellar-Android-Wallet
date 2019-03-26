package io.demars.stellarwallet.mvvm.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.OnLoadTransactions
import io.demars.stellarwallet.mvvm.remote.RemoteRepository
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.TransactionResponse
import timber.log.Timber

class TransactionsRepository private constructor(private val remoteRepository: RemoteRepository) {
  private val ENABLE_STREAM = false
  private var transactionsList: ArrayList<TransactionResponse> = ArrayList()
  private var transactionListLiveData = MutableLiveData<ArrayList<TransactionResponse>>()
  private var eventSource: SSEStream<TransactionResponse>? = null
  private var isBusy = false
  private var currentCursor: String = ""
  /**
   * Returns an observable for ALL the transactions table changes
   */
  fun loadList(forceRefresh: Boolean): LiveData<ArrayList<TransactionResponse>> {
    if (forceRefresh || transactionsList.isEmpty()) {
      forceRefresh()
    }
    return transactionListLiveData
  }

  @Synchronized
  fun forceRefresh() {
    Timber.d("Force refresh transactions")
    if (isBusy) {
      Timber.d("ignoring force refresh, it is busy.")
      return
    }
    isBusy = true
    fetchTransactionsList(true)
  }

  fun clear() {
    transactionsList.clear()
  }

  private fun notifyLiveData(data: ArrayList<TransactionResponse>) {
    Timber.d("notifyLiveData size {${data.size}}")
    transactionListLiveData.postValue(data)
  }

  /**
   * Makes a call to the webservice. Keep it private since the view/viewModel should be 100% abstracted
   * from the data sources implementation.
   */
  private fun fetchTransactionsList(notifyFirsTime: Boolean = false) {
    var cursor = ""
    if (!transactionsList.isEmpty()) {
      cursor = transactionsList.last().pagingToken
      if (notifyFirsTime) {
        notifyLiveData(transactionsList)
      }
    }

    remoteRepository.getTransactions(cursor, 200, object : OnLoadTransactions {
      override fun onError(errorMessage: String) {
        isBusy = false
      }

      override fun onLoadTransactions(result: ArrayList<TransactionResponse>?) {
        Timber.d("fetched ${result?.size} transactions from cursor $cursor")
        if (result != null) {
          if (!result.isEmpty()) {
            //is the first time let's notify the ui
            val isFirstTime = transactionsList.isEmpty()
            transactionsList.addAll(result)
            if (isFirstTime) notifyLiveData(transactionsList)
            Timber.d("recursive call to getTransactions")
            fetchTransactionsList()
          } else {
            if (cursor != currentCursor) {
              if (ENABLE_STREAM) {
                closeStream()
                Timber.d("Opening the stream")
                eventSource = remoteRepository.registerForTransactions("now", EventListener {
                  Timber.d("Stream response {$it}, created at: ${it.createdAt}")
                  transactionsList.add(0, it)
                  notifyLiveData(transactionsList)
                })
              }
              currentCursor = cursor
            }
            isBusy = false
            notifyLiveData(transactionsList)
          }
        }
      }
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

    private var instance: TransactionsRepository? = null

    fun getInstance(): TransactionsRepository {
      if (instance == null) {
        instance = TransactionsRepository(RemoteRepository())
      }
      return instance as TransactionsRepository
    }
  }
}
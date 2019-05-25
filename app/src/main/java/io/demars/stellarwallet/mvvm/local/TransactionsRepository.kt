package io.demars.stellarwallet.mvvm.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.OnLoadOperations
import io.demars.stellarwallet.mvvm.remote.RemoteRepository
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
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
  fun listLiveData(): LiveData<ArrayList<TransactionResponse>> = transactionListLiveData
  private fun notifyLiveData(data: ArrayList<TransactionResponse>) {
    Timber.d("notifyLiveData size {${data.size}}")
    transactionListLiveData.postValue(data)
  }

  @Synchronized
  fun forceRefresh() {
    Timber.d("Force refresh transactions")
    if (isBusy) {
      Timber.d("ignoring force refresh, it is busy.")
      return
    }
    isBusy = true
    fetch()
  }

  fun clear() {
    transactionsList.clear()
  }

  /**
   * Makes a call to the webservice. Keep it private since the view/viewModel should be 100% abstracted
   * from the data sources implementation.
   */
  private fun fetch() {
    transactionsList.clear()
    remoteRepository.getTransactions("", 100, object : OnLoadOperations {
      override fun onLoadOperations(result: java.util.ArrayList<Pair<OperationResponse, String?>>?, cursor: String) = Unit
      override fun onError(errorMessage: String) {
        isBusy = false
      }

      override fun onLoadTransactions(result: ArrayList<TransactionResponse>, cursor: String) {
        Timber.d("fetched ${result.size} transactions from cursor $cursor")
        if (result.isNotEmpty()) {
          transactionsList.addAll(result)
        } else {
          notifyLiveData(transactionsList)
          isBusy = false

          if (cursor != currentCursor) {
            openStream()
            currentCursor = cursor
          }
        }
      }
    })
  }

  fun openStream() {
    if (!ENABLE_STREAM) return
    closeStream()
    Timber.d("Opening the stream")
    eventSource = remoteRepository.registerForTransactions("now", EventListener {
      Timber.d("Stream response {$it}, created at: ${it.createdAt}")
      transactionsList.add(0, it)
      notifyLiveData(transactionsList)
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
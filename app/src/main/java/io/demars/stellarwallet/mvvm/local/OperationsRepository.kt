package io.demars.stellarwallet.mvvm.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.OnLoadOperations
import io.demars.stellarwallet.mvvm.remote.RemoteRepository
import org.stellar.sdk.MemoText
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber

class OperationsRepository private constructor(private val remoteRepository: RemoteRepository) {
  private val ENABLE_STREAM = false
  private var operationsList: ArrayList<Pair<OperationResponse, String?>> = ArrayList()
  private var operationListLiveData = MutableLiveData<ArrayList<Pair<OperationResponse, String?>>>()
  private var eventSource: SSEStream<OperationResponse>? = null
  private var isBusy = false
  private var currentCursor: String = ""

  /**
   * Returns an observable for ALL the operations table changes
   */
  fun listLiveData(): LiveData<ArrayList<Pair<OperationResponse, String?>>> {
    return operationListLiveData
  }

  @Synchronized
  fun forceRefresh() {
    Timber.d("Force refresh operations")
    if (isBusy) {
      Timber.d("ignoring force refresh, it is busy.")
      return
    }
    isBusy = true
    fetch()
  }

  fun clear() {
    operationsList.clear()
  }

  private fun notifyLiveData(data: ArrayList<Pair<OperationResponse, String?>>) {
    Timber.d("notifyLiveData size {${data.size}}")
    operationListLiveData.postValue(data)
  }

  /**
   * Makes a call to the webservice. Keep it private since the view/viewModel should be 100% abstracted
   * from the data sources implementation.
   */
  private fun fetch() {
    operationsList.clear()
    remoteRepository.getOperations("", 100,
      object : OnLoadOperations {
        override fun onError(errorMessage: String) {
          isBusy = false
        }

        override fun onLoadOperations(result: ArrayList<Pair<OperationResponse, String?>>?, cursor: String) {
          Timber.d("fetched ${result?.size} operations from cursor $cursor")
          if (result != null) {
            if (result.isNotEmpty()) {
              operationsList.addAll(result)
              notifyLiveData(operationsList)
            } else {
              notifyLiveData(operationsList)
              if (cursor != currentCursor) {
                openStream()
                currentCursor = cursor
              }
            }
          }
        }

        override fun onLoadTransactions(result: ArrayList<TransactionResponse>, cursor: String) {
          if (result.isEmpty()) {
            isBusy = false
            return
          }

          val operationsWithMemo = ArrayList<Pair<OperationResponse, String?>>()
          for (operation in operationsList) {
            var memo: String? = null
            for (transaction in result) {
              if (transaction.memo is MemoText && operation.first.transactionHash == transaction.hash) {
                // Found memo for operation(transaction)
                memo = (transaction.memo as MemoText).text
                break
              }
            }

            operationsWithMemo.add(Pair(operation.first, memo ?: operation.second))
          }

          operationsList = operationsWithMemo

          notifyLiveData(operationsList)
        }
      })
  }

  fun openStream() {
    if (!ENABLE_STREAM) return
    closeStream()
    Timber.d("Opening the stream")
    eventSource = remoteRepository.registerForOperations("now", EventListener {
      Timber.d("Stream response {$it}, created at: ${it.createdAt}")
      operationsList.add(0, Pair(it, null))
      notifyLiveData(operationsList)
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

    private var instance: OperationsRepository? = null

    fun getInstance(): OperationsRepository {
      if (instance == null) {
        instance = OperationsRepository(RemoteRepository())
      }
      return instance as OperationsRepository
    }
  }
}
package io.demars.stellarwallet.mvvm.remote

import org.stellar.sdk.responses.TransactionResponse

interface OnLoadTransactions {
  fun onLoadTransactions(result: ArrayList<TransactionResponse>?)
  fun onError(errorMessage:String)
}
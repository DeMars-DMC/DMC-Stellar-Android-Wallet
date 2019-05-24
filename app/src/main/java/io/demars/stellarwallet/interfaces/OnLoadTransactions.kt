package io.demars.stellarwallet.interfaces

import org.stellar.sdk.responses.TransactionResponse

interface OnLoadTransactions {
  fun onLoadTransactions(result: ArrayList<TransactionResponse>)
  fun onError(errorMessage:String)
}
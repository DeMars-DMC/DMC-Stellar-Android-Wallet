package io.demars.stellarwallet.interfaces

import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import java.util.ArrayList

interface OnLoadOperations {
  fun onLoadOperations(result: ArrayList<Pair<OperationResponse, String?>>?)
  fun onLoadTransactions(result: ArrayList<TransactionResponse>)
  fun onError(errorMessage:String)

}
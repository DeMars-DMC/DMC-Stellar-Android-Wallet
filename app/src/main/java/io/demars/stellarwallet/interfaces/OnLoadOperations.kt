package io.demars.stellarwallet.interfaces

import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import java.util.ArrayList

interface OnLoadOperations {
  fun onLoadOperations(result: ArrayList<Pair<OperationResponse, String?>>?, cursor: String)
  fun onLoadTransactions(result: ArrayList<TransactionResponse>, cursor: String)
  fun onError(errorMessage: String)

}
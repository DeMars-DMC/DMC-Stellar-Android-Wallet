package io.demars.stellarwallet.mvvm.remote

import org.stellar.sdk.responses.operations.OperationResponse
import java.util.ArrayList

interface OnLoadOperations {
  fun onLoadOperations(result: ArrayList<Pair<OperationResponse, String?>>?)
  fun onError(errorMessage:String)

}
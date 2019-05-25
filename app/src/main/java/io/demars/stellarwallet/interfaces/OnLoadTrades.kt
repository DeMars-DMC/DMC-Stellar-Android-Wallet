package io.demars.stellarwallet.interfaces

import org.stellar.sdk.responses.TradeResponse

interface OnLoadTrades {
  fun onLoadTrades(result: ArrayList<TradeResponse>?, cursor : String)
  fun onError(errorMessage:String)
}
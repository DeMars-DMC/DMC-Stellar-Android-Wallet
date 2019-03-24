package io.demars.stellarwallet.mvvm.remote

import org.stellar.sdk.responses.TradeResponse

interface OnLoadTrades {
  fun onLoadTrades(result: ArrayList<TradeResponse>?)
  fun onError(errorMessage:String)
}
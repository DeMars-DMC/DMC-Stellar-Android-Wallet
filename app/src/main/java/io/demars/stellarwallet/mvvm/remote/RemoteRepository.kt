package io.demars.stellarwallet.mvvm.remote

import io.demars.stellarwallet.interfaces.*
import io.demars.stellarwallet.api.horizon.Horizon
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.effects.EffectResponse
import org.stellar.sdk.responses.operations.OperationResponse

class RemoteRepository {

  fun getEffects(cursor: String, limit: Int, listener: OnLoadEffects) {
    Horizon.getLoadEffectsTask(cursor, limit, listener).execute()
  }

  fun getOperations(cursor: String, limit: Int, listener: OnLoadOperations) {
    Horizon.getLoadOperationsTask(cursor, limit, listener).execute()
  }


  fun getTransactions(cursor: String, limit: Int, listener: OnLoadOperations) {
    Horizon.getLoadTransactionsTask(cursor, limit, listener).execute()
  }

  fun getTrades(cursor: String, limit: Int, listener: OnLoadTrades) {
    Horizon.getLoadTradesTask(cursor, limit, listener).execute()
  }

  fun registerForEffects(cursor: String, listener: EventListener<EffectResponse>): SSEStream<EffectResponse>? {
    return Horizon.registerForEffects(cursor, listener)
  }

  fun registerForOperations(cursor: String, listener: EventListener<OperationResponse>): SSEStream<OperationResponse>? {
    return Horizon.registerForOperations(cursor, listener)
  }

  fun registerForTransactions(cursor: String, listener: EventListener<TransactionResponse>): SSEStream<TransactionResponse>? {
    return Horizon.registerForTransactions(cursor, listener)
  }

  fun registerForTrades(cursor: String, listener: EventListener<TradeResponse>): SSEStream<TradeResponse>? {
    return Horizon.registerForTrades(cursor, listener)
  }
}

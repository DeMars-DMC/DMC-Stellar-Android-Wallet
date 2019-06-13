package io.demars.stellarwallet.remote

import android.os.AsyncTask
import io.demars.stellarwallet.interfaces.*
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.stellar.HorizonException
import org.stellar.sdk.Asset
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.effects.EffectResponse
import org.stellar.sdk.responses.operations.OperationResponse

interface HorizonTasks {
  fun init(server: ServerType)
  fun registerForEffects(cursor: String, listener: EventListener<EffectResponse>): SSEStream<EffectResponse>?
  fun registerForOperations(cursor: String, listener: EventListener<OperationResponse>): SSEStream<OperationResponse>?
  fun registerForTransactions(cursor: String, listener: EventListener<TransactionResponse>): SSEStream<TransactionResponse>?
  fun registerForTrades(cursor: String, listener: EventListener<TradeResponse>): SSEStream<TradeResponse>?
  fun getLoadAccountTask(listener: OnLoadAccount): AsyncTask<Void, Void, AccountResponse>
  fun getLoadEffectsTask(cursor: String, limit: Int, listener: OnLoadEffects): AsyncTask<Void, Void, ArrayList<EffectResponse>?>
  fun getLoadOperationsTask(cursor: String, limit: Int, listener: OnLoadOperations): AsyncTask<Void, Void, ArrayList<Pair<OperationResponse, String?>>?>
  fun getLoadTransactionsTask(cursor: String, limit: Int, listener: OnLoadOperations): AsyncTask<Void, Void, ArrayList<TransactionResponse>>
  fun getLoadTradesTask(cursor: String, limit: Int, listener: OnLoadTrades): AsyncTask<Void, Void, ArrayList<TradeResponse>?>
  fun getSendTask(listener: SuccessErrorCallback, destAddress: String, secretSeed: CharArray, memo: String, amount: String): AsyncTask<Void, Void, HorizonException>
  fun getWithdrawTask(listener: SuccessErrorCallback, assetCode: String, secretSeed: CharArray, destination: String, memo: String, amount: String, fee: String): AsyncTask<Void, Void, HorizonException>
  fun getJoinInflationDestination(listener: SuccessErrorCallback, secretSeed: CharArray, inflationDest: String): AsyncTask<Void, Void, HorizonException>
  fun getChangeTrust(listener: SuccessErrorCallback, asset: Asset, removeTrust: Boolean, secretSeed: CharArray): AsyncTask<Void, Void, HorizonException?>
  fun getCreateMarketOffer(listener: Horizon.OnMarketOfferListener, secretSeed: CharArray, sellingAsset: Asset, buyingAsset: Asset, amount: String, price: String)
  fun getOrderBook(listener: Horizon.OnOrderBookListener, buyingAsset: DataAsset, sellingAsset: DataAsset)
  fun getOffers(listener: Horizon.OnOffersListener)
  fun deleteOffer(id: Long, secretSeed: CharArray, selling: Asset, buying: Asset, price: String, listener: Horizon.OnMarketOfferListener)
}
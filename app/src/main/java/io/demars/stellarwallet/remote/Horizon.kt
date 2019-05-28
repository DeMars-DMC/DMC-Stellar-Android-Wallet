package io.demars.stellarwallet.remote

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.*
import io.demars.stellarwallet.models.AssetUtils
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.HorizonException
import io.demars.stellarwallet.models.Operation
import org.jetbrains.anko.doAsync
import org.stellar.sdk.*
import org.stellar.sdk.Transaction.Builder.TIMEOUT_INFINITE
import org.stellar.sdk.requests.*
import org.stellar.sdk.responses.*
import org.stellar.sdk.responses.effects.EffectResponse
import org.stellar.sdk.responses.operations.OperationResponse
import shadow.okhttp3.OkHttpClient
import timber.log.Timber
import java.net.ConnectException
import java.util.concurrent.TimeUnit

object Horizon : HorizonTasks {
  private lateinit var HORIZON_SERVER: Server
  override fun init(server: ServerType) {
    var serverAddress = ""
    when (server) {
      ServerType.PROD -> {
        serverAddress = "https://horizon.stellar.org"
        Network.usePublicNetwork()
      }
      ServerType.TEST_NET -> {
        serverAddress = "https://horizon-testnet.stellar.org"
        Network.useTestNetwork()
      }
    }
    HORIZON_SERVER = createServer(serverAddress)
  }

  override fun getLoadEffectsTask(cursor: String, limit: Int, listener: OnLoadEffects): AsyncTask<Void, Void, ArrayList<EffectResponse>?> {
    return LoadEffectsTask(cursor, limit, listener)
  }

  override fun getLoadOperationsTask(cursor: String, limit: Int, listener: OnLoadOperations): AsyncTask<Void, Void, ArrayList<Pair<OperationResponse, String?>>?> {
    return LoadOperationsTask(cursor, limit, listener)
  }

  override fun getLoadTransactionsTask(cursor: String, limit: Int, listener: OnLoadOperations): AsyncTask<Void, Void, ArrayList<TransactionResponse>> {
    return LoadTransactionsTask(cursor, limit, listener)
  }

  override fun getLoadTradesTask(cursor: String, limit: Int, listener: OnLoadTrades): AsyncTask<Void, Void, ArrayList<TradeResponse>?> {
    return LoadTradesTask(cursor, limit, listener)
  }

  override fun getSendTask(listener: SuccessErrorCallback, destAddress: String, secretSeed: CharArray, memo: String, amount: String): AsyncTask<Void, Void, HorizonException> {
    return SendTask(listener, destAddress, secretSeed, memo, amount)
  }

  override fun getJoinInflationDestination(listener: SuccessErrorCallback, secretSeed: CharArray, inflationDest: String): AsyncTask<Void, Void, HorizonException> {
    return JoinInflationDestination(listener, secretSeed, inflationDest)
  }

  override fun getChangeTrust(listener: SuccessErrorCallback, asset: Asset, removeTrust: Boolean, secretSeed: CharArray): AsyncTask<Void, Void, HorizonException?> {
    return ChangeTrust(listener, asset, removeTrust, secretSeed)
  }

  override fun getLoadAccountTask(listener: OnLoadAccount): AsyncTask<Void, Void, AccountResponse> {
    return LoadAccountTask(listener)
  }

  override fun deleteOffer(id: Long, secretSeed: CharArray, selling: Asset, buying: Asset, price: String, listener: Horizon.OnMarketOfferListener) {
    AsyncTask.execute {
      val server = getServer()
      val offerOperation = ManageSellOfferOperation.Builder(selling, buying, "0", price).setOfferId(id).build()
      val sourceKeyPair = KeyPair.fromSecretSeed(secretSeed)

      try {
        val sourceAccount = server.accounts().account(sourceKeyPair)

        val transaction = Transaction.Builder(sourceAccount).setTimeout(TIMEOUT_INFINITE).addOperation(offerOperation).build()
        transaction.sign(sourceKeyPair)
        val response = server.submitTransaction(transaction)

        Handler(Looper.getMainLooper()).post {
          if (response.isSuccess) {
            listener.onExecuted()
          } else {
            listener.onFailed(response.extras.resultCodes.operationsResultCodes[0].toString())
          }
        }
      } catch (error: Exception) {
        if (error.message != null) {
          listener.onFailed(error.message as String)
        } else {
          listener.onFailed("Unknown error")
        }
      }
    }
  }

  override fun registerForEffects(cursor: String, listener: EventListener<EffectResponse>): SSEStream<EffectResponse>? {
    val server = getServer()
    val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
    try {
      //ATTENTION STREAM must work with order.ASC!
      return server.effects()
        .cursor(cursor)
        .order(RequestBuilder.Order.ASC)
        .forAccount(sourceKeyPair).stream(listener)
    } catch (error: Exception) {
      Timber.e(error.message.toString())
    }
    return null
  }

  override fun registerForOperations(cursor: String, listener: EventListener<OperationResponse>): SSEStream<OperationResponse>? {
    val server = getServer()
    val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
    try {
      //ATTENTION STREAM must work with order.ASC!
      return server.operations()
        .cursor(cursor)
        .order(RequestBuilder.Order.ASC)
        .forAccount(sourceKeyPair).stream(listener)
    } catch (error: Exception) {
      Timber.e(error.message.toString())
    }
    return null
  }

  override fun registerForTransactions(cursor: String, listener: EventListener<TransactionResponse>): SSEStream<TransactionResponse>? {
    val server = getServer()
    val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
    try {
      //ATTENTION STREAM must work with order.ASC!
      return server.transactions()
        .cursor(cursor)
        .order(RequestBuilder.Order.ASC)
        .forAccount(sourceKeyPair).stream(listener)
    } catch (error: Exception) {
      Timber.e(error.message.toString())
    }
    return null
  }

  override fun registerForTrades(cursor: String, listener: EventListener<TradeResponse>): SSEStream<TradeResponse>? {
    val server = getServer()
    val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
    try {
      //ATTENTION STREAM must work with order.ASC!
      return (server.trades().order(RequestBuilder.Order.ASC) as TradesRequestBuilder)
        .cursor(cursor)
        .forAccount(sourceKeyPair).stream(listener)
    } catch (error: Exception) {
      Timber.e(error.message.toString())
    }
    return null
  }

  override fun getCreateMarketOffer(listener: OnMarketOfferListener,
                                    secretSeed: CharArray,
                                    sellingAsset: Asset,
                                    buyingAsset: Asset,
                                    amount: String,
                                    price: String) {
    AsyncTask.execute {
      try {
        val server = getServer()
        val managedOfferOperation = ManageSellOfferOperation.Builder(sellingAsset, buyingAsset, amount, price).build()
        val sourceKeyPair = KeyPair.fromSecretSeed(secretSeed)
        val sourceAccount = server.accounts().account(sourceKeyPair)
        val transaction = Transaction.Builder(sourceAccount).setTimeout(TIMEOUT_INFINITE).addOperation(managedOfferOperation).build()
        transaction.sign(sourceKeyPair)
        val response = server.submitTransaction(transaction)
        Handler(Looper.getMainLooper()).post {
          if (response.isSuccess) {
            listener.onExecuted()
          } else {
            val list = response.extras.resultCodes.operationsResultCodes
            if (list != null && list.isNotEmpty()) {
              listener.onFailed(list[0].toString())
            }
          }
        }
      } catch (ex: Exception) {
        Handler(Looper.getMainLooper()).post {
          listener.onFailed(ex.localizedMessage)
        }
      }
    }
  }

  override fun getOrderBook(listener: OnOrderBookListener, buyingAsset: DataAsset, sellingAsset: DataAsset) {
    AsyncTask.execute {
      val server = getServer()
      val buying: Asset = AssetUtils.toAssetFrom(buyingAsset)
      val selling: Asset = AssetUtils.toAssetFrom(sellingAsset)

      try {
        val response = server.orderBook().buyingAsset(buying).sellingAsset(selling).execute()
        Handler(Looper.getMainLooper()).post {
          listener.onOrderBook(response.asks, response.bids)
        }
      } catch (error: Exception) {
        error.message?.let {
          listener.onFailed(it)
        } ?: run {
          listener.onFailed("Failed to get the order book")
        }

      }
    }
  }

  override fun getOffers(listener: OnOffersListener) {
    LoadOffersTask(listener).execute()
  }

  private class LoadOffersTask(private val listener: OnOffersListener) : AsyncTask<Void, Void, ArrayList<OfferResponse>>() {
    var errorMessage: String = "failed to fetch the offers"
    override fun doInBackground(vararg params: Void?): ArrayList<OfferResponse>? {
      var list: ArrayList<OfferResponse>? = null
      val server = getServer()
      try {
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        val response = server.offers().forAccount(sourceKeyPair).execute()
        if (response != null) {
          list = response.records
        }
      } catch (error: Exception) {
        if (error !is NullPointerException) {
          Timber.d(error)
          error.message?.let {
            errorMessage = it
          }
        }
      }
      return list
    }

    override fun onPostExecute(result: ArrayList<OfferResponse>?) {
      result?.let {
        listener.onOffers(it)
      } ?: run {
        listener.onFailed(errorMessage)
      }
    }

  }

  private class LoadAccountTask(private val listener: OnLoadAccount) : AsyncTask<Void, Void, AccountResponse>() {
    var error: ErrorResponse? = null
    override fun doInBackground(vararg params: Void?): AccountResponse? {
      var account: AccountResponse? = null
      try {
        val server = getServer()
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        account = server.accounts().account(sourceKeyPair)

      } catch (ex: Exception) {
        if (ex !is NullPointerException) {
          Timber.d(ex.message.toString())
          error = if (ex is ErrorResponse) ex
          else ErrorResponse(Constants.UNKNOWN_ERROR, ex.message)
        }
      }

      return account
    }

    override fun onPostExecute(result: AccountResponse?) {
      error?.let {
        listener.onError(it)
      } ?: run {
        listener.onLoadAccount(result)
      }
    }
  }

  private class LoadEffectsTask(val cursor: String, val limit: Int, private val listener: OnLoadEffects) : AsyncTask<Void, Void, ArrayList<EffectResponse>?>() {
    var errorMessage: String? = null
    override fun doInBackground(vararg params: Void?): ArrayList<EffectResponse>? {
      val server = getServer()
      var effectResults: Page<EffectResponse>? = null
      try {
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        effectResults = server.effects().order(RequestBuilder.Order.DESC)
          .cursor(cursor)
          .limit(limit)
          .forAccount(sourceKeyPair).execute()
      } catch (error: Exception) {
        Timber.e(error.message.toString())
        errorMessage = error.message.toString()
      }

      return effectResults?.records
    }

    override fun onPostExecute(result: ArrayList<EffectResponse>?) {
      errorMessage?.let {
        listener.onError(it)
      } ?: run {
        listener.onLoadEffects(result)
      }
    }
  }

  private class LoadOperationsTask(val cursor: String, val limit: Int, private val listener: OnLoadOperations) : AsyncTask<Void, Void, ArrayList<Pair<OperationResponse, String?>>?>() {
    var errorMessage: String? = null
    override fun doInBackground(vararg params: Void?): ArrayList<Pair<OperationResponse, String?>>? {
      val server = getServer()
      var operationsResult: ArrayList<Pair<OperationResponse, String?>>? = null
      try {
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        operationsResult = server.operations().order(RequestBuilder.Order.DESC)
          .cursor(cursor)
          .limit(limit)
          .includeFailed(false)
          .forAccount(sourceKeyPair).execute()?.records?.filter {
          it.type == Operation.OperationType.CREATED.value ||
            it.type == Operation.OperationType.PAYMENT.value ||
            it.type == Operation.OperationType.CHANGE_TRUST.value ||
            it.type == Operation.OperationType.ALLOW_TRUST.value
        }?.map {
          Pair<OperationResponse, String?>(it, null)
        } as ArrayList<Pair<OperationResponse, String?>>

        if (operationsResult.isNotEmpty()) {
          val newCursor = operationsResult.last().first.pagingToken
          getLoadOperationsTask(newCursor, limit, listener).execute()
        } else {
          getLoadTransactionsTask("", limit, listener).execute()
        }
      } catch (error: Exception) {
        Timber.e(error.message.toString())
        errorMessage = error.message.toString()
      }

      return operationsResult
    }

    override fun onPostExecute(result: ArrayList<Pair<OperationResponse, String?>>?) {
      errorMessage?.let {
        listener.onError(it)
      } ?: run {
        listener.onLoadOperations(result, cursor)
      }
    }
  }

  private class LoadTransactionsTask(val cursor: String, val limit: Int, private val listener: OnLoadOperations) : AsyncTask<Void, Void, ArrayList<TransactionResponse>>() {
    var errorMessage: String? = null
    override fun doInBackground(vararg params: Void?): ArrayList<TransactionResponse> {
      val server = getServer()
      var transactionResults: ArrayList<TransactionResponse> = ArrayList()
      try {
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        transactionResults = server.transactions().order(RequestBuilder.Order.DESC)
          .cursor(cursor)
          .limit(limit)
          .includeFailed(false)
          .forAccount(sourceKeyPair).execute()?.records ?: ArrayList()

        if (transactionResults.isNotEmpty()) {
          val newCursor = transactionResults.last().pagingToken
          getLoadTransactionsTask(newCursor, limit, listener).execute()
        }

      } catch (error: Exception) {
        Timber.e(error.message.toString())
        errorMessage = error.message.toString()
      }

      return transactionResults
    }

    override fun onPostExecute(result: ArrayList<TransactionResponse>) {
      errorMessage?.let {
        listener.onError(it)
      } ?: run {
        listener.onLoadTransactions(result, cursor)
      }
    }
  }

  private class LoadTradesTask(val cursor: String, val limit: Int, private val listener: OnLoadTrades) : AsyncTask<Void, Void, ArrayList<TradeResponse>?>() {
    var errorMessage: String? = null
    override fun doInBackground(vararg params: Void?): ArrayList<TradeResponse>? {
      val server = getServer()
      var tradeResults: ArrayList<TradeResponse>? = ArrayList()
      try {
        val sourceKeyPair = KeyPair.fromAccountId(WalletApplication.wallet.getStellarAccountId()!!)
        tradeResults = (server.trades().order(RequestBuilder.Order.DESC) as TradesRequestBuilder)
          .cursor(cursor)
          .limit(limit)
          .forAccount(sourceKeyPair).execute()?.records ?: ArrayList()

        if (tradeResults.isNotEmpty()) {
          val newCursor = tradeResults.last().pagingToken
          getLoadTradesTask(newCursor, limit, listener).execute()
        }

      } catch (error: Exception) {
        Timber.e(error.message.toString())
        errorMessage = error.message.toString()
      }

      return tradeResults
    }

    override fun onPostExecute(result: ArrayList<TradeResponse>?) {
      errorMessage?.let {
        listener.onError(it)
      } ?: run {
        listener.onLoadTrades(result, cursor)
      }
    }
  }

  private class SendTask(private val listener: SuccessErrorCallback, private val destAddress: String,
                         private val secretSeed: CharArray, private val memo: String,
                         private val amount: String) : AsyncTask<Void, Void, HorizonException>() {

    override fun doInBackground(vararg params: Void?): HorizonException? {
      try {
        val server = getServer()
        val sourceKeyPair = KeyPair.fromSecretSeed(secretSeed)
        val destKeyPair = KeyPair.fromAccountId(destAddress)
        var isCreateAccount = false

        try {
          server.accounts().account(destKeyPair)
        } catch (error: Exception) {
          Timber.e(error.message.toString())
          if (error is ErrorResponse && error.code == 404) {
            isCreateAccount = true
          } else {
            return HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
              arrayListOf(error.message),
              HorizonException.HorizonExceptionType.SEND)
          }
        }

        val sourceAccount = server.accounts().account(sourceKeyPair)

        val transactionBuilder = Transaction.Builder(sourceAccount).setTimeout(TIMEOUT_INFINITE)
        if (isCreateAccount) {
          transactionBuilder.addOperation(CreateAccountOperation.Builder(destKeyPair, amount).build())
        } else {
          transactionBuilder.addOperation(PaymentOperation.Builder(destKeyPair, getCurrentAsset(), amount).build())
        }

        if (memo.isNotEmpty()) {
          transactionBuilder.addMemo(Memo.text(memo))
        }

        val transaction = transactionBuilder.build()
        transaction.sign(sourceKeyPair)

        val response = server.submitTransaction(transaction)
        if (!response.isSuccess) {
          return HorizonException(response.extras.resultCodes.transactionResultCode,
            response.extras.resultCodes.operationsResultCodes,
            HorizonException.HorizonExceptionType.SEND)
        }
      } catch (error: Exception) {
        Timber.d(error.message.toString())
        return HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
          arrayListOf(error.message.toString()),
          HorizonException.HorizonExceptionType.SEND)
      }
      return null
    }

    override fun onPostExecute(result: HorizonException?) {
      if (result != null) {
        listener.onError(result)
      } else {
        listener.onSuccess()
      }
    }
  }

  private class JoinInflationDestination(private val listener: SuccessErrorCallback,
                                         private val secretSeed: CharArray,
                                         private val inflationDest: String)
    : AsyncTask<Void, Void, HorizonException>() {

    override fun doInBackground(vararg params: Void?): HorizonException? {
      try {
        val server = getServer()
        val sourceKeyPair = KeyPair.fromSecretSeed(secretSeed)
        val destKeyPair = KeyPair.fromAccountId(inflationDest)
        val sourceAccount = server.accounts().account(sourceKeyPair)
        val transaction = Transaction.Builder(sourceAccount).setTimeout(TIMEOUT_INFINITE)
          .addOperation(SetOptionsOperation.Builder()
            .setInflationDestination(destKeyPair)
            .build())
          .build()

        transaction.sign(sourceKeyPair)
        val response = server.submitTransaction(transaction)

        if (!response.isSuccess) {
          return HorizonException(response.extras.resultCodes.transactionResultCode,
            response.extras.resultCodes.operationsResultCodes,
            HorizonException.HorizonExceptionType.INFLATION)
        }

      } catch (error: Exception) {
        Timber.e(error.message.toString())
        return HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
          arrayListOf(error.message.toString()),
          HorizonException.HorizonExceptionType.INFLATION)
      }
      return null
    }

    override fun onPostExecute(result: HorizonException?) {
      if (result != null) {
        listener.onError(result)
      } else {
        listener.onSuccess()
      }
    }
  }

  private class ChangeTrust(private val listener: SuccessErrorCallback, private val asset: Asset,
                            private val removeTrust: Boolean, private val secretSeed: CharArray)
    : AsyncTask<Void, Void, HorizonException?>() {

    override fun doInBackground(vararg params: Void?): HorizonException? {
      try {
        val server = getServer()
        val sourceKeyPair = KeyPair.fromSecretSeed(secretSeed)
        val limit = if (removeTrust) "0.0000000" else Constants.MAX_ASSET_STRING_VALUE
        val sourceAccount = server.accounts().account(sourceKeyPair)
        val transaction = Transaction.Builder(sourceAccount).setTimeout(TIMEOUT_INFINITE)
          .addOperation(ChangeTrustOperation.Builder(asset, limit).build())
          .build()

        transaction.sign(sourceKeyPair)
        val response = server.submitTransaction(transaction)

        if (!response.isSuccess) {
          return HorizonException(response.extras.resultCodes.transactionResultCode,
            response.extras.resultCodes.operationsResultCodes,
            HorizonException.HorizonExceptionType.CHANGE_TRUST_LINE)
        }

      } catch (error: ErrorResponse) {
        Timber.e(error.body.toString())
        return HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
          arrayListOf(error.body.toString()),
          HorizonException.HorizonExceptionType.CHANGE_TRUST_LINE)
      } catch (error: Exception) {
        return when (error) {
          is ConnectException -> HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
            arrayListOf("connection_problem"), HorizonException.HorizonExceptionType.CHANGE_TRUST_LINE)
          is SubmitTransactionTimeoutResponseException ->
            HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
              arrayListOf("tx_timeout"), HorizonException.HorizonExceptionType.CHANGE_TRUST_LINE)
          else -> HorizonException(Constants.DEFAULT_TRANSACTION_FAILED_CODE,
            arrayListOf(error.message), HorizonException.HorizonExceptionType.CHANGE_TRUST_LINE)
        }
      }
      return null
    }

    override fun onPostExecute(result: HorizonException?) {
      if (result != null) {
        listener.onError(result)
      } else {
        listener.onSuccess()
      }
    }
  }

  interface OnMarketOfferListener {
    fun onExecuted()
    fun onFailed(errorMessage: String)
  }

  interface OnOrderBookListener {
    fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>)
    fun onFailed(errorMessage: String)
  }

  interface OnOffersListener {
    fun onOffers(offers: ArrayList<OfferResponse>)
    fun onFailed(errorMessage: String)
  }

  private fun getCurrentAsset(): Asset {
    val assetCode = WalletApplication.userSession.getSessionAsset().assetCode
    val assetIssuer = WalletApplication.userSession.getSessionAsset().assetIssuer

    return if (assetCode == Constants.LUMENS_ASSET_TYPE) {
      AssetTypeNative()
    } else {
      Asset.createNonNativeAsset(assetCode, KeyPair.fromAccountId(assetIssuer))
    }
  }

  /**
   * HORIZON_SUBMIT_TIMEOUT is a time in seconds after Horizon sends a timeout response
   * after internal txsub timeout.
   */
  private const val HORIZON_SUBMIT_TIMEOUT = 60L

  private fun getServer(): Server {
    checkNotNull(HORIZON_SERVER, lazyMessage = { "Horizon server has not been initialized, please call {${this::class.java}#initFcm(..)" })
    return HORIZON_SERVER
  }

  private fun createServer(serverAddress: String): Server {
    val server = Server(serverAddress)
    val httpClient = OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()

    val submitHttpClient = OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(HORIZON_SUBMIT_TIMEOUT + 5, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()

    server.httpClient = httpClient
    server.submitHttpClient = submitHttpClient

    return server
  }
}
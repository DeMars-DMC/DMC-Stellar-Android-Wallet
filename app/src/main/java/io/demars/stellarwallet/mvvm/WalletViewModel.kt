package io.demars.stellarwallet.mvvm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants.Companion.DEFAULT_ACCOUNT_BALANCE
import io.demars.stellarwallet.interfaces.StellarAccount
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.local.OperationsRepository
import io.demars.stellarwallet.mvvm.local.TradesRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.jetbrains.anko.doAsync
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber

class WalletViewModel(application: Application) : AndroidViewModel(application) {
  @SuppressLint("StaticFieldLeak")
  private val applicationContext: Context = application.applicationContext
  private var sessionAsset: SessionAsset = DefaultAsset()

  private val operationsRepository: OperationsRepository = OperationsRepository.getInstance()
  private val tradesRepository: TradesRepository = TradesRepository.getInstance()

  private var stellarAccount: StellarAccount? = null
  private var operationsListResponse: ArrayList<Pair<OperationResponse, String?>>? = null
  private var tradesListResponse: ArrayList<TradeResponse>? = null

  private var walletViewState: MutableLiveData<WalletViewState> = MutableLiveData()
  private var state: WalletState = WalletState.UPDATING

  private var handler = Handler()
  private var runnableCode: Runnable? = null

  init {
    WalletApplication.assetSession.observeForever {
      if (it != null) {
        sessionAsset = it
        notifyViewState()
      }
    }

    operationsRepository.loadList().observeForever {
      Timber.d("operations repository, observer triggered")
      if (it != null) {
        operationsListResponse = it
        state = WalletState.ACTIVE
        TradesRepository.getInstance().forceRefresh()
      }
    }

    tradesRepository.loadList().observeForever {
      Timber.d("trades repository, observer triggered")
      if (it != null) {
        tradesListResponse = it
        state = WalletState.ACTIVE
        notifyViewState()
      }
    }

    AccountRepository.refreshData().observeForever {
      if (it != null) {
        when (it.httpCode) {
          200 -> {
            val immutableAccount = this.stellarAccount
            if (immutableAccount == null
              || immutableAccount.basicHashCode() != it.stellarAccount.basicHashCode()
              || state != WalletState.ACTIVE) {
              stellarAccount = it.stellarAccount
              state = WalletState.ACTIVE

              OperationsRepository.getInstance().forceRefresh()
            } else {
              //let's ignore this response
              Timber.d("ignoring account response")
            }
          }
          404 -> {
            stellarAccount = it.stellarAccount
            state = WalletState.NOT_FUNDED
          }
          else -> {
            state = WalletState.ERROR
          }
        }

        notifyViewState()
      }
    }
  }

  fun forceRefresh() {
    state = WalletState.UPDATING
    notifyViewState()

    if (NetworkUtils(applicationContext).isNetworkAvailable()) doAsync {
      AccountRepository.refresh()
    }
  }

  fun walletViewState(forceRefresh: Boolean): MutableLiveData<WalletViewState> {
    // it does not need to refresh since polling will try to get an active account
    if (forceRefresh) {
      forceRefresh()
    }

    return walletViewState
  }

  private fun notifyViewState() {
    Timber.d("Notifying state {$state}")

    val accountId = WalletApplication.wallet.getStellarAccountId()!!
    when (state) {
      WalletState.ACTIVE -> {
        if (stellarAccount == null) return
        if (operationsListResponse.isNullOrEmpty()) return

        val availableBalance = getAvailableBalance()
        val totalAvailableBalance = getTotalAssetBalance()
        walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE,
          accountId, sessionAsset.assetCode, availableBalance, totalAvailableBalance,
          operationsListResponse, tradesListResponse))

      }
      WalletState.ERROR -> {
        walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ERROR,
          accountId, sessionAsset.assetCode,
          null, null, null, null))
      }
      WalletState.NOT_FUNDED -> {
        val availableBalance = AvailableBalance("XLM", null, DEFAULT_ACCOUNT_BALANCE)
        val totalAvailableBalance = TotalBalance(state, "Lumens", "XLM", DEFAULT_ACCOUNT_BALANCE)
        walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.UNFUNDED,
          accountId, sessionAsset.assetCode, availableBalance, totalAvailableBalance,
          null, null))
      }
      else -> {
        // nothing
      }
    }
  }

  private fun getAvailableBalance(): AvailableBalance {
    val assetCode = if (sessionAsset.assetCode == "native") "XLM" else sessionAsset.assetCode
    val decimalPlaces = AssetUtils.getDecimalPlaces(assetCode)
    val available = AccountUtils.getAvailableBalance(assetCode)
    val totalAvailable = truncateDecimalPlaces(available, decimalPlaces)
    return AvailableBalance(assetCode, sessionAsset.assetIssuer, totalAvailable)
  }

  private fun getTotalAssetBalance(): TotalBalance {
    val currAsset = if (sessionAsset.assetCode == "native") "XLM" else sessionAsset.assetCode
    val decimalPlaces = AssetUtils.getDecimalPlaces(currAsset)
    val assetBalance = truncateDecimalPlaces(AccountUtils.getTotalBalance(currAsset), decimalPlaces)
    return TotalBalance(WalletState.ACTIVE, sessionAsset.assetName, sessionAsset.assetCode, assetBalance)
  }

  fun moveToForeGround() {
    startPolling()
  }

  fun moveToBackground() {
    stopPolling()
  }

  private fun stopPolling() {
    Timber.d("disabling polling")
    synchronized(this) {
      handler.removeCallbacks(runnableCode)
    }
  }

  private fun startPolling() {
    synchronized(this) {
      Timber.d("starting polling")
      runnableCode = Runnable {
        Timber.d("starting pulling cycle")
        if (NetworkUtils(applicationContext).isNetworkAvailable()) AccountRepository.refresh()
      }
      handler.post(runnableCode)
    }
  }
}

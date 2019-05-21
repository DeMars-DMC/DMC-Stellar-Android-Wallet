package io.demars.stellarwallet.mvvm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants.Companion.DEFAULT_ACCOUNT_BALANCE
import io.demars.stellarwallet.interfaces.StellarAccount
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.balance.BalanceRepository
import io.demars.stellarwallet.mvvm.local.OperationsRepository
import io.demars.stellarwallet.mvvm.local.TradesRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.jetbrains.anko.doAsync
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber

class WalletViewModelPolling(application: Application) : AndroidViewModel(application) {
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
  private var handlerMain = Handler(Looper.getMainLooper())
  private var runnableCode: Runnable? = null
  private var pollingStarted = false
  private var balance: BalanceAvailability? = null

  init {
    WalletApplication.assetSession.observeForever {
      if (it != null) {
        sessionAsset = it
        notifyViewState()
      }
    }

    operationsRepository.loadList(false).observeForever {
      Timber.d("effects repository, observer triggered")
      if (it != null) {
        operationsListResponse = it
        state = WalletState.ACTIVE
        notifyViewState()
      }
    }

    tradesRepository.loadList(false).observeForever {
      Timber.d("effects repository, observer triggered")
      if (it != null) {
        tradesListResponse = it
        if (stellarAccount != null && operationsListResponse != null) {
          state = WalletState.ACTIVE
          notifyViewState()
        }
      }
    }

    BalanceRepository.loadBalance().observeForever {
      balance = it
      if (it != null && operationsListResponse != null) {
        Timber.d("new balance received")
        state = WalletState.ACTIVE
        notifyViewState()
      }
      Timber.d("new balance --> Refreshing Effects")
      //TODO: PROBABLY THIS WHY TIMELINE IS NOT UPDATING
//      EffectsRepository.getInstance().forceRefresh()
    }
  }

  fun forceRefresh() {
    state = WalletState.UPDATING
    doAsync {
      loadAccount(true)
    }
  }

  fun walletViewState(forceRefresh: Boolean): MutableLiveData<WalletViewState> {
    // it does not need to refresh since polling will try to get an active account
    if (forceRefresh) {
      forceRefresh()
    }
    return walletViewState
  }

  private fun loadAccount(notify: Boolean) {
    Timber.d("Loading account, notify {$notify}")
    handlerMain.post {
      AccountRepository.loadAccountEvent().observeForever {
        if (it != null) {
          when (it.httpCode) {
            200 -> {
              val immutableAccount = this.stellarAccount
              if (immutableAccount == null
                || immutableAccount.basicHashCode() != it.stellarAccount.basicHashCode()
                || state != WalletState.ACTIVE) {
                stellarAccount = it.stellarAccount
                if (operationsListResponse != null && tradesListResponse != null) {
                  state = WalletState.ACTIVE
                  notifyViewState()
                }
              } else {
                //let's ignore this response
                Timber.d("ignoring account response")
              }
            }
            404 -> {
              stellarAccount = it.stellarAccount
              state = WalletState.NOT_FUNDED
              notifyViewState()
              if (!pollingStarted) {
                startPolling()
              }
            }
            else -> {
              state = WalletState.ERROR
              if (!pollingStarted) {
                startPolling()
              }
            }
          }
        }
      }
    }
  }

  private fun notifyViewState() {
    Timber.d("Notifying state {$state}")
    stellarAccount?.let {
      when (state) {
        WalletState.ACTIVE -> {
          balance?.let {
            val availableBalance = getAvailableBalance(it)
            val totalAvailableBalance = getTotalAssetBalance()
            walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE,
              it.getAccountId(), sessionAsset.assetCode, availableBalance, totalAvailableBalance,
              operationsListResponse, tradesListResponse))
          }
        }
        WalletState.ERROR -> {
          walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ERROR,
            it.getAccountId(), sessionAsset.assetCode,
            null, null, null, null))
        }
        WalletState.NOT_FUNDED -> {
          val availableBalance = AvailableBalance("XLM", null, DEFAULT_ACCOUNT_BALANCE)
          val totalAvailableBalance = TotalBalance(state, "Lumens", "XLM", DEFAULT_ACCOUNT_BALANCE)
          walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.UNFUNDED,
            it.getAccountId(), sessionAsset.assetCode, availableBalance, totalAvailableBalance,
            null, null))
        }
        else -> {
          // nothing
        }
      }
    }
  }

  private fun getAvailableBalance(balance: BalanceAvailability): AvailableBalance {
    val totalAvailable = if (sessionAsset.assetCode == "native") {
      truncateDecimalPlaces(balance.getNativeAssetAvailability().totalAvailable.toString())
    } else {
      truncateDecimalPlaces(balance.getAssetAvailability(sessionAsset.assetCode, sessionAsset.assetIssuer).totalAvailable.toString())
    }
    return AvailableBalance(sessionAsset.assetCode, sessionAsset.assetIssuer, totalAvailable)
  }

  private fun getTotalAssetBalance(): TotalBalance {
    val currAsset = sessionAsset.assetCode
    val assetBalance = truncateDecimalPlaces(AccountUtils.getTotalBalance(currAsset))
    return TotalBalance(WalletState.ACTIVE, sessionAsset.assetName, sessionAsset.assetCode, assetBalance)
  }


  fun moveToForeGround() {
    startPolling()
  }

  fun moveToBackground() {
    Timber.d("disabling polling")
    synchronized(this) {
      handler.removeCallbacks(runnableCode)
      pollingStarted = false
    }
  }

  private fun startPolling() {
    if (pollingStarted) return
    synchronized(this) {
      pollingStarted = true
      Timber.d("starting polling")
      runnableCode = object : Runnable {
        override fun run() {
          Timber.d("starting pulling cycle")
          when {
            NetworkUtils(applicationContext).isNetworkAvailable() -> loadAccount(true)
          }
          handler.postDelayed(this, 10000)
        }
      }
      handler.post(runnableCode)
    }
  }
}

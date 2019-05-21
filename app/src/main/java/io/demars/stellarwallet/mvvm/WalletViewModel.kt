package io.demars.stellarwallet.mvvm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants.Companion.DEFAULT_ACCOUNT_BALANCE
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.balance.BalanceRepository
import io.demars.stellarwallet.mvvm.local.OperationsRepository
import io.demars.stellarwallet.mvvm.local.TradesRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.jetbrains.anko.doAsync
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber

class WalletViewModel(application: Application) : AndroidViewModel(application) {
  @SuppressLint("StaticFieldLeak")
  private val applicationContext: Context = application.applicationContext
  private var sessionAsset: SessionAsset = DefaultAsset()

  private val operationsRepository: OperationsRepository = OperationsRepository.getInstance()
  private val tradesRepository: TradesRepository = TradesRepository.getInstance()

  private var accountResponse: AccountResponse? = null
  private var operationsListResponse: ArrayList<Pair<OperationResponse, String?>>? = null
  private var tradesListResponse: ArrayList<TradeResponse>? = null

  private var walletViewState: MutableLiveData<WalletViewState> = MutableLiveData()
  private var state: WalletState = WalletState.UPDATING

  private var handler = Handler()
  private var runnableCode: Runnable? = null

  private var pollingStarted = false

  private var balance: BalanceAvailability? = null

  init {
    loadAccount(false)

    operationsRepository.loadList(false).observeForever {
      Timber.d("operations repository, observer triggered")
      if (it != null) {
        operationsListResponse = it
        if (accountResponse != null && tradesListResponse != null) {
          state = WalletState.ACTIVE
          notifyViewState()
        }
      }
    }

    tradesRepository.loadList(false).observeForever {
      Timber.d("trades repository, observer triggered")
      if (it != null) {
        tradesListResponse = it
        if (accountResponse != null && operationsListResponse != null) {
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

    WalletApplication.assetSession.observeForever {
      if (it != null) {
        sessionAsset = it
        notifyViewState()
      }
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
    AccountRepository.loadAccountEvent().observeForever {
      if (it != null) {
        when (it.httpCode) {
          200 -> {
            accountResponse = it.stellarAccount.getAccountResponse()
            if (operationsListResponse != null && tradesListResponse != null) {
              state = WalletState.ACTIVE
              notifyViewState()
            }
          }
          404 -> {
            accountResponse = null
            state = WalletState.NOT_FUNDED
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

        if (notify) {
          notifyViewState()
        }
      }
    }
  }

  private fun notifyViewState() {
    Timber.d("Notifying state {$state}")
    val accountId = WalletApplication.wallet.getStellarAccountId()!!
    when (state) {
      WalletState.ACTIVE -> {
        balance?.let {
          val availableBalance = getAvailableBalance(it)
          val totalAvailableBalance = getTotalAssetBalance()
          walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE,
            accountId, sessionAsset.assetCode, availableBalance, totalAvailableBalance,
            operationsListResponse, tradesListResponse))
        }
      }
      WalletState.ERROR -> {
        walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ERROR, accountId,
          sessionAsset.assetCode, null, null, null, null))
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
      if (state != WalletState.ACTIVE) {
        pollingStarted = true
        Timber.d("starting polling")
        runnableCode = object : Runnable {
          override fun run() {
            Timber.d("starting pulling cycle")
            when {
              state == WalletState.ACTIVE -> {
                Timber.d("polling cancelled"); return
              }
              NetworkUtils(applicationContext).isNetworkAvailable() -> loadAccount(true)
            }
            handler.postDelayed(this, 4000)
          }
        }
        handler.post(runnableCode)
      }
    }
  }
}

package io.demars.stellarwallet.mvvm.effects

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
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.jetbrains.anko.doAsync
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.effects.EffectResponse
import timber.log.Timber

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val applicationContext : Context = application.applicationContext
    private val effectsRepository : EffectsRepository = EffectsRepository.getInstance()
    private val accountRepository : AccountRepository = AccountRepository()
    private var walletViewState: MutableLiveData<WalletViewState> = MutableLiveData()
    private var accountResponse: AccountResponse? = null
    private var effectsListResponse: ArrayList<EffectResponse>? = null
    private var state: WalletState = WalletState.UPDATING
    private var pollingStarted = false
    private var handler = Handler()
    private var runnableCode : Runnable? = null
    private var sessionAsset : io.demars.stellarwallet.models.SessionAsset = DefaultAsset()

    init {
        loadAccount(false)

        effectsRepository.loadList(false).observeForever {
            Timber.d("effects repository, observer triggered")
            if (it != null) {
                var toNotify = true
                effectsListResponse = it
                if (state != WalletState.ACTIVE && accountResponse != null) {
                    state = WalletState.ACTIVE
                } else if(state != WalletState.ACTIVE) {
                    loadAccount(true)
                    toNotify = false
                }
                if (toNotify) {
                    notifyViewState()
                }
            }
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
            effectsRepository.forceRefresh()
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
        accountRepository.loadAccount().observeForever {
            if (it != null) {
                when (it.httpCode) {
                    200 -> {
                        accountResponse = it.stellarAccount.getAccountResponse()
                        if (effectsListResponse != null) {
                            state = WalletState.ACTIVE
                        } else if(state != WalletState.ACTIVE){
                            effectsRepository.forceRefresh()
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
                        // disabling the ui of ERROR since without pull to refresh makes no sense
                        // state = WalletState.ERROR
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
        when(state) {
            WalletState.ACTIVE -> {
                val availableBalance = getAvailableBalance()
                val totalAvailableBalance = getTotalAssetBalance()
                walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE, accountId,  sessionAsset.assetCode, availableBalance, totalAvailableBalance, effectsListResponse))
            }
           // WalletState.ERROR -> {
           //      walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ERROR, accountId,  sessionAsset.assetCode, null, null, null))
           // }
            WalletState.NOT_FUNDED -> {
                val availableBalance = AvailableBalance("XLM", DEFAULT_ACCOUNT_BALANCE)
                val totalAvailableBalance = TotalBalance(state, "Lumens", "XLM", DEFAULT_ACCOUNT_BALANCE)
                walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.UNFUNDED, accountId, sessionAsset.assetCode, availableBalance, totalAvailableBalance, null))
            } else -> {
                // nothing
            }
        }
    }

    private fun getAvailableBalance() : AvailableBalance {
        val currAsset = sessionAsset.assetCode
        val balance = truncateDecimalPlaces(WalletApplication.wallet.getAvailableBalance())
        return AvailableBalance(currAsset, balance)
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
                            state == WalletState.ACTIVE -> { Timber.d("polling cancelled") ; return }
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

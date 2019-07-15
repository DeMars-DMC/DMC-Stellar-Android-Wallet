package io.demars.stellarwallet.mvvm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.helpers.Constants.Companion.DEFAULT_ACCOUNT_BALANCE
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.local.OperationsRepository
import io.demars.stellarwallet.mvvm.local.TradesRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.NetworkUtils
import io.demars.stellarwallet.utils.StringFormat.Companion.truncateDecimalPlaces
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber

class WalletViewModel(application: Application) : AndroidViewModel(application) {
  @SuppressLint("StaticFieldLeak")
  private val context: Context = application.applicationContext
  private var sessionAsset: SessionAsset = DefaultAsset()

  private val operationsRepository: OperationsRepository = OperationsRepository.getInstance()
  private val tradesRepository: TradesRepository = TradesRepository.getInstance()

  private var stellarAccount: StellarAccount? = null
  private var operationsListResponse: ArrayList<Pair<OperationResponse, String?>>? = null
  private var tradesListResponse: ArrayList<TradeResponse>? = null

  private var walletViewState: MutableLiveData<WalletViewState> = MutableLiveData()

  private var canRefresh = false
  private var needNotify = false

  init {
    DmcApp.assetSession.observeForever {
      if (it != null) {
        sessionAsset = it
        notifyViewState()
      }
    }

    operationsRepository.listLiveData().observeForever {
      Timber.d("operations repository, observer triggered")
      if (it != null) {
        operationsListResponse = it
        notifyViewState()
      }
    }

    tradesRepository.listLiveData().observeForever {
      Timber.d("trades repository, observer triggered")
      if (it != null) {
        tradesListResponse = it
        OperationsRepository.getInstance().forceRefresh()
      }
    }

    AccountRepository.refreshData().observeForever {
      if (canRefresh && it != null) {
        when (it.httpCode) {
          200 -> {
            val immutableAccount = this.stellarAccount
            if (needNotify || immutableAccount == null
              || !immutableAccount.basicEquals(it.stellarAccount)) {
              stellarAccount = it.stellarAccount
              needNotify = false

              TradesRepository.getInstance().forceRefresh()
            } else {
              //let's ignore this response
              Timber.d("ignoring account response")
            }
          }
          404 -> {
            stellarAccount = it.stellarAccount
            needNotify = true
          }
          else -> {
            needNotify = true
          }
        }

        notifyViewState()
      }
    }
  }

  fun forceRefresh() {
    needNotify = true
    notifyViewState()

    if (NetworkUtils(context).isNetworkAvailable()) AccountRepository.refresh()
  }

  fun walletViewState(forceRefresh: Boolean): MutableLiveData<WalletViewState> {
    // it does not need to refresh since polling will try to get an active account
    if (forceRefresh) {
      forceRefresh()
    }

    return walletViewState
  }

  private fun notifyViewState() {
    Timber.d("Notifying wallet state")

    val accountId = DmcApp.wallet.getStellarAccountId()!!
    if (stellarAccount == null) return
    if (operationsListResponse.isNullOrEmpty()) return

    val availableBalance = getAvailableBalance()
    val totalAvailableBalance = getTotalAssetBalance()
    walletViewState.postValue(WalletViewState(WalletViewState.AccountStatus.ACTIVE,
      accountId, sessionAsset.assetCode, availableBalance, totalAvailableBalance,
      operationsListResponse, tradesListResponse))
  }

  private fun getAvailableBalance(): AvailableBalance {
    val assetCode = if (sessionAsset.assetCode == "native") "XLM" else sessionAsset.assetCode
    val decimalPlaces = AssetUtils.getMaxDecimals(assetCode)
    val available = AccountUtils.getAvailableBalance(assetCode)
    val totalAvailable = truncateDecimalPlaces(available, decimalPlaces)
    return AvailableBalance(assetCode, sessionAsset.assetIssuer, totalAvailable)
  }

  private fun getTotalAssetBalance(): TotalBalance {
    val currAsset = if (sessionAsset.assetCode == "native") "XLM" else sessionAsset.assetCode
    val decimalPlaces = AssetUtils.getMaxDecimals(currAsset)
    val assetBalance = truncateDecimalPlaces(AccountUtils.getTotalBalance(currAsset), decimalPlaces)
    return TotalBalance(sessionAsset.assetName, sessionAsset.assetCode, assetBalance)
  }

  fun moveToForeGround() {
    canRefresh = true

    if (NetworkUtils(context).isNetworkAvailable()) AccountRepository.refresh()
  }

  fun moveToBackground() {
    canRefresh = false
  }


}

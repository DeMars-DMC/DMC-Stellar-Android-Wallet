package io.demars.stellarwallet.mvvm.balance

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import io.demars.stellarwallet.models.BalanceAvailability

class BalanceViewModel(application: Application) : AndroidViewModel(application) {
  @SuppressLint("StaticFieldLeak")
  private lateinit var liveData : LiveData<BalanceAvailability>

  fun loadBalance(): LiveData<BalanceAvailability> {
    if (!::liveData.isInitialized) liveData = BalanceRepository.loadBalance()
    return liveData
  }
}
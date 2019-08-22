package io.demars.stellarwallet.models

import android.content.Context
import io.demars.stellarwallet.api.horizon.model.MinimumBalance

interface UserSession {
  fun getSessionAsset(): SessionAsset
  fun setSessionAsset(sessionAsset: SessionAsset)
  fun getPin(): String?
  fun setPin(pin: String?)
  fun getFormattedCurrentAssetCode(): String?
  fun getFormattedCurrentAvailableBalance(context: Context): String?
  fun getAvailableBalance(): String?
  fun setMinimumBalance(minimumBalance: MinimumBalance)
  fun getMinimumBalance(): MinimumBalance?
}

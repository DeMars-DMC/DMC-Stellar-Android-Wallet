package io.demars.stellarwallet.models

import android.content.Context

interface UserSession {
    fun getSessionAsset(): io.demars.stellarwallet.models.SessionAsset
    fun setSessionAsset(sessionAsset: io.demars.stellarwallet.models.SessionAsset)
    fun getPin(): String?
    fun setPin(pin: String?)
    fun getFormattedCurrentAssetCode(): String?
    fun getFormattedCurrentAvailableBalance(context: Context): String?
    fun getAvailableBalance(): String?
    fun setMinimumBalance(minimumBalance: MinimumBalance)
    fun getMinimumBalance(): MinimumBalance?
}

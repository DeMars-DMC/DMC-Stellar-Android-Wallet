package io.demars.stellarwallet.models

import android.content.Context
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat

class UserSessionImpl : UserSession {

  override fun setMinimumBalance(minimumBalance: MinimumBalance) {
    minimumBalanceSession = minimumBalance
  }

  override fun getMinimumBalance(): MinimumBalance? {
    return minimumBalanceSession
  }

  private var asset: SessionAsset = DefaultAsset()

  override fun getSessionAsset(): SessionAsset {
    return asset
  }

  override fun setSessionAsset(sessionAsset: SessionAsset) {
    asset = sessionAsset
  }

  override fun setPin(pin: String?) {
    sessionPin = pin
  }

  private var minimumBalanceSession: MinimumBalance? = null
  private var sessionPin: String? = null

  override fun getPin(): String? {
    return sessionPin
  }

  override fun getFormattedCurrentAssetCode(): String {
    return StringFormat.formatAssetCode(getSessionAsset().assetCode)
  }

  @Suppress("UNUSED_PARAMETER")
  override fun getFormattedCurrentAvailableBalance(context: Context): String {
    val assetCode = getFormattedCurrentAssetCode()
    val decimalNumbers = AssetUtils.getDecimalPlaces(assetCode)
    return StringFormat.truncateDecimalPlaces(getAvailableBalance(), decimalNumbers) +
      " " + getFormattedCurrentAssetCode()
  }

  override fun getAvailableBalance(): String {
    return if (getSessionAsset().assetCode == Constants.LUMENS_ASSET_TYPE) {
      WalletApplication.wallet.getAvailableBalance()
    } else {
      AccountUtils.getTotalBalance(getFormattedCurrentAssetCode())
    }
  }
}

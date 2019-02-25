package io.demars.stellarwallet.models

import android.content.Context
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat
import java.text.DecimalFormat

class UserSessionImpl : UserSession {

    override fun setMinimumBalance(minimumBalance: MinimumBalance) {
        minimumBalanceSession = minimumBalance
    }

    override fun getMinimumBalance(): MinimumBalance? {
       return minimumBalanceSession
    }

    private var asset : io.demars.stellarwallet.models.SessionAsset = DefaultAsset()

    override fun getSessionAsset(): io.demars.stellarwallet.models.SessionAsset {
      return asset
    }

    override fun setSessionAsset(sessionAsset: io.demars.stellarwallet.models.SessionAsset){
        asset = sessionAsset
    }

    override fun setPin(pin:String?) {
      sessionPin = pin
    }

    private val decimalFormat : DecimalFormat = DecimalFormat("0.#######")

    private var minimumBalanceSession: MinimumBalance? = null
    private var sessionPin: String? = null

    override fun getPin(): String? {
        return sessionPin
    }
    override fun getFormattedCurrentAssetCode() : String {
        return StringFormat.formatAssetCode(getSessionAsset().assetCode)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun getFormattedCurrentAvailableBalance(context: Context): String {
        return decimalFormat.format(getAvailableBalance().toDouble()) + " " + getFormattedCurrentAssetCode()
    }

    override fun getAvailableBalance(): String {
        return if (getSessionAsset().assetCode == Constants.LUMENS_ASSET_TYPE) {
            WalletApplication.wallet.getAvailableBalance()
        } else {
            AccountUtils.getTotalBalance(getFormattedCurrentAssetCode())
        }
    }
}

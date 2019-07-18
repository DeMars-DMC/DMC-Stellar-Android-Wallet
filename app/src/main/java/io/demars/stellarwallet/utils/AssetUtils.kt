package io.demars.stellarwallet.utils

import android.content.Context
import io.demars.stellarwallet.Preferences
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.SelectionModel
import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeCreditAlphaNum12
import org.stellar.sdk.AssetTypeCreditAlphaNum4
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.xdr.AssetType

class AssetUtils {
  companion object {
    const val NATIVE_ASSET_CODE = "XLM"

    fun toAssetFrom(dataAsset: DataAsset): Asset {
      return if (dataAsset.type == "native") {
        AssetTypeNative()
      } else {
        Asset.create(dataAsset.type, dataAsset.code, dataAsset.issuer)
      }
    }

    fun toDataAssetFrom(selection: SelectionModel): DataAsset? {
      val asset = selection.asset
      if (asset != null) {
        return toDataAssetFrom(asset)
      }
      return null
    }

    fun toDataAssetFrom(asset: Asset): DataAsset? {
      var dataAsset: DataAsset? = null
      val assetType = asset.toXdr().discriminant
      if (assetType != null) {
        dataAsset = when (assetType) {
          AssetType.ASSET_TYPE_CREDIT_ALPHANUM12 -> {
            val concreteAsset = asset as AssetTypeCreditAlphaNum12
            DataAsset(concreteAsset.type, concreteAsset.code, concreteAsset.issuer.accountId)
          }
          AssetType.ASSET_TYPE_CREDIT_ALPHANUM4 -> {
            val concreteAsset = asset as AssetTypeCreditAlphaNum4
            DataAsset(concreteAsset.type, concreteAsset.code, concreteAsset.issuer.accountId)
          }
          AssetType.ASSET_TYPE_NATIVE -> {
            val concreteAsset = asset as AssetTypeNative
            DataAsset(concreteAsset.type, NATIVE_ASSET_CODE, "null")
          }
        }
      }
      return dataAsset
    }

    fun isReporting(context: Context, balance: AccountResponse.Balance): Boolean {
      val reportingAsset = getDataAssetFromPrefs(context)
      return (balance.assetCode == reportingAsset.code &&
        balance.assetIssuer.accountId == reportingAsset.issuer)
        || (balance.assetType == "native" && reportingAsset.type == "native")
    }

    fun saveDateAssetToPrefs(context: Context, dataAsset: DataAsset) {
      Preferences.setReportingAssetType(context, dataAsset.type)
      Preferences.setReportingAssetCode(context, dataAsset.code)
      Preferences.setReportingAssetIssuer(context, dataAsset.issuer)
    }

    fun getDataAssetFromPrefs(context: Context): DataAsset {
      val type = Preferences.getReportingAssetType(context)
      val code = Preferences.getReportingAssetCode(context)
      val issuer = Preferences.getReportingAssetIssuer(context)

      return if (type == "native") {
        DataAsset(type, NATIVE_ASSET_CODE, "null")
      } else {
        DataAsset(type, code, issuer)
      }
    }

    fun getCode(asset: Asset): String? {
      var code: String? = null
      val assetType = asset.toXdr().discriminant
      if (assetType != null) {
        code = when (assetType) {
          AssetType.ASSET_TYPE_CREDIT_ALPHANUM12 -> {
            val concreteAsset = asset as AssetTypeCreditAlphaNum12
            concreteAsset.code
          }
          AssetType.ASSET_TYPE_CREDIT_ALPHANUM4 -> {
            val concreteAsset = asset as AssetTypeCreditAlphaNum4
            concreteAsset.code
          }
          AssetType.ASSET_TYPE_NATIVE -> {
            NATIVE_ASSET_CODE
          }
        }
      }
      return code
    }

    fun getLogo(assetCode: String): Int = when (assetCode) {
      Constants.LUMENS_ASSET_TYPE, Constants.LUMENS_ASSET_CODE -> Constants.LUMENS_IMAGE_RES
      Constants.DMC_ASSET_CODE -> Constants.DMC_IMAGE_RES
      Constants.ETH_ASSET_CODE -> Constants.ETH_IMAGE_RES
      Constants.BTC_ASSET_CODE -> Constants.BTC_IMAGE_RES
      Constants.ZAR_ASSET_CODE -> Constants.ZAR_IMAGE_RES
      Constants.NGNT_ASSET_CODE -> Constants.NGNT_IMAGE_RES
      else -> 0
    }

    fun getShortCode(assetCode: String): String = when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> "L"
      Constants.BTC_ASSET_CODE -> "B"
      Constants.ETH_ASSET_CODE -> "L"
      Constants.ZAR_ASSET_CODE -> "R"
      Constants.NGNT_ASSET_CODE -> "N"
      else -> ""
    }

    fun getWithdrawAccount(assetCode: String?): String = when (assetCode) {
      Constants.ZAR_ASSET_CODE -> Constants.ZAR_ASSET_ISSUER
      Constants.NGNT_ASSET_CODE -> Constants.NGNT_ASSET_WITHDRAW
      else -> ""
    }

    fun getMaxDecimals(assetCode: String): Int = when {
      assetCode.equals(Constants.ZAR_ASSET_CODE, true) -> 2
      assetCode.equals(Constants.NGNT_ASSET_CODE, true) -> 2
      else -> 7
    }
  }
}

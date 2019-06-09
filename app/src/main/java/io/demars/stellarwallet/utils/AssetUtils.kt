package io.demars.stellarwallet.utils

import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.SelectionModel
import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeCreditAlphaNum12
import org.stellar.sdk.AssetTypeCreditAlphaNum4
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.xdr.AssetType

class AssetUtils {
  companion object {
    val NATIVE_ASSET_CODE = "XLM"

    fun toAssetFrom(dataAsset: DataAsset): Asset {
      val buying: Asset = if (dataAsset.type == "native") {
        AssetTypeNative()
      } else {
        Asset.create(dataAsset.type, dataAsset.code, dataAsset.issuer)
      }
      return buying
    }

    fun toDataAssetFrom(selection: SelectionModel): DataAsset? {
      val asset = selection.asset
      if (asset != null) {
        return toDataAssetFrom(asset)
      }
      return null
    }

    private fun toDataAssetFrom(asset: Asset): DataAsset? {
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
      Constants.LUMENS_ASSET_CODE -> Constants.LUMENS_IMAGE_RES
      Constants.ZAR_ASSET_TYPE -> Constants.ZAR_IMAGE_RES
      Constants.DMC_ASSET_TYPE -> Constants.DMC_IMAGE_RES
      Constants.RTGS_ASSET_TYPE -> Constants.RTGS_IMAGE_RES
      else -> 0
    }

    fun getMaxDecimals(assetCode: String): Int = when {
      assetCode.equals(Constants.ZAR_ASSET_TYPE, true) -> 2
      assetCode.equals(Constants.RTGS_ASSET_TYPE, true) -> 2
      else -> 7
    }

    fun isSessionAsset(assetCode: String) : Boolean = xlmAsNative(assetCode).equals(
      WalletApplication.userSession.getSessionAsset().assetCode, true)

    private fun xlmAsNative(assetCode: String) : String =
      if (assetCode == Constants.LUMENS_ASSET_CODE) Constants.LUMENS_ASSET_TYPE else assetCode
  }
}

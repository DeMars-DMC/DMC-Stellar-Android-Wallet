package io.demars.stellarwallet.models.ui

import org.stellar.sdk.Asset

/**
 * Class which provides a model for DmcAsset
 * @constructor Sets all properties of the DmcAsset
 * @property code the baseAssetCode code
 * @property image the link to icon of baseAssetCode
 * @property issuer the public address of the issuer of this baseAssetCode
 * @property amount the amount of balance of baseAssetCode
 * @property isAdded either added or not added baseAssetCode
 */

data class DmcAsset(var code: String, var image: Int, var issuer: String,
                    var name: String, var amount: String?,
                    var isAdded: Boolean = false, var asset: Asset?) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DmcAsset

    if (code != other.code) return false
    if (issuer != other.issuer) return false

    return true
  }

  override fun hashCode(): Int {
    var result = code.hashCode()
    result = 31 * result + issuer.hashCode()
    return result
  }
}


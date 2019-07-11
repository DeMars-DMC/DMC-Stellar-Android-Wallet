package io.demars.stellarwallet.models

import io.demars.stellarwallet.helpers.Constants

class DefaultAsset : SessionAsset {
  override fun getAssetCode(): String = Constants.LUMENS_ASSET_TYPE
  override fun getAssetName(): String = Constants.LUMENS_ASSET_NAME
  override fun getAssetIssuer(): String = ""
}

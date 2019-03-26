package io.demars.stellarwallet.models

class DefaultAsset : io.demars.stellarwallet.models.SessionAsset {
    val LUMENS_ASSET_TYPE = "native"
    val LUMENS_ASSET_NAME = "Stellar Lumens"

    override fun getAssetCode(): String {
      return LUMENS_ASSET_TYPE
    }

    override fun getAssetName(): String {
       return LUMENS_ASSET_NAME
    }

    override fun getAssetIssuer(): String {
       return ""
    }
}

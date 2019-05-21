package io.demars.stellarwallet.models

interface BalanceAvailability {
  fun getAccountId(): String
  fun getActiveAssetAvailability(): AssetAvailability
  fun getAssetAvailability(assetCode: String, issuer:String): AssetAvailability
  fun getNativeAssetAvailability(): NativeAssetAvailability
  fun getAllBalances(): ArrayList<AssetAvailability>
}
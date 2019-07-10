package io.demars.stellarwallet.interfaces

import org.stellar.sdk.Asset

interface AssetListener {
    fun changeTrustline(asset: Asset, isRemove: Boolean)
    fun assetSelected(assetCode: String)
    fun deposit(assetCode: String)
    fun withdraw(assetCode: String)
    fun addCustomAsset()
}
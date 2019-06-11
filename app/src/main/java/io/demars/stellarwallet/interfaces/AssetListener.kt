package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.SessionAsset
import org.stellar.sdk.Asset

interface AssetListener {
    fun changeTrustline(asset: Asset, isRemoveAsset: Boolean)
    fun assetSelected(sessionAsset: SessionAsset)
    fun deposit(assetCode: String)
    fun withdraw(assetCode: String)
}
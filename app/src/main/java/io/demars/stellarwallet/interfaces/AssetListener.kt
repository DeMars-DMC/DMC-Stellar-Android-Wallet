package io.demars.stellarwallet.interfaces

import android.view.View
import io.demars.stellarwallet.models.SessionAsset
import org.stellar.sdk.Asset

interface AssetListener {
    fun changeTrustline(asset: Asset, isRemoveAsset: Boolean)
    // TODO: CHANGE TO NORMAL ASSET OBJECT AND REMOVE SESSION ASSET AT ALL
    fun assetSelected(sessionAsset: SessionAsset, image: View, code: View, balance: View)
    fun deposit(assetCode: String)
    fun withdraw(assetCode: String)
}
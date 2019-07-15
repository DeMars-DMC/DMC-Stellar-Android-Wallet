package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.DataAsset
import org.stellar.sdk.Asset

interface OnAssetSelected {
  fun onAssetSelected(asset:DataAsset)
}
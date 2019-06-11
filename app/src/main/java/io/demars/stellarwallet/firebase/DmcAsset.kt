package io.demars.stellarwallet.firebase

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DmcAsset(val assetCode: String) {
}
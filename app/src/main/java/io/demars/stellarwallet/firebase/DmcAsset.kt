package io.demars.stellarwallet.firebase

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class DmcAsset(var assetCode: String = ""):Serializable {
  var banks = HashMap<String, String>()
}
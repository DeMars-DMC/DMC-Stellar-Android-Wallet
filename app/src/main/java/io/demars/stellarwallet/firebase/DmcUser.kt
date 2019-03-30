package io.demars.stellarwallet.firebase

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DmcUser(
  var phone: String = ""
) {
  var verified = false
  var stellar_addresses: ArrayList<String>? = null
}
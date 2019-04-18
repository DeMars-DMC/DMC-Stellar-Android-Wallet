package io.demars.stellarwallet.firebase

import com.google.firebase.database.IgnoreExtraProperties
import io.demars.stellarwallet.models.Address
import java.io.Serializable

@IgnoreExtraProperties
data class DmcUser(
  var uid: String = "",
  var phone: String = ""
) : Serializable {
  var first_name = ""
  var last_name = ""
  var birth_date = ""
  var nationality = ""
  var address = Address()
  var email_address = ""
  var stellar_address = ""
  var document_type = ""
  var document_number = ""
  var id_expiry_date = ""
  var id_photo_uploaded = false
  var id_selfie_uploaded = false
  var state = State.UNCOMPLETED.ordinal

  enum class State {
    UNCOMPLETED, VERIFYING, VERIFIED, REVERIFYING, BLOCKED, CLOSED
  }

  fun isRegistrationCompleted(): Boolean {
    return state > State.UNCOMPLETED.ordinal
  }

  fun isVerified(): Boolean {
    return state > State.VERIFYING.ordinal
  }
}
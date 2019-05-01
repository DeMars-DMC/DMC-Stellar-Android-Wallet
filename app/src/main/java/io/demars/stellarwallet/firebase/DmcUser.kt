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
  var id_back_uploaded = false
  var id_selfie_uploaded = false
  var created_at = 0L
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

  fun isReadyToRegister() :Boolean {
    return uid.isNotEmpty() && phone.isNotEmpty()
  }

  override fun toString(): String {
    return "DmcUser(uid='$uid', phone='$phone', first_name='$first_name', last_name='$last_name', birth_date='$birth_date', nationality='$nationality', address=$address, email_address='$email_address', stellar_address='$stellar_address', document_type='$document_type', document_number='$document_number', id_expiry_date='$id_expiry_date', id_photo_uploaded=$id_photo_uploaded, id_selfie_uploaded=$id_selfie_uploaded, state=$state)"
  }
}
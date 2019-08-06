package io.demars.stellarwallet.firebase

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import io.demars.stellarwallet.models.Address
import io.demars.stellarwallet.models.BankAccount
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
  var communication_type = ""
  var document_type = ""
  var document_number = ""
  var id_expiry_date = ""
  var id_photo_uploaded = false
  var id_back_uploaded = false
  var id_selfie_uploaded = false
  var id_photo_url = ""
  var id_back_url = ""
  var id_selfie_url = ""
  var created_at = 0L
  var state = State.DIGITAL.ordinal
  var notification_key = ""
  val banksZAR = ArrayList<BankAccount>()
  val banksNGNT = ArrayList<BankAccount>()

  enum class State {
    DIGITAL, VERIFYING, VERIFIED, DOCUMENTS_UNCLEAR, ID_EXPIRE_SHORTLY, ID_EXPIRED, BLOCKED, CLOSED
  }

  @Exclude
  fun isRegistrationCompleted(): Boolean {
    return state > State.DIGITAL.ordinal
  }

  @Exclude
  fun isVerified(): Boolean {
    return state == State.VERIFIED.ordinal
  }

  @Exclude
  fun isReadyToRegister(): Boolean {
    return uid.isNotEmpty() && phone.isNotEmpty()
  }
}
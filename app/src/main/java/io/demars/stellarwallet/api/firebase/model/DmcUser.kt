package io.demars.stellarwallet.api.firebase.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class DmcUser(
  var uid: String = "",
  var phone: String = ""
) : Serializable {
  //region Properties
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
  //endregion

  //region Helper methods
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
  //endregion

  //region User Address model
  class Address : Serializable {
    var first_line = ""
    var second_line = ""
    var town_city = ""
    var post_code = ""
    var country = ""

    @Exclude
    fun isValid(): Boolean = first_line.isNotEmpty() && town_city.isNotEmpty() &&
      post_code.isNotEmpty() && country.isNotEmpty()

    override fun toString(): String = "$first_line $second_line $town_city, $post_code $country"
  }
  //endregion

  //region User Bank Account model
  data class BankAccount(var name: String = "",
                         var branch: String = "",
                         var type: String = "",
                         var number: String = "",
                         var bankName: String = "") {

    @Exclude
    fun isValid(): Boolean = bankName.isNotEmpty() && branch.isNotEmpty() &&
      type.isNotEmpty() && number.isNotEmpty()
  }
  //endregion

  //region Enums
  enum class State {
    DIGITAL, VERIFYING, VERIFIED, DOCUMENTS_UNCLEAR, ID_EXPIRE_SHORTLY, ID_EXPIRED, BLOCKED, CLOSED
  }
  //endregion
}
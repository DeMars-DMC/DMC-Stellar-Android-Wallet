package io.demars.stellarwallet.firebase

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DmcUser(
  var uid: String = "",
  var phone: String = ""
) {
  var first_name = ""
  var last_name = ""
  var birth_date = ""
  var nationality = ""
  var address = ""
  var email_address = ""
  var stellar_address = ""
  var document_type = ""
  var document_number = ""
  var id_expiry_date = ""
  var id_photo_uploaded = false
  var id_selfie_uploaded = false
  var registrationCompleted = false
  var verified = false
}
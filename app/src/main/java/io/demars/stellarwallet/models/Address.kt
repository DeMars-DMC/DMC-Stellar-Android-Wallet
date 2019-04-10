package io.demars.stellarwallet.models

class Address {
  var firstLine = ""
  var secondLine = ""
  var townCity = ""
  var postCode = ""
  var country = ""

  fun isValid() : Boolean = firstLine.isNotEmpty() && townCity.isNotEmpty() &&
    postCode.isNotEmpty() && country.isNotEmpty()

  override fun toString(): String = "$firstLine $secondLine $townCity, $postCode $country"
}
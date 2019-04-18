package io.demars.stellarwallet.models

import java.io.Serializable

class Address : Serializable {
  var first_line = ""
  var second_line = ""
  var town_city = ""
  var post_code = ""
  var country = ""

  fun isValid(): Boolean = first_line.isNotEmpty() && town_city.isNotEmpty() &&
    post_code.isNotEmpty() && country.isNotEmpty()

  override fun toString(): String = "$first_line $second_line $town_city, $post_code $country"
}
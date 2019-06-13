package io.demars.stellarwallet.models

import com.google.firebase.database.Exclude

data class BankAccount(var name: String = "",
                       var branch: String = "",
                       var type: String = "",
                       var number: String = "",
                       var bankName: String = "") {

  @Exclude
  fun isValid(): Boolean = name.isNotEmpty() && branch.isNotEmpty() &&
    type.isNotEmpty() && number.isNotEmpty()
}
package io.demars.stellarwallet.models

import io.demars.stellarwallet.api.firebase.model.DmcUser

data class Withdrawal(val assetCode: String, val amount: String, val fee: String, val bankAccount: DmcUser.BankAccount)
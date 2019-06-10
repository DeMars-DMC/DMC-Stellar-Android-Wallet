package io.demars.stellarwallet.models

data class Withdrawal(val assetCode: String, val amount: String, val fee: String, val bankAccount: BankAccount)
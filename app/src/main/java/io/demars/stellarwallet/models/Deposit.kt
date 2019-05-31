package io.demars.stellarwallet.models

data class Deposit(val assetCode: String, val amount: String, val bankAccount: BankAccount)
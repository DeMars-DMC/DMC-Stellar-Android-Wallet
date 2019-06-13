package io.demars.stellarwallet.models

data class Deposit(val assetCode: String, val amount: String, val ref: String, val anchorBank: BankAccount, val userBank: BankAccount)
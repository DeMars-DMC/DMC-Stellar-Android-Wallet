package io.demars.stellarwallet.models

data class Deposit(val assetCode: String, val amount: String, val ref: String, val anchorBank: BankAccount, val userBank: BankAccount) {
  fun toReadableTitle() = "New $assetCode Deposit Request"
  fun toReadableMessage() = "Please deposit $amount $assetCode at any ${anchorBank.bankName}\n" +
    "${anchorBank.name}\n" +
    (if (anchorBank.branch.isEmpty()) "" else "Branch Code:  ${anchorBank.branch}\n") +
    "Account Number: ${anchorBank.number}\n" +
    (if (anchorBank.type.isEmpty()) "" else "Type: ${anchorBank.type}\n") +
    "Narration/Description/Remarks: $ref"
}
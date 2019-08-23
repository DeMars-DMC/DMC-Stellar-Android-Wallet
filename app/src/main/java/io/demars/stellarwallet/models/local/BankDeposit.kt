package io.demars.stellarwallet.models.local

import io.demars.stellarwallet.api.firebase.model.DmcUser

class BankDeposit(val assetCode: String, val amount: String, val ref: String, val anchorBank: DmcUser.BankAccount, val userBank: DmcUser.BankAccount) : Deposit(assetCode) {
  override fun toReadableMessage(): String = "Please deposit $amount $assetCode at any ${anchorBank.bankName}\n" +
    "${anchorBank.name}\n" +
    (if (anchorBank.branch.isEmpty()) "" else "Branch Code:  ${anchorBank.branch}\n") +
    "Account Number: ${anchorBank.number}\n" +
    (if (anchorBank.type.isEmpty()) "" else "Type: ${anchorBank.type}\n") +
    "Narration/Description/Remarks: $ref"

  override fun toString(): String {
    return "BankDeposit(baseAssetCode='$assetCode', amount='$amount', ref='$ref', anchorBank=$anchorBank, userBank=$userBank)"
  }
}
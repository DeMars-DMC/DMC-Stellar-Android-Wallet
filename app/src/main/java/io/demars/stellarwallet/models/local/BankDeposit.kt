package io.demars.stellarwallet.models.local

import io.demars.stellarwallet.api.firebase.model.DmcUser
import io.demars.stellarwallet.helpers.Constants

class BankDeposit(val assetCode: String, val amount: String, val ref: String, val anchorBank: DmcUser.BankAccount, val userBank: DmcUser.BankAccount) : Deposit(assetCode) {
  override fun toReadableMessage(): String = when (assetCode) {
    Constants.NGNT_ASSET_CODE ->
      "Please deposit $amount Niara at any Guaranty Trust Bank\n" +
        "Cowrie Integrated Systems Limited\n" +
        "Account Number: 0174408645\n" +
        "Narration/Description/Remarks: F34C2D5\n" +
        "\n" +
        "Your peer to peer digital Naira will be credited to your wallet instantly on receipt."
    else -> // ZAR
      "Please deposit $amount $assetCode at any ${anchorBank.bankName}\n" +
        "${anchorBank.name}\n" +
        (if (anchorBank.branch.isEmpty()) "" else "Branch Code:  ${anchorBank.branch}\n") +
        "Account Number: ${anchorBank.number}\n" +
        (if (anchorBank.type.isEmpty()) "" else "Type: ${anchorBank.type}\n") +
        "Narration/Description/Remarks: $ref"
  }

  override fun toString(): String {
    return "BankDeposit(baseAssetCode='$assetCode', amount='$amount', ref='$ref', anchorBank=$anchorBank, userBank=$userBank)"
  }
}
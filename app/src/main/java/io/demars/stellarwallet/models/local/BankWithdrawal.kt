package io.demars.stellarwallet.models.local

import io.demars.stellarwallet.api.firebase.model.DmcUser

class BankWithdrawal(val assetCode: String, val amount: String, val fee: String, val bankAccount: DmcUser.BankAccount) : Withdrawal(assetCode, amount, fee) {
  override fun toString(): String {
    return "BankWithdrawal(baseAssetCode='$assetCode', amount='$amount', fee='$fee', bankAccount=$bankAccount)"
  }
}
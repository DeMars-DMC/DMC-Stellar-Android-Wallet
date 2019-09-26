package io.demars.stellarwallet.models.local

class CryptoWithdrawal(val assetCode: String, val amount: String) : Withdrawal(assetCode) {
  override fun toString(): String {
    return "CryptoWithdrawal(baseAssetCode='$assetCode', amount='$amount')"
  }
}

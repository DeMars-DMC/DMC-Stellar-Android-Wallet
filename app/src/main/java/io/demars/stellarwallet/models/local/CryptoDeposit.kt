package io.demars.stellarwallet.models.local

class CryptoDeposit(val assetCode: String, val amount: String, val anchorAccount : String, val messageFromAnchor:String) : Deposit(assetCode) {
  override fun toReadableMessage(): String = messageFromAnchor
  override fun toString(): String {
    return "CryptoDeposit(baseAssetCode='$assetCode', amount='$amount', messageFromAnchor='$messageFromAnchor')"
  }

}
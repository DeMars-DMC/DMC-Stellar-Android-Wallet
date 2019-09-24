package io.demars.stellarwallet.models.local

class CryptoDeposit(val assetCode: String, val amount: String, val ref: String, val anchorAccount : String, private val messageFromAnchor:String) : Deposit(assetCode) {
  override fun toReadableMessage(): String = messageFromAnchor
  override fun getDepositAssetCode(): String = this.assetCode
  override fun getDepositAmount(): String = this.amount
  override fun getDepositRef(): String = this.ref
  override fun toString(): String =
     "CryptoDeposit(baseAssetCode='$assetCode', amount='$amount', messageFromAnchor='$messageFromAnchor')"


}
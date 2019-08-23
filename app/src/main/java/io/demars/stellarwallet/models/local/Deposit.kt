package io.demars.stellarwallet.models.local

abstract class Deposit(val baseAssetCode: String) {
  fun toReadableTitle() = "New $baseAssetCode Deposit Request"
  abstract fun toReadableMessage(): String
}

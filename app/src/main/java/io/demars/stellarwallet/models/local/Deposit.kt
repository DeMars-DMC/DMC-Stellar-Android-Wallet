package io.demars.stellarwallet.models.local

import java.io.Serializable

abstract class Deposit(val baseAssetCode: String) : Serializable {
  fun toReadableTitle() = "New $baseAssetCode Deposit Request"
  abstract fun toReadableMessage(): String
  abstract fun getDepositAmount(): String
  abstract fun getDepositAssetCode(): String
  abstract fun getDepositRef(): String
}

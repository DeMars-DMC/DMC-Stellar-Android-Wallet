package io.demars.stellarwallet.interfaces

interface TransactionsListener {
  fun onTransactionClicked(transaction: Any)
}
package io.demars.stellarwallet.models.stellar

import io.demars.stellarwallet.helpers.Constants

class TrustLine(private val numBalances: Int) {
    var count = numBalances - 1
    var amount = count * Constants.MINIMUM_BALANCE_INCREMENT
}
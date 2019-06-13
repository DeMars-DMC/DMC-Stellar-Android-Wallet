package io.demars.stellarwallet.models.stellar

import io.demars.stellarwallet.helpers.Constants

class Offer (var count: Int) {
    var amount = count * Constants.MINIMUM_BALANCE_INCREMENT
}
package io.demars.stellarwallet.api.horizon.model

import io.demars.stellarwallet.helpers.Constants

class Offer (var count: Int) {
    var amount = count * Constants.MINIMUM_BALANCE_INCREMENT
}
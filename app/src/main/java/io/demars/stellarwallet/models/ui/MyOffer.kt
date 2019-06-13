package io.demars.stellarwallet.models.ui

import io.demars.stellarwallet.models.Currency
import java.util.*

class MyOffer(var id: Int, var date: Date, var currencyFrom: Currency,
              var currencyTo: Currency, var amountFrom: Float, var amountTo: Float)
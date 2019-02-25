package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.SelectionModel

interface OnTradeCurrenciesChanged {
    fun onCurrencyChange(selling: SelectionModel, buying: SelectionModel)
}
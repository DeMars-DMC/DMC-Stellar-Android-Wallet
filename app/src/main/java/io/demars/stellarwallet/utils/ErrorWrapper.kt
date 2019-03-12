package io.demars.stellarwallet.utils

import android.content.Context
import io.demars.stellarwallet.R

class ErrorWrapper(context: Context) {
    var map: MutableMap<String, String> = mutableMapOf()

    init {
        map[context.getString(R.string.technical_tx_failed)] = context.getString(R.string.readable_tx_failed)
        map[context.getString(R.string.technical_tx_insufficient_balance)] = context.getString(R.string.readable_tx_insufficient_balance)
        map[context.getString(R.string.technical_op_underfunded)] = context.getString(R.string.readable_op_underfunded)
        map[context.getString(R.string.technical_op_low_reserve)] = context.getString(R.string.readable_op_low_reserve)
        map[context.getString(R.string.technical_connection_problem)] = context.getString(R.string.readable_connection_problem)
        map[context.getString(R.string.technical_tx_timeout)] = context.getString(R.string.readable_tx_timeout)
    }
}
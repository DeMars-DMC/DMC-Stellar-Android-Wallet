package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.api.horizon.model.HorizonException

interface SuccessErrorCallback {
    fun onSuccess()
    fun onError(error: HorizonException)
}
package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.HorizonException

interface SuccessErrorCallback {
    fun onSuccess()
    fun onError(error: HorizonException)
}
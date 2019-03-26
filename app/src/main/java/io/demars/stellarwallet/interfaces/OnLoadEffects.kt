package io.demars.stellarwallet.interfaces

import org.stellar.sdk.responses.effects.EffectResponse
import java.util.ArrayList

interface OnLoadEffects {
    fun onLoadEffects(result: ArrayList<EffectResponse>?)
    fun onError(errorMessage:String)
}

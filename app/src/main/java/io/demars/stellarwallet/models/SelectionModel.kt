package io.demars.stellarwallet.models

import org.stellar.sdk.Asset

open class SelectionModel(var label: String, var value: Int, var holdings: Double, var asset : Asset?) {
    override fun toString(): String {
        return label
    }
}
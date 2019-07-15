package io.demars.stellarwallet.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DataAsset constructor(val type: String, val code: String, val issuer: String) : Parcelable

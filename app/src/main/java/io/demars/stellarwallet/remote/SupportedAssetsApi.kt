package io.demars.stellarwallet.remote

import io.demars.stellarwallet.models.SupportedAsset

import retrofit2.Call
import retrofit2.http.GET

interface SupportedAssetsApi {
    @get:GET("/directory/assets")
    val assets: Call<Map<String, SupportedAsset>>
}

package io.demars.stellarwallet.api.stellarport

import io.demars.stellarwallet.api.stellarport.model.*
import io.demars.stellarwallet.helpers.Constants
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface StellarPortApi {

  @GET("/authentication")
  fun getAuthToken(@Query("account") account: String): Call<GetAuthResponse>

  @POST("/authentication")
  fun postAuthToken(@Body transactionBody: PostAuthRequestBody): Call<PostAuthResponse>

  @GET("/v2/GBVOL67TMUQBGL4TZYNMY3ZQ5WGQYFPFD5VJRWXR72VA33VFNL225PL5/deposit")
  fun deposit(@Header("Authorization") authHeader: String, @Query("asset_code") assetCode: String, @Query("account") account: String): Call<DepositResponse>

  @GET("/v2/GBVOL67TMUQBGL4TZYNMY3ZQ5WGQYFPFD5VJRWXR72VA33VFNL225PL5/withdraw")
  fun withdraw(@Header("Authorization") authHeader: String, @Query("asset_code") assetCode: String, @Query("account") account: String, @Query("dest") dest: String): Call<WithdrawResponse>

  object Creator {
    private var okHttpClientAuth = OkHttpClient.Builder().build()

    private var retrofitAuth = Retrofit.Builder()
      .baseUrl(Constants.STELLARPORT_AUTH_URL)
      .client(okHttpClientAuth)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

    fun createAuth(): StellarPortApi = retrofitAuth.create(StellarPortApi::class.java)

    private var retrofitTransfer = Retrofit.Builder()
      .baseUrl(Constants.STELLARPORT_TRANSFER_URL)
      .client(okHttpClientAuth)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

    fun createTransfer(): StellarPortApi = retrofitTransfer.create(StellarPortApi::class.java)

  }
}
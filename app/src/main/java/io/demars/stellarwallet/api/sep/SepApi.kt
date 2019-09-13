package io.demars.stellarwallet.api.sep

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface SepApi {

  @GET("/authentication")
  fun getAuthToken(@Query("account") account: String): Call<GetAuthResponse>

  @POST("/authentication")
  fun postAuthToken(@Body transactionBody: PostAuthRequestBody): Call<PostAuthResponse>

  @GET("{depositPath}")
  fun deposit(@Header("Authorization") authHeader: String,
              @Path(value = "depositPath", encoded = true) depositPath: String,
              @Query("asset_code") assetCode: String,
              @Query("account") account: String,
              @Query("email") email : String,
              @Query("email_address") emailAddress : String): Call<Sep6DepositResponse>

  @GET("{withdrawPath}")
  fun withdraw(@Header("Authorization") authHeader: String,
               @Path(value = "withdrawPath", encoded = true) withdrawPath: String,
               @Query("asset_code") assetCode: String,
               @Query("type") type: String,
               @Query("account") account: String,
               @Query("dest") dest: String,
               @Query("dest_extra") destExtra: String = "",
               @Query("email") email : String,
               @Query("email_address") emailAddress : String): Call<Sep6WithdrawResponse>

  object Creator {
    private var okHttpClientAuth = OkHttpClient.Builder().build()

    private fun createNewRetrofit(baseUrl: String) = Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClientAuth)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

    fun create(baseUrl: String): SepApi = createNewRetrofit(baseUrl).create(SepApi::class.java)

  }
}
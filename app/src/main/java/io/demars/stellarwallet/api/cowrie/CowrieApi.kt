package io.demars.stellarwallet.api.cowrie

import io.demars.stellarwallet.api.cowrie.model.DepositResponse
import io.demars.stellarwallet.api.cowrie.model.WithdrawResponse
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CowrieApi {

  @GET("fiat/crypto?fiat=NGN&crypto=NGNT")
  fun ngnForNgnt(@Query("address") stellarAddress: String) : Call<DepositResponse>

  @GET("crypto/fiat?crypto=NGNT&fiat=NGN")
  fun ngntForNgn(@Query("bank_code") bankCode: String,
                 @Query("account_number") accountNumber: String) : Call<WithdrawResponse>

  object Creator {
    private var okHttpClient = OkHttpClient.Builder().build()

    private var retrofit = Retrofit.Builder()
      .baseUrl("https://api.cowrie.exchange/")
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()

    fun create(): CowrieApi = retrofit.create(CowrieApi::class.java)
  }
}
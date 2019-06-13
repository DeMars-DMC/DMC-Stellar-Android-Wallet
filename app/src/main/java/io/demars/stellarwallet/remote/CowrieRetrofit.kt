package io.demars.stellarwallet.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CowrieRetrofit {
  private var okHttpClient = OkHttpClient.Builder().build()

  private var retrofit = Retrofit.Builder()
    .baseUrl("https://api.cowrie.exchange/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  fun create(): CowrieApi = retrofit.create(CowrieApi::class.java)
}
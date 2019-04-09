package io.demars.stellarwallet.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DmcRetrofit {

    private var okHttpClient = OkHttpClient.Builder().build()

    private var retrofit = Retrofit.Builder()
            .baseUrl("https://api.blockeq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun <T> create(service: Class<T>): T {
        return retrofit.create(service)
    }

}

package io.demars.stellarwallet.remote

import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DmcRetrofit {

    private var okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(StethoInterceptor())
            .build()

    private var retrofit = Retrofit.Builder()
            .baseUrl("https://api.blockeq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun <T> create(service: Class<T>): T {
        return retrofit.create(service)
    }

}

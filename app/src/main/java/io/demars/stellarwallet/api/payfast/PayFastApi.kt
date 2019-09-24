package io.demars.stellarwallet.api.payfast

import io.demars.stellarwallet.helpers.Constants
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface PayFastApi {

  @FormUrlEncoded
  @POST("eng/process")
  fun process(@FieldMap params: Map<String, String>): Call<ResponseBody>

  companion object {
    private var client = OkHttpClient.Builder()
      .followRedirects(false)
      .followSslRedirects(false)
      .build()

    private fun createNewRetrofit() = Retrofit.Builder()
      .baseUrl(Constants.PAYFAST_BASE_URL)
      .client(client)
      .build()

    fun create(): PayFastApi = createNewRetrofit().create(PayFastApi::class.java)
  }
}
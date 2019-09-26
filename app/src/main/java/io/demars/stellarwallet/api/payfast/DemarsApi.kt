package io.demars.stellarwallet.api.payfast

import io.demars.stellarwallet.helpers.Constants
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface DemarsApi {

  @GET("/deposit")
  fun deposit(@Query("name_first") firstName: String,
              @Query("name_last") lastName: String,
              @Query("email_address") email: String,
              @Query("deposit_ref") depositRef: String,
              @Query("amount") amount: String): Call<ResponseBody>

  companion object {
    private var client = OkHttpClient.Builder()
      .followRedirects(false)
      .followSslRedirects(false)
      .build()

    private fun createNewRetrofit() = Retrofit.Builder()
      .baseUrl(Constants.DEMARS_PAYFAST_URL)
      .client(client)
      .build()

    fun create(): DemarsApi = createNewRetrofit().create(DemarsApi::class.java)
  }
}
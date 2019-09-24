package io.demars.stellarwallet.api.firebase

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import retrofit2.http.*


interface FirebaseApi {
  @Headers(
    "Content-Type:application/json",
    "Authorization:key=$serverKey",
    "project_id:$senderId"
  )
  @GET("notification")
  fun getGroup(@Query("notification_key_name") keyName: String): Call<HashMap<String, String>>

  @Headers(
    "Content-Type:application/json",
    "Authorization:key=$serverKey",
    "project_id:$senderId"
  )
  @POST("notification")
  fun manageGroup(@Body body: HashMap<String, Any>): Call<HashMap<String, String>>

  companion object {
    private const val serverKey = "AAAAFHZShHA:APA91bF0qxRi55-Xvd6dT6Hr8K4CSQHKPSvUCeHXqzI8FYkNFt3ck8PdMERCniJqniWlioKKWs8peEKAOjBW2h2uBfTwoO3cPUumbajwgPpRYqfzoMcDziQNd8cV9XXWPTe3VrAnzAVZ"
    private const val senderId = "87884465264"
    private const val baseUrl = "https://fcm.googleapis.com/fcm/"
    private val okHttpClient = OkHttpClient.Builder().build()

    val api: FirebaseApi by lazy {
      Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .baseUrl(baseUrl)
        .build().create(FirebaseApi::class.java)
    }
  }
}
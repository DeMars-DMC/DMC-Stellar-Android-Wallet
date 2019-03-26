package io.demars.stellarwallet.remote

import io.demars.stellarwallet.models.Diagnostic
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface DiagnosticApi {
    @POST("diagnostic")
    fun uploadDiagnostic(@Body diagnostic: Diagnostic) : Call<JsonObject>
}

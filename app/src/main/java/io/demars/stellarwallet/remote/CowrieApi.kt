package io.demars.stellarwallet.remote

import io.demars.stellarwallet.models.cowrie.DepositResponse
import io.demars.stellarwallet.models.cowrie.WithdrawalResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CowrieApi {

  @GET("fiat/crypto?fiat=NGN&crypto=NGNT")
  fun ngnForNgnt(@Query("address") stellarAddress: String) : Call<DepositResponse>

  @GET("crypto/fiat?crypto=NGNT&fiat=NGN")
  fun ngntForNgn(@Query("bank_code") bankCode: String,
                 @Query("account_number") accountNumber: String) : Call<WithdrawalResponse>

}
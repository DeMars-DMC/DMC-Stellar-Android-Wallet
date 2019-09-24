package io.demars.stellarwallet.api.sep

import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.BaseActivity
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.AccountUtils
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Transaction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object Sep10 {
  fun authenticate(activity: BaseActivity, authUrl: String, listener: AuthListener) {
    val account = DmcApp.wallet.getStellarAccountId()
    if (account == null) {
      activity.toast("Error - Stellar account can't be null")
      return
    }

    val seed = AccountUtils.getSecretSeed(activity)
    val apiAuth = SepApi.create(authUrl)
    apiAuth.getAuthToken(account).enqueue(object : Callback<GetAuthResponse> {
      override fun onResponse(call: Call<GetAuthResponse>, response: Response<GetAuthResponse>) {
        val body = response.body()
        if (body != null && body.transaction.isNotEmpty()) {
          // Here we getting "challenge" transaction from Auth endpoint
          // we will sign it if it's safe and finish authentication
          val transaction = Transaction.fromEnvelopeXdr(body.transaction)

          if (transaction.sourceAccount.accountId == Constants.STELLARPORT_SIGNING_KEY) {
            // If SIGNING_KEY matching Source Account we can sign transaction and proceed
            transaction.sign(KeyPair.fromSecretSeed(seed))

            val signedTransaction = transaction.toEnvelopeXdrBase64()
            apiAuth.postAuthToken(PostAuthRequestBody(signedTransaction)).enqueue(object : Callback<PostAuthResponse> {
              override fun onResponse(call: Call<PostAuthResponse>, response: Response<PostAuthResponse>) {
                val postBody = response.body()
                if (postBody != null && postBody.token.isNotEmpty()) {
                  // Ok here we got JWT token that we can use for Authorization on Transfer Server
                  listener.onTokenResponse(account, postBody.token)
                } else {
                  SepErrorHandler.handleError(activity, response.errorBody())
                }
              }

              override fun onFailure(call: Call<PostAuthResponse>, t: Throwable) {
                activity.toast("Error - PostAuthToken request failed")
              }
            })
          } else {
            activity.toast("Error - Source account doesn't match SIGNING_KEY. Transaction isn't safe")
          }
        } else {
          SepErrorHandler.handleError(activity, response.errorBody())
        }
      }

      override fun onFailure(call: Call<GetAuthResponse>, t: Throwable) {
        activity.toast("Error - GetAuthToken request failed")
      }
    })
  }

  interface AuthListener : SepListener {
    fun onTokenResponse(account: String, token: String)
  }
}
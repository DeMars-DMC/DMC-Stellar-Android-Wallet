package io.demars.stellarwallet.api.stellarport

import com.google.gson.Gson
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.BaseActivity
import io.demars.stellarwallet.api.stellarport.model.*
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.ViewUtils
import okhttp3.ResponseBody
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Transaction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object StellarPort {
  fun authenticate(activity: BaseActivity, listener: AuthListener) {
    val account = DmcApp.wallet.getStellarAccountId()
    if (account == null) {
      activity.toast("Error - Stellar account can't be null")
      return
    }

    val seed = AccountUtils.getSecretSeed(activity)
    val apiAuth = StellarPortApi.Creator.createAuth()
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
                  handleError(activity, response.errorBody())
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
          handleError(activity, response.errorBody())
        }
      }

      override fun onFailure(call: Call<GetAuthResponse>, t: Throwable) {
        activity.toast("Error - GetAuthToken request failed")
      }
    })
  }

  fun authenticatedWithdraw(activity: BaseActivity, assetCode: String, dest: String, listener: WithdrawListener) {
    authenticate(activity, object : AuthListener {
      override fun onTokenResponse(account: String, token: String) {
        withdraw(activity, token, assetCode, account, dest, listener)
      }
    })
  }

  fun authenticatedDeposit(activity: BaseActivity, assetCode: String, listener: DepositListener) {
    authenticate(activity, object : AuthListener {
      override fun onTokenResponse(account: String, token: String) {
        deposit(activity, token, assetCode, account, listener)
      }
    })
  }

  private fun deposit(activity: BaseActivity, token: String, assetCode: String, account: String, listener: DepositListener) {
    val apiTransfer = StellarPortApi.Creator.createTransfer()
    val tokenHeader = "Bearer $token"
    apiTransfer.deposit(tokenHeader, assetCode, account).enqueue(object : Callback<DepositResponse> {

      override fun onResponse(call: Call<DepositResponse>, response: Response<DepositResponse>) {
        val body = response.body()
        val errorBody = response.errorBody()
        when {
          response.isSuccessful && body != null -> listener.onDepositResponse(body)
          else -> handleError(activity, errorBody)
        }
      }

      override fun onFailure(call: Call<DepositResponse>, t: Throwable) {
        activity.toast("Error - $assetCode BankDeposit request failed")
      }

    })
  }

  private fun withdraw(activity: BaseActivity, token: String, assetCode: String, account: String, dest: String, listener: WithdrawListener) {
    val apiTransfer = StellarPortApi.Creator.createTransfer()
    val tokenHeader = "Bearer $token"

    apiTransfer.withdraw(tokenHeader, assetCode, account, dest).enqueue(object : Callback<WithdrawResponse> {
      override fun onResponse(call: Call<WithdrawResponse>, response: Response<WithdrawResponse>) {
        val body = response.body()
        val errorBody = response.errorBody()

        when {
          response.isSuccessful && body != null -> listener.onWithdrawResponse(body)
          else -> handleError(activity, errorBody)
        }
      }

      override fun onFailure(call: Call<WithdrawResponse>, t: Throwable) {
        activity.toast("Error - $assetCode Withdraw request failed")
      }
    })
  }

  private fun handleError(activity: BaseActivity, errorBody: ResponseBody?) {
    if (errorBody == null) {
      activity.toast("UNKNOWN ERROR")
      return
    }

    when (val parsedError = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)) {
      null -> activity.toast("PARSING ERROR FAILED")
      else -> {
        when {
          //Need to complete Customer info form on StellarPort so open in a WebView
          parsedError.type == "interactive_customer_info_needed" && parsedError.url.isNotEmpty() ->
            ViewUtils.openUrl(activity, parsedError.url)
          // Other Errors
          else -> activity.toast(parsedError.error)
        }
      }
    }
  }

  interface Listener
  interface AuthListener : Listener {
    fun onTokenResponse(account: String, token: String)
  }

  interface DepositListener : Listener {
    fun onDepositResponse(response: DepositResponse)
  }

  interface WithdrawListener : Listener {
    fun onWithdrawResponse(response: WithdrawResponse)
  }
}
package io.demars.stellarwallet.api.sep

import android.app.Activity
import android.app.AlertDialog
import io.demars.stellarwallet.R
import io.demars.stellarwallet.activities.BaseActivity
import io.demars.stellarwallet.utils.ViewUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object Sep6 {

  const val TYPE_CRYPTO = "crypto"
  const val TYPE_BANK = "bank_account"

  fun authenticatedDeposit(activity: BaseActivity,
                           authUrl: String,
                           transferUrl: String,
                           depositPath: String,
                           assetCode: String,
                           email: String,
                           listener: DepositListener) {
    Sep10.authenticate(activity, authUrl, object : Sep10.AuthListener {
      override fun onTokenResponse(account: String, token: String) {
        // Ok we successfully authenticated so we can request deposit on TRANSFER SERVER
        deposit(activity, transferUrl, depositPath, token, assetCode, account, email, listener)
      }
    })
  }

  fun authenticatedWithdraw(activity: BaseActivity,
                            authUrl: String,
                            transferUrl: String,
                            withdrawPath: String,
                            assetCode: String,
                            type: String,
                            dest: String,
                            destExtra: String,
                            email: String,
                            listener: WithdrawListener) {
    Sep10.authenticate(activity, authUrl, object : Sep10.AuthListener {
      override fun onTokenResponse(account: String, token: String) {
        // Ok we successfully authenticated so we can request withdrawal on TRANSFER SERVER
        withdraw(activity, transferUrl, withdrawPath, token, assetCode,
          type, account, dest, destExtra, email, listener)
      }
    })
  }

  fun deposit(activity: BaseActivity,
              transferUrl: String,
              depositPath: String,
              token: String?,
              assetCode: String,
              account: String,
              email: String,
              listener: DepositListener) {
    val apiTransfer = SepApi.Creator.create(transferUrl)
    val tokenHeader = "Bearer $token"
    apiTransfer.deposit(tokenHeader, depositPath, assetCode, account, email, email).enqueue(object : Callback<Sep6DepositResponse> {

      override fun onResponse(call: Call<Sep6DepositResponse>, response: Response<Sep6DepositResponse>) {
        val body = response.body()
        val errorBody = response.errorBody()
        when {
          response.isSuccessful && body != null -> listener.onDepositResponse(body)
          else -> SepErrorHandler.handleError(activity, errorBody)
        }
      }

      override fun onFailure(call: Call<Sep6DepositResponse>, t: Throwable) {
        activity.toast("Error - $assetCode BankDeposit request failed")
      }

    })
  }

  fun withdraw(activity: BaseActivity,
               transferUrl: String,
               withdrawPath: String,
               token: String?,
               assetCode: String,
               type: String,
               account: String,
               dest: String,
               destExtra: String,
               email: String,
               listener: WithdrawListener) {
    val api = SepApi.Creator.create(transferUrl)
    val tokenHeader = "Bearer $token"

    api.withdraw(tokenHeader, withdrawPath, assetCode, type, account, dest, destExtra, email, email).enqueue(object : Callback<Sep6WithdrawResponse> {
      override fun onResponse(call: Call<Sep6WithdrawResponse>, response: Response<Sep6WithdrawResponse>) {
        val body = response.body()
        val errorBody = response.errorBody()

        when {
          response.isSuccessful && body != null -> listener.onWithdrawResponse(body)
          else -> SepErrorHandler.handleError(activity, errorBody)
        }
      }

      override fun onFailure(call: Call<Sep6WithdrawResponse>, t: Throwable) {
        activity.toast("Error - $assetCode Withdraw request failed")
      }
    })
  }


  fun showInteractiveDialogConfirmation(activity: Activity, url: String) {
    AlertDialog.Builder(activity)
      .setTitle(R.string.interactive_customer_info_title)
      .setMessage(R.string.interactive_customer_info_message)
      .setNegativeButton(R.string.cancel) { _, _ -> }
      .setPositiveButton(R.string.proceed) { _, _ ->
        ViewUtils.openUrl(activity, url)
      }.show()
  }

  interface DepositListener : SepListener {
    fun onDepositResponse(response: Sep6DepositResponse)
  }

  interface WithdrawListener : SepListener {
    fun onWithdrawResponse(response: Sep6WithdrawResponse)
  }
}
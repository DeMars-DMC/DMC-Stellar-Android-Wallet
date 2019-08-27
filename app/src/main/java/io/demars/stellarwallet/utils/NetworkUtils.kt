package io.demars.stellarwallet.utils

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import io.demars.stellarwallet.R

class NetworkUtils(private val context: Context) {

    // Moving this to Horizon.kt
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun displayNoNetwork() {
        ViewUtils.showToast(context, context.getString(R.string.no_network))
    }
}
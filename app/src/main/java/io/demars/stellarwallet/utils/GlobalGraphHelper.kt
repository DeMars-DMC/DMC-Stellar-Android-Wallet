package io.demars.stellarwallet.utils

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.LaunchActivity
import io.demars.stellarwallet.activities.ManageAssetsActivity
import io.demars.stellarwallet.encryption.KeyStoreWrapper
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.mvvm.local.OperationsRepository
import io.demars.stellarwallet.mvvm.local.TradesRepository
import io.demars.stellarwallet.mvvm.local.TransactionsRepository

class GlobalGraphHelper {
  companion object {
    fun wipeAndRestart(activity: FragmentActivity) {
      wipe(activity.applicationContext)
      restart(activity)
    }

    fun isExistingWallet(): Boolean {
      return !DmcApp.wallet.getEncryptedPhrase().isNullOrEmpty()
        && !DmcApp.wallet.getStellarAccountId().isNullOrEmpty()
    }

    fun launchWallet(activity: FragmentActivity) {
      val intent = Intent(activity, ManageAssetsActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      activity.startActivity(intent)
    }

    fun wipe(context: Context): Boolean {
      clearSession()
      Firebase.signOut()
      AccountRepository.clear()
      OperationsRepository.getInstance().clear()
      TransactionsRepository.getInstance().clear()
      TradesRepository.getInstance().clear()
      KeyStoreWrapper(context).clear()
      return DmcApp.wallet.clearLocalStore()
    }

    fun restart(activity: FragmentActivity) {
      DmcApp.showPin = true
      val intent = Intent(activity, LaunchActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      activity.startActivity(intent)
      activity.finish()
    }

    fun clearSession() {
      DmcApp.userSession.setPin(null)
    }
  }
}

package io.demars.stellarwallet.firebase

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.ManageAssetsActivity

class MessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage?) {
    super.onMessageReceived(remoteMessage)
    remoteMessage?.data?.get("state")?.toIntOrNull()?.let { state ->
      val message = when (state) {
        DmcUser.State.DIGITAL.ordinal -> ""
        DmcUser.State.VERIFYING.ordinal -> "Your account will be verified soon"
        DmcUser.State.VERIFIED.ordinal -> "Your account has successfully been verified"
        DmcUser.State.DOCUMENTS_UNCLEAR.ordinal -> "Your account application is still pending"
        DmcUser.State.ID_EXPIRE_SHORTLY.ordinal -> "Your ID is expiring shortly. Please update it"
        DmcUser.State.ID_EXPIRED.ordinal -> "Your ID is expired. Please update it"
        DmcUser.State.BLOCKED.ordinal -> "Your account is blocked"
        DmcUser.State.CLOSED.ordinal -> "Your account is closed"
        else -> ""
      }

      val title = when (state) {
        DmcUser.State.VERIFYING.ordinal,
        DmcUser.State.VERIFIED.ordinal -> "Thank you"
        DmcUser.State.DOCUMENTS_UNCLEAR.ordinal -> "Documents verification"
        DmcUser.State.ID_EXPIRE_SHORTLY.ordinal,
        DmcUser.State.ID_EXPIRED.ordinal -> "Document update needed"
        DmcUser.State.BLOCKED.ordinal -> "We're sorry"
        DmcUser.State.CLOSED.ordinal -> "Your account is closed"
        else -> ""
      }

      if (message.isEmpty()) return

      val intent = Intent(this, ManageAssetsActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

      val notification = NotificationCompat.Builder(this, DmcApp.CHANNEL_ID_ACC)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_stat_ic_main_logo)
        .setColor(ContextCompat.getColor(this, R.color.colorAccent))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
      val manager = NotificationManagerCompat.from(applicationContext)
      manager.notify(123, notification)

    }
  }
}

package io.demars.stellarwallet.api.firebase

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.WalletActivity
import io.demars.stellarwallet.api.firebase.model.DmcUser

class MessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    remoteMessage.data["state"]?.toIntOrNull()?.let { state ->
      val message = when (state) {
        DmcUser.State.DIGITAL.ordinal -> ""
        DmcUser.State.VERIFYING.ordinal -> "Your account will be verified soon"
        DmcUser.State.VERIFIED.ordinal -> "Your account has successfully been verified"
        DmcUser.State.DOCUMENTS_UNCLEAR.ordinal -> "We regret to inform you that your account has not passed verification. Please contact backoffice@demars.io for further info."
        DmcUser.State.ID_EXPIRE_SHORTLY.ordinal -> "Your ID document will be expiring soon. Kindly email a clear image of an updated document to backoffice@demars.io."
        DmcUser.State.ID_EXPIRED.ordinal -> "Your ID document is expired. Kindly email a clear image of an updated document to backoffice@demars.io."
        DmcUser.State.BLOCKED.ordinal -> " We regret to inform you that your DMC App account has been blocked, pending further investigation. Contact backoffice@demars.io for further info."
        DmcUser.State.CLOSED.ordinal -> "We can confirm that your account has been closed. You may now uninstall your wallet App."
        else -> ""
      }

      val title = when (state) {
        DmcUser.State.VERIFYING.ordinal,
        DmcUser.State.VERIFIED.ordinal -> "Thank you"
        DmcUser.State.DOCUMENTS_UNCLEAR.ordinal -> "Account verification"
        DmcUser.State.ID_EXPIRE_SHORTLY.ordinal,
        DmcUser.State.ID_EXPIRED.ordinal -> "KYC Documents update needed"
        DmcUser.State.BLOCKED.ordinal -> "Your account is blocked"
        DmcUser.State.CLOSED.ordinal -> "Your account is closed"
        else -> ""
      }

      if (message.isEmpty()) return

      val intent = Intent(this, WalletActivity::class.java)
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

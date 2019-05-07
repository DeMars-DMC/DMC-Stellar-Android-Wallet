package io.demars.stellarwallet.firebase

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication

class MessagingService : FirebaseMessagingService()  {
  override fun onMessageReceived(remoteMessage: RemoteMessage?) {
    super.onMessageReceived(remoteMessage)
    val notification = NotificationCompat.Builder(this, WalletApplication.CHANNEL_ID_ACC)
      .setContentTitle(remoteMessage?.notification?.title?: "")
      .setContentText(remoteMessage?.notification?.body?:"")
      .setSmallIcon(R.drawable.ic_stat_ic_main_logo)
      .setColor(ContextCompat.getColor(this, R.color.colorAccent))
      .build()
    val manager = NotificationManagerCompat.from(applicationContext)
    manager.notify(123, notification)
  }
}

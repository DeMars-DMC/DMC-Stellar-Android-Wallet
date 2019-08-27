package io.demars.stellarwallet.helpers

import io.demars.stellarwallet.api.firebase.model.DmcUser
import io.demars.stellarwallet.models.local.*
import org.jetbrains.anko.doAsync
import timber.log.Timber
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object MailHelper {
  private const val EMAIL_FROM = "andriod@demars.io"
  private const val EMAIL_OFFICE = "backoffice@demars.io"
  private fun sendMail(email: String, subject: String, content: String) {
    // config
    // Sender's email ID needs to be mentioned
    val from = EMAIL_FROM

    // Get system properties
    val properties = System.getProperties()
    // Setup mail server
    properties.setProperty("mail.smtps.host", "securesgp44.sgcpanel.com")
    properties.setProperty("mail.smtps.port", "465")
    properties.setProperty("mail.smtps.auth", "true")
    properties.setProperty("mail.user", EMAIL_FROM)
    properties.setProperty("mail.password", "vrsA67%\$\$drdty")

    // Get the default Session object.
    val session = Session.getDefaultInstance(properties)

    try {
      val message = MimeMessage(session)

      // Set From: header field of the header.
      message.setFrom(InternetAddress(from))

      // Set To: header field of the header.
      message.addRecipient(Message.RecipientType.TO,
        InternetAddress(email))

      message.subject = subject
      message.setText(content)

      val transport = session.getTransport("smtps")
      transport.connect(null, properties.getProperty("mail.password"))
      message.saveChanges()
      transport.sendMessage(message, message.allRecipients)
      transport.close()

      Timber.d("Successfully sent message to - $email, subject - $subject, content - $content")
    } catch (mex: MessagingException) {
      mex.printStackTrace()
    }
  }

  private fun sendMailAsync(email: String, subject: String, content: String) {
    doAsync {
      sendMail(email, subject, content)
    }
  }

  // Sends needed emails when new user is created
  fun notifyAboutNewUser(user: DmcUser) {
    // Notifying Back office
    sendMailAsync(EMAIL_OFFICE, "New Account created", "New account was just created - $user")
  }

  fun notifyAboutNewDeposit(user: DmcUser, deposit: Deposit) {
    // Notifying Back office
    sendMailAsync(EMAIL_OFFICE, deposit.toReadableTitle(), "New deposit(${deposit.baseAssetCode}) was just requested\n\n$deposit\n\n$user")
    // Notifying User with deposit payment information
    sendMailAsync(user.email_address, deposit.toReadableTitle(), deposit.toReadableMessage())
}

fun notifyAboutNewWithdrawal(user: DmcUser, withdrawal: Withdrawal) {
  // Notifying Back office
  sendMailAsync(EMAIL_OFFICE, "New ${withdrawal.baseAssetCode} BankWithdrawal Request", "New withdrawal(${withdrawal.baseAssetCode}) was just requested\n\n$withdrawal\n\n$user")
}
}
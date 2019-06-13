package io.demars.stellarwallet.helpers

import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.models.Deposit
import io.demars.stellarwallet.models.Withdrawal
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
    sendMailAsync(EMAIL_OFFICE, "New ${deposit.assetCode} Deposit Request", "New deposit(${deposit.assetCode}) was just requested\n\n$deposit\n\n$user")
    // Notifying User with deposit payment information
    val anchorInfo = deposit.anchorBank
    sendMailAsync(user.email_address, "New ${deposit.assetCode} Deposit",
      "Please deposit ${deposit.amount} ${deposit.assetCode} at any ${anchorInfo.bankName}\n" +
        "${anchorInfo.name}\n" +
        (if (anchorInfo.branch.isEmpty()) "" else "Branch Code:  ${anchorInfo.branch}\n") +
        "Account Number: ${anchorInfo.number}\n" +
        (if (anchorInfo.type.isEmpty()) "" else "Type: ${anchorInfo.type}\n") +
        "Narration/Description/Remarks: ${deposit.ref}")
  }

  fun notifyAboutNewWithdrawal(user: DmcUser, withdrawal: Withdrawal) {
    // Notifying Back office
    sendMailAsync(EMAIL_OFFICE, "New ${withdrawal.assetCode} Withdrawal Request", "New withdrawal(${withdrawal.assetCode}) was just requested\n\n$withdrawal\n\n$user")
   }
}
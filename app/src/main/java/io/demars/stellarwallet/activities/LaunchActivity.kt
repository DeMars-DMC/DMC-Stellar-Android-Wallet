package io.demars.stellarwallet.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import io.demars.stellarwallet.R
import io.demars.stellarwallet.models.MnemonicType
import io.demars.stellarwallet.utils.GlobalGraphHelper
import kotlinx.android.synthetic.main.activity_launch.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import android.os.CountDownTimer
import android.telephony.TelephonyManager
import com.google.firebase.FirebaseTooManyRequestsException
import io.demars.stellarwallet.views.pin.PinLockView


class LaunchActivity : BaseActivity(), PinLockView.DialerListener {
  companion object {
    private const val REQUEST_CREATE_USER = 111
    private const val SMS_TIMEOUT = 60L
  }

  private enum class Mode {
    INITIAL, CODE, STELLAR
  }

  private var mode = Mode.INITIAL
  private var token: PhoneAuthProvider.ForceResendingToken? = null
  private var verificationId: String? = null
  private var phone: String = ""
  private var smsCode: String = ""
  private val smsTimer = object : CountDownTimer(SMS_TIMEOUT * 1000, 1000) {
    override fun onTick(millisUntilFinished: Long) {
      val secondsLeft = (millisUntilFinished / 1000).toInt()
      if (secondsLeft != 0) {
        loginButton.text = formatTimer(secondsLeft)
      }
    }

    override fun onFinish() {
      loginButton.isEnabled = true
      loginButton.text = formatTimer(0)
      loginButton.setOnClickListener {
        verifyPhoneNumber()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch)
    FirebaseAuth.getInstance().useAppLanguage()
    FirebaseAuth.getInstance().signOut()
    updateForMode(mode)
  }

  private fun updateForMode(mode: Mode) {
    this.mode = mode
    when (mode) {
      Mode.INITIAL -> updateViewForInitial()
      Mode.CODE -> updateViewForCode()
      Mode.STELLAR -> updateViewForStellar()
    }
  }

  private fun updateViewForInitial() {
    phone = "+${getPhoneCode()}"

    createWalletButton.visibility = View.GONE
    recoverWalletButton.visibility = View.GONE

    verificationLabel.visibility = View.VISIBLE
    verificationLabel.text = getString(R.string.enter_your_phone_number)

    verificationText.visibility = View.VISIBLE
    verificationText.text = phone

    loginMessage.visibility = View.VISIBLE
    loginButton.visibility = View.VISIBLE
    loginButton.isEnabled = true
    loginButton.setText(R.string.log_in)
    loginButton.setOnClickListener {
      verifyPhoneNumber()
    }

    dialerView.visibility = View.VISIBLE
    dialerView.mDialerListener = this
  }

  private fun updateViewForCode() {
    smsCode = ""

    createWalletButton.visibility = View.GONE
    recoverWalletButton.visibility = View.GONE

    verificationText.visibility = View.VISIBLE
    verificationText.text = formatSmsCode()

    verificationLabel.visibility = View.VISIBLE
    verificationLabel.text = getString(R.string.enter_code_from_sms)

    loginMessage.visibility = View.GONE
    loginButton.visibility = View.VISIBLE
    loginButton.isEnabled = false
    loginButton.text = formatTimer(SMS_TIMEOUT.toInt())

    dialerView.visibility = View.VISIBLE
    dialerView.mDialerListener = this

    smsTimer.start()
  }

  private fun updateViewForStellar() {
    if (GlobalGraphHelper.isExistingWallet()) {
      verificationText.visibility = View.GONE
      loginButton.visibility = View.GONE
      GlobalGraphHelper.launchWallet(this)
    } else {
      verificationText.visibility = View.GONE
      loginButton.visibility = View.GONE
      dialerView.visibility = View.GONE

      createWalletButton.visibility = View.VISIBLE
      createWalletButton.isEnabled = true
      createWalletButton.setText(R.string.recover_wallet)
      createWalletButton.setOnClickListener {
        showCreateDialog()
      }

      recoverWalletButton.visibility = View.VISIBLE
      recoverWalletButton.isEnabled = true
      recoverWalletButton.setText(R.string.recover_wallet)
      recoverWalletButton.setOnClickListener {
        showRecoverDialog()
      }
    }
  }

  private fun showCreateDialog() {
    val builder = AlertDialog.Builder(this@LaunchActivity)
    val walletLengthList = listOf(getString(R.string.create_word_option_1), getString(R.string.create_word_option_2)).toTypedArray()
    builder.setTitle(getString(R.string.create_wallet))
      .setItems(walletLengthList) { _, which ->
        // The 'which' argument contains the index position
        // of the selected item

        val walletLength = if (which == 0) {
          MnemonicType.WORD_12
        } else {
          MnemonicType.WORD_24
        }

        startActivity(MnemonicActivity.newCreateMnemonicIntent(this, walletLength))
      }.show()
  }

  private fun showRecoverDialog() {
    val builder = AlertDialog.Builder(this@LaunchActivity)
    val walletLengthList = listOf(getString(R.string.recover_from_phrase), getString(R.string.recover_from_seed)).toTypedArray()
    builder.setTitle(getString(R.string.recover_wallet))
      .setItems(walletLengthList) { _, which ->
        // The 'which' argument contains the index position
        // of the selected item

        val isPhraseRecovery = (which == 0)

        startActivity(RecoverWalletActivity.newInstance(this, isPhraseRecovery))
      }.show()
  }

  private fun verifyPhoneNumber() {
    if (phone.trim().isEmpty() || phone.length < 6) {
      onError("Please enter valid phone number")
      return
    }

    PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, SMS_TIMEOUT,
      TimeUnit.SECONDS, this, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onCodeSent(verificationId: String?, forceResendingToken: PhoneAuthProvider.ForceResendingToken?) {
        super.onCodeSent(verificationId, forceResendingToken)

        this@LaunchActivity.token = forceResendingToken
        this@LaunchActivity.verificationId = verificationId

        updateForMode(Mode.CODE)
      }

      override fun onVerificationCompleted(authCredential: PhoneAuthCredential?) {
        Timber.d("onVerificationCompleted:$authCredential")
        signInWithPhoneAuthCredential(authCredential!!)
      }

      override fun onVerificationFailed(ex: FirebaseException?) {
        Timber.w("onVerificationFailed:${ex.toString()}")
        when (ex) {
          is FirebaseAuthInvalidCredentialsException -> onError("Invalid phone number")
          is FirebaseTooManyRequestsException -> onError("Too many requests")
          else -> onError(ex?.localizedMessage)
        }

      }
    })
  }

  private fun verifyCode() {
    val credential = PhoneAuthProvider.getCredential(verificationId!!, smsCode)
    signInWithPhoneAuthCredential(credential)
  }

  private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
    hideUI()
    FirebaseAuth.getInstance().signInWithCredential(credential)
      .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
          task.result?.user?.uid?.let { uid ->
            startActivityForResult(CreateUserActivity.newInstance(this, uid, phone),
              REQUEST_CREATE_USER)
          } ?: onError("Something went wrong. Please try again")
        } else {
          if (task.exception is FirebaseAuthInvalidCredentialsException) {
            onError("Wrong code from SMS")
          } else {
            onError(task.exception?.localizedMessage)
          }
        }
      }
  }

  private fun onError(message: String?, justToast: Boolean = false) {
    Toast.makeText(this@LaunchActivity, message, Toast.LENGTH_LONG).show()
    if (!justToast) {
      clearPhoneAuthSession()
      updateForMode(Mode.INITIAL)
    }
  }

  private fun clearPhoneAuthSession() {
    token = null
    verificationId = null
    phone = ""
    smsCode = ""
    smsTimer.cancel()
  }

  private fun formatTimer(seconds: Int): String {
    return getString(R.string.pattern_send_new_sms,
      when (seconds) {
        0 -> ""
        in 1..9 -> "0:0$seconds"
        else -> "0:$seconds"
      })
  }

  override fun onBackPressed() {
    if (mode == Mode.CODE) {
      clearPhoneAuthSession()
      updateForMode(Mode.INITIAL)
    } else {
      super.onBackPressed()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    clearPhoneAuthSession()
  }


  private fun hideUI() {
    verificationText.visibility = View.GONE
    verificationLabel.visibility = View.GONE
    loginMessage.visibility = View.GONE
    loginButton.visibility = View.GONE
    dialerView.visibility = View.GONE
    createWalletButton.visibility = View.GONE
    recoverWalletButton.visibility = View.GONE
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CREATE_USER && resultCode == Activity.RESULT_OK) {
      updateForMode(Mode.STELLAR)
    } else {
      updateForMode(Mode.INITIAL)
    }
  }

  private fun getPhoneCode(): String {
    val countryCode = (getSystemService(Context.TELEPHONY_SERVICE)
      as TelephonyManager?)?.networkCountryIso ?: ""
    val countryCodes = resources.getStringArray(R.array.country_code)
    for (codePair in countryCodes) {
      val codeArray = codePair.split(",")
      if (codeArray[1].trim().equals(countryCode, true)) {
        return codeArray[0]
      }
    }
    return ""
  }

  override fun onDial(number: Int) {
    when (mode) {
      Mode.INITIAL -> {
        if (phone.length == 18) return
        phone = "$phone$number"
        verificationText.text = phone
      }
      Mode.CODE -> {
        if (smsCode.length == 6) return
        smsCode = "$smsCode$number"
        verificationText.text = formatSmsCode()
        if (smsCode.length == 6) verifyCode()
      }
      else -> {
      }
    }
  }

  override fun onDelete() {
    when (mode) {
      Mode.INITIAL -> {
        if (phone.length == 1) return
        phone = phone.substring(0, phone.length - 1)
        verificationText.text = phone
      }
      Mode.CODE -> {
        if (smsCode.isEmpty()) return
        smsCode = smsCode.substring(0, smsCode.length - 1)
        verificationText.text = formatSmsCode()
      }
      else -> {
      }
    }
  }

  override fun onDeleteAll() {
    when (mode) {
      Mode.INITIAL -> {
        phone = "+"
        verificationText.text = phone
      }
      Mode.CODE -> {
        smsCode = ""
        verificationText.text = formatSmsCode()
      }
      else -> {
      }
    }
  }

  private fun formatSmsCode(): String {
    return when (smsCode.length) {
      0 -> "_ _ _ _ _ _"
      1 -> "${smsCode[0]} _ _ _ _ _"
      2 -> "${smsCode[0]} ${smsCode[1]} _ _ _ _"
      3 -> "${smsCode[0]} ${smsCode[1]} ${smsCode[2]} _ _ _"
      4 -> "${smsCode[0]} ${smsCode[1]} ${smsCode[2]} ${smsCode[3]} _ _"
      5 -> "${smsCode[0]} ${smsCode[1]} ${smsCode[2]} ${smsCode[3]} ${smsCode[4]} _"
      else -> "${smsCode[0]} ${smsCode[1]} ${smsCode[2]} ${smsCode[3]} ${smsCode[4]} ${smsCode[5]}"
    }
  }
}

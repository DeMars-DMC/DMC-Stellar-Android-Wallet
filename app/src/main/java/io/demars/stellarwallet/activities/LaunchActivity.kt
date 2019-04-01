package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
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
import java.util.regex.Pattern
import android.os.CountDownTimer
import com.google.firebase.FirebaseTooManyRequestsException
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.firebase.Firebase


class LaunchActivity : BaseActivity() {

  companion object {
    private const val REQUEST_CODE_CAMERA = 111
    private const val SMS_TIMEOUT = 60L
  }

  private enum class Mode {
    PHONE_NUMBER, CODE, STELLAR
  }

  private var mode = Mode.PHONE_NUMBER
  private var token: PhoneAuthProvider.ForceResendingToken? = null
  private var verificationId: String? = null
  private var phone: String = ""
  private val smsTimer = object : CountDownTimer(SMS_TIMEOUT * 1000, 1000) {
    override fun onTick(millisUntilFinished: Long) {
      val secondsLeft = (millisUntilFinished / 1000).toInt()
      if (secondsLeft != 0) {
        recoverWalletButton.text = formatTimer(secondsLeft)
      }
    }

    override fun onFinish() {
      recoverWalletButton.isEnabled = true
      recoverWalletButton.text = formatTimer(0)
      recoverWalletButton.setOnClickListener {
        verifyPhoneNumber()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launch)
    FirebaseAuth.getInstance().useAppLanguage()
    updateForMode(mode)
  }

  private fun updateForMode(mode: Mode) {
    this.mode = mode
    when (mode) {
      Mode.PHONE_NUMBER -> updateViewForPhoneNumber()
      Mode.CODE -> updateViewForCode()
      Mode.STELLAR -> updateViewForStellar()
    }
  }

  private fun updateViewForPhoneNumber() {
    if (FirebaseAuth.getInstance().currentUser == null) {
      verificationEditText.visibility = View.VISIBLE
      verificationEditText.filters = arrayOf(InputFilter.LengthFilter(20))
      verificationEditText.text.clear()
      verificationEditText.setHint(R.string.phone_number_hint)

      createWalletButton.visibility = View.VISIBLE
      createWalletButton.setText(R.string.verify_phone_number)
      createWalletButton.setOnClickListener {
        validatePhoneNumber()
        verifyPhoneNumber()
      }

      recoverWalletButton.visibility = View.VISIBLE
      recoverWalletButton.isEnabled = true
      recoverWalletButton.setText(R.string.continue_as_guest)
      recoverWalletButton.setOnClickListener {
        showGuestDialog()
      }
    } else {
      updateForMode(Mode.STELLAR)
    }
  }

  private fun updateViewForCode() {
    verificationEditText.visibility = View.VISIBLE
    verificationEditText.filters = arrayOf(InputFilter.LengthFilter(6))
    verificationEditText.text.clear()
    verificationEditText.setHint(R.string.code_from_sms)

    createWalletButton.visibility = View.VISIBLE
    createWalletButton.setText(R.string.verify)
    createWalletButton.setOnClickListener {
      verifyCode()
    }

    recoverWalletButton.visibility = View.VISIBLE
    recoverWalletButton.isEnabled = false
    recoverWalletButton.text = formatTimer(SMS_TIMEOUT.toInt())

    smsTimer.start()
  }

  private fun updateViewForStellar() {
    if (GlobalGraphHelper.isExistingWallet()) {
      verificationEditText.visibility = View.GONE
      createWalletButton.visibility = View.GONE
      recoverWalletButton.visibility = View.GONE
      GlobalGraphHelper.launchWallet(this)
    } else {
      verificationEditText.visibility = View.GONE

      createWalletButton.visibility = View.VISIBLE
      createWalletButton.setText(R.string.create_new_wallet)
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

  private fun showGuestDialog() {
    val builder = AlertDialog.Builder(this@LaunchActivity)
    builder.setTitle(getString(R.string.continue_as_guest) + "?")
      .setMessage(R.string.continue_as_guest_explain)
      .setPositiveButton(R.string.ok) { _, _ ->
        updateForMode(Mode.STELLAR)
      }
      .setNegativeButton(R.string.verify) { dialog, _ ->
        dialog.dismiss()
      }.show()
  }

  private fun verifyPhoneNumber() {
    if (phone.trim().isEmpty()) return

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
    val smsCode = verificationEditText.text.toString()
    if (validateSmsCode(smsCode)) {
      val credential = PhoneAuthProvider.getCredential(verificationId!!, smsCode)
      signInWithPhoneAuthCredential(credential)
    } else {
      onError("Doesn't look like SMS code", true)
    }
  }

  private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
    hideUI()
    FirebaseAuth.getInstance().signInWithCredential(credential)
      .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
          task.result?.user?.uid?.let { uid ->
            val user = DmcUser(phone)
            Firebase.getDatabaseReference().child("users").child(uid).setValue(user)
              .addOnSuccessListener {
                Timber.d("Added a new user to Firebase Database uid - $uid, phone - $phone")
                openCameraActivity()
              }.addOnFailureListener {
                Timber.w("Failed to add a new user to Firebase Database uid - $uid, phone - $phone, error - ${it.message}")
                onError("Failed to create a new user. Please try again", true)

                showUI()
              }
          } ?: showUI()
        } else {
          showUI()
          if (task.exception is FirebaseAuthInvalidCredentialsException) {
            onError("Wrong code from SMS", true)
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
      updateForMode(Mode.PHONE_NUMBER)
    }
  }

  private fun clearPhoneAuthSession() {
    token = null
    verificationId = null
    phone = ""
    smsTimer.cancel()
  }

  private fun validatePhoneNumber() {
    phone = verificationEditText.text.toString()
    if (phone.isBlank()) {
      onError("Phone number should not be empty", true)
    } else if (!phone.startsWith("+")) {
      verificationEditText.text.insert(0, "+")
      phone = verificationEditText.text.toString()
    }
  }

  private fun validateSmsCode(code: String): Boolean {
    val p = Pattern.compile("(\\d{6})")
    val m = p.matcher(code)
    return m.find()
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
      updateForMode(Mode.PHONE_NUMBER)
    } else {
      super.onBackPressed()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    clearPhoneAuthSession()
  }

  private fun openCameraActivity() {
    val useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    startActivityForResult(if (useCamera2) Camera2Activity.newInstance(this)
    else CameraActivity.newInstance(this), REQUEST_CODE_CAMERA)
  }

  private fun hideUI() {
    verificationEditText.visibility = View.GONE
    createWalletButton.visibility = View.GONE
    recoverWalletButton.visibility = View.GONE
  }

  private fun showUI() {
    verificationEditText.visibility = View.GONE
    createWalletButton.visibility = View.GONE
    recoverWalletButton.visibility = View.GONE
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      updateForMode(Mode.STELLAR)
    }
  }
}

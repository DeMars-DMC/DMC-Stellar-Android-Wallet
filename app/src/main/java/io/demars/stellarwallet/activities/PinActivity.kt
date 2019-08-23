package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import io.demars.stellarwallet.R
import io.demars.stellarwallet.utils.GlobalGraphHelper
import kotlinx.android.synthetic.main.activity_pin.*
import timber.log.Timber
import androidx.appcompat.app.AppCompatActivity
import io.demars.stellarwallet.interfaces.OnPinLockCompleteListener
import android.os.Build
import android.annotation.TargetApi
import android.content.pm.PackageManager
import androidx.biometric.BiometricPrompt
import io.demars.stellarwallet.utils.BiometricUtils

class PinActivity : AppCompatActivity() {

  private var numAttempts = 0
  private val MAX_ATTEMPTS = 3
  private var PIN: String? = null
  private lateinit var biometricPrompt: BiometricPrompt

  companion object {
    private const val INTENT_ARG_MESSAGE: String = "INTENT_ARG_MESSAGE"
    private const val INTENT_ARG_PIN: String = "INTENT_ARG_PIN"
    private const val INTENT_ARG_BIOMETRIC: String = "INTENT_ARG_BIOMETRIC"

    /**
     * New Instance of Intent to launch a {@link PinActivity}
     * @param context the activityContext of the requestor
     * @param pin pin to verified otherwise it will simple return the inserted pin, it must contain 4 number characters.
     * @param message messageFromAnchor to show on the top of the pinlock.
     */
    fun newInstance(context: Context, pin: String?, message: String = context.getString(R.string.please_enter_your_pin),
                    askBiometrics: Boolean): Intent {
      val intent = Intent(context, PinActivity::class.java)
      intent.putExtra(INTENT_ARG_MESSAGE, message)
      intent.putExtra(INTENT_ARG_BIOMETRIC, askBiometrics)
      if (pin != null) {
        if (pin.length == 4) {
          intent.putExtra(INTENT_ARG_PIN, pin)
        } else {
          throw IllegalStateException("pin ahs to contain 4 characters, found = '${pin.length}")
        }
      }
      return intent
    }

    fun getPinFromIntent(intent: Intent?): String? {
      if (intent == null) return null
      return intent.getStringExtra(INTENT_ARG_PIN)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_pin)

    pinLockView.setPinLockListener(object : OnPinLockCompleteListener() {
      override fun onComplete(pin: String) {
        Timber.d("OnComplete")
        when (PIN) {
          null -> setPin(pin)
          pin -> finishResultOK(pin)
          else -> processIncorrectPin()
        }
      }
    })

    pinLockView.attachIndicatorDots(indicatorDots)

    if (!intent.hasExtra(INTENT_ARG_MESSAGE)) throw IllegalStateException("missing argument {$INTENT_ARG_MESSAGE}, did you use #newInstance(..)?")

    val message = intent.getStringExtra(INTENT_ARG_MESSAGE)
    customMessageTextView.text = message

    PIN = intent.getStringExtra(INTENT_ARG_PIN)

    val askBiometrics = intent.getBooleanExtra(INTENT_ARG_BIOMETRIC, false)
    if (askBiometrics && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      displayBiometricPrompt()
    }
  }

  //region User Interface
  private fun setPin(pin: String) {
    PIN = pin

    pinLockView.resetPinLockView()
    customMessageTextView.text = getString(R.string.please_reenter_your_pin)
  }

  private fun processIncorrectPin() {
    showWrongPinDots(true)
    val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
    shakeAnimation.setAnimationListener(object : Animation.AnimationListener {
      override fun onAnimationStart(arg0: Animation) {}
      override fun onAnimationRepeat(arg0: Animation) {}
      override fun onAnimationEnd(arg0: Animation) {
        showWrongPinDots(false)
        pinLockView.resetPinLockView()
        numAttempts++
        customMessageTextView.text = resources.getQuantityString(R.plurals.attempts_template,
          MAX_ATTEMPTS - numAttempts, MAX_ATTEMPTS - numAttempts)
        if (numAttempts == MAX_ATTEMPTS) {
          GlobalGraphHelper.wipeAndRestart(this@PinActivity)
        }
      }
    })
    wrongPinDots.startAnimation(shakeAnimation)
  }

  private fun showWrongPinDots(show: Boolean) {
    indicatorDots.visibility = if (show) View.GONE else View.VISIBLE
    wrongPinDots.visibility = if (show) View.VISIBLE else View.GONE
  }

  override fun onBackPressed() {
    setResult(RESULT_CANCELED)
    super.onBackPressed()
    overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
  }

  private fun finishResultOK(pin: String?) {
    val intent = Intent()
    intent.putExtra(INTENT_ARG_PIN, pin)
    setResult(Activity.RESULT_OK, intent)
    overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
    finish()
  }
  //endregion

  //region Biometrics
  @TargetApi(Build.VERSION_CODES.P)
  private fun initBiometricPrompt() {
    if (!::biometricPrompt.isInitialized) {
      biometricPrompt = BiometricPrompt(this, mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            biometricPrompt.cancelAuthentication()
          }

          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            finishResultOK(PIN)
          }
        })
    }
  }

  @TargetApi(Build.VERSION_CODES.P)
  private fun displayBiometricPrompt() {
    if (!BiometricUtils.isSdkVersionSupported) return
    if (!BiometricUtils.isBiometricPromptEnabled) return
    if (!BiometricUtils.isHardwareSupported(this)) return
    if (!BiometricUtils.isPermissionGranted(this)) return
    if (!BiometricUtils.isFingerprintAvailable(this)) return

    initBiometricPrompt()
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Biometric scanner")
      .setNegativeButtonText("Enter PIN")
      .build()

    biometricPrompt.authenticate(promptInfo)
  }
  //endregion
}
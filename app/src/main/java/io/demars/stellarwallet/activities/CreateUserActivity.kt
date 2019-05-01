package io.demars.stellarwallet.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View.GONE
import io.demars.stellarwallet.R
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.interfaces.AfterTextChanged
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_create_user.*
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.demars.stellarwallet.enums.CameraMode
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.helpers.MailHelper
import io.demars.stellarwallet.models.Address
import io.demars.stellarwallet.views.DmcURLSpan
import kotlinx.android.synthetic.main.activity_create_user.toolbar
import org.jetbrains.anko.textColor
import java.util.*


class CreateUserActivity : BaseActivity() {
  lateinit var user: DmcUser
  private var isCreating = true
  private var birthDateDialog: DatePickerDialog? = null
  private var expiryDateDialog: DatePickerDialog? = null
  private var address = Address()
  private var infoCheckedOnce = false

  companion object {
    private const val REQUEST_CODE_CAMERA_ID_FRONT = 111
    private const val REQUEST_CODE_CAMERA_ID_BACK = 222
    private const val REQUEST_CODE_CAMERA_SELFIE = 333
    private const val ARG_USER = "ARG_USER"

    fun newInstance(context: Context, user: DmcUser): Intent {
      val intent = Intent(context, CreateUserActivity::class.java)
      intent.putExtra(ARG_USER, user)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ViewUtils.setTransparentStatusBar(this)
    setContentView(R.layout.activity_create_user)

    this.user = intent.getSerializableExtra(ARG_USER) as DmcUser
    this.isCreating = !user.isRegistrationCompleted()

    if (isCreating) {
      showEditableView()
    } else {
      showNonEditableView()
    }
  }

  private fun showExpiryDateDialog() {
    if (expiryDateDialog == null) {
      val today = Calendar.getInstance()
      expiryDateDialog = DatePickerDialog(this,
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
          val expiryDate = "$year-${month + 1}-$dayOfMonth"
          expiryDatePicker.setTextColor(Color.BLACK)
          expiryDatePicker.text = expiryDate
          user.id_expiry_date = expiryDate
        }, today[Calendar.YEAR], today[Calendar.MONTH], today[Calendar.DAY_OF_MONTH])
      expiryDateDialog?.datePicker?.minDate = today.timeInMillis
      expiryDateDialog?.show()
    } else {
      expiryDateDialog?.show()
    }
  }

  private fun showBirthDateDialog() {
    if (birthDateDialog == null) {
      val eighteenYearsBack = Calendar.getInstance()
      eighteenYearsBack.add(Calendar.YEAR, -18)
      birthDateDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        val birthDate = "$year-${month + 1}-$dayOfMonth"
        dateOfBirthPicker.setTextColor(Color.BLACK)
        dateOfBirthPicker.text = birthDate
        user.birth_date = birthDate
      }, eighteenYearsBack[Calendar.YEAR],
        eighteenYearsBack[Calendar.MONTH],
        eighteenYearsBack[Calendar.DAY_OF_MONTH])
      birthDateDialog?.datePicker?.maxDate = eighteenYearsBack.timeInMillis
      birthDateDialog?.show()
    } else {
      birthDateDialog?.show()
    }
  }

  private fun hideWelcomeDialog() {
    val padding = resources.getDimension(R.dimen.padding_vertical)
    dialogBg.animate().alpha(0F)
    welcomeDialog.animate().translationY(-padding).setDuration(100).withEndAction {
      welcomeDialog.animate().translationY(padding * 100).setDuration(500).withEndAction {
        welcomeDialog.visibility = GONE
        dialogBg.visibility = GONE
      }
    }
  }

  private fun openCameraActivity(requestCode: Int) {
    val cameraMode = when (requestCode) {
      REQUEST_CODE_CAMERA_ID_FRONT -> CameraMode.ID_FRONT
      REQUEST_CODE_CAMERA_ID_BACK -> CameraMode.ID_BACK
      else -> CameraMode.ID_SELFIE
    }

    val useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    startActivityForResult(if (useCamera2) Camera2Activity.newInstance(this, cameraMode)
    else CameraActivity.newInstance(this, cameraMode), requestCode)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      when (requestCode) {
        REQUEST_CODE_CAMERA_ID_FRONT -> {
          user.id_photo_uploaded = true
          idPhotoCheck.setImageResource(R.drawable.ic_check_circle_accent_24dp)
        }
        REQUEST_CODE_CAMERA_ID_BACK -> {
          user.id_back_uploaded = true
          idBackCheck.setImageResource(R.drawable.ic_check_circle_accent_24dp)
        }
        REQUEST_CODE_CAMERA_SELFIE -> {
          user.id_selfie_uploaded = true
          idSelfieCheck.setImageResource(R.drawable.ic_check_circle_accent_24dp)
        }
      }
    }
  }

  private fun verifyAndCreateNewUser() {
    var infoChecked = true
    if (user.first_name.isNotBlank()) {
      addVerificationSpan(firstNameLabel, true)
    } else {
      addVerificationSpan(firstNameLabel, false)
      infoChecked = false
    }

    if (user.last_name.isNotBlank()) {
      addVerificationSpan(surnameLabel, true)
    } else {
      addVerificationSpan(surnameLabel, false)
      infoChecked = false
    }

    if (user.birth_date.isNotBlank()) {
      addVerificationSpan(birthDateLabel, true)
    } else {
      addVerificationSpan(birthDateLabel, false)
      infoChecked = false
    }

    if (user.nationality.isNotBlank()) {
      addVerificationSpan(nationalityLabel, true)
    } else {
      addVerificationSpan(nationalityLabel, false)
      infoChecked = false
    }

    if (user.address.isValid()) {
      addVerificationSpan(addressLabel, true)
    } else {
      addVerificationSpan(addressLabel, false)
      infoChecked = false
    }

    if (user.email_address.isNotBlank()) {
      addVerificationSpan(emailLabel, true)
    } else {
      addVerificationSpan(emailLabel, false)
      infoChecked = false
    }

    if (user.document_type.isNotBlank()) {
      addVerificationSpan(documentTypeLabel, true)
    } else {
      addVerificationSpan(documentTypeLabel, false)
      infoChecked = false
    }

    if (user.document_number.isNotBlank()) {
      addVerificationSpan(documentNumberLabel, true)
    } else {
      addVerificationSpan(documentNumberLabel, false)
      infoChecked = false
    }

    if (user.id_expiry_date.isNotBlank()) {
      addVerificationSpan(expiryDateLabel, true)
    } else {
      addVerificationSpan(expiryDateLabel, false)
      infoChecked = false
    }

    if (user.id_photo_uploaded && user.id_selfie_uploaded) {
      addVerificationSpan(personalIdLabel, true)
    } else {
      addVerificationSpan(personalIdLabel, false)
      infoChecked = false
    }

    infoCheckedOnce = true

    if (infoChecked) {
      user.state = DmcUser.State.VERIFYING.ordinal
      user.created_at = System.currentTimeMillis()
      Firebase.getDatabaseReference().child("users")
        .child(Firebase.getCurrentUserUid()!!).setValue(user).addOnSuccessListener {
          MailHelper.notifyUserCreated(user)
          setResultAndFinish()
        }.addOnFailureListener {
          Toast.makeText(this, "Something went wrong. Please try again", Toast.LENGTH_LONG).show()
        }
    } else {
      Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_LONG).show()
      scrollView.smoothScrollTo(0, 0)
    }
  }

  private fun setResultAndFinish() {
    val intent = Intent()
    intent.putExtra("user", user)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  private fun addVerificationSpan(textView: TextView, verified: Boolean) {
    val spannable = SpannableStringBuilder(if (infoCheckedOnce)
      textView.text.substring(0, textView.text.length - 2) else textView.text)
    val color = ContextCompat.getColor(this,
      if (verified) R.color.colorGreen else R.color.colorApricot)
    val textToInsert = if (verified) " ✓" else " ✗"
    val spanIndex = spannable.length

    spannable.insert(spanIndex, textToInsert)
    spannable.setSpan(ForegroundColorSpan(color), spanIndex, spanIndex + 2,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    textView.setText(spannable, TextView.BufferType.SPANNABLE)
  }

  private fun showEditableView() {
    toolbar.setTitle(R.string.create_dmc_account)

    dialogButton.setOnClickListener {
      hideWelcomeDialog()
      ViewUtils.showKeyboard(this, firstNameInput)
    }

    firstNameText.visibility = GONE
    surnameText.visibility = GONE
    addressText.visibility = GONE
    emailText.visibility = GONE
    documentNumberText.visibility = GONE

    firstNameInput.visibility = VISIBLE
    surnameInput.visibility = VISIBLE
    addressFirstLineInput.visibility = VISIBLE
    addressSecondLineInput.visibility = VISIBLE
    townCityInput.visibility = VISIBLE
    postcodeInput.visibility = VISIBLE
    emailInput.visibility = VISIBLE
    documentNumberInput.visibility = VISIBLE
    submitButton.visibility = VISIBLE

    firstNameInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        user.first_name = editable.trim().toString()
      }
    })

    surnameInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        user.last_name = editable.trim().toString()
      }
    })

    dateOfBirthPicker.setOnClickListener {
      showBirthDateDialog()
    }

    nationalityPicker.setOnClickListener {
      val nationalities = resources.getStringArray(R.array.nationality)
      AlertDialog.Builder(this).setTitle(getString(R.string.select_nationality))
        .setItems(nationalities) { _, which ->
          val nationality = nationalities[which]
          nationalityPicker.setTextColor(Color.BLACK)
          nationalityPicker.text = nationality
          user.nationality = nationality
        }.show()
    }

    addressFirstLineInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.first_line = editable.trim().toString()
        if (address.isValid()) {
          user.address = address
        }
      }
    })

    addressSecondLineInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.second_line = editable.trim().toString()
        if (address.isValid()) {
          user.address = address
        }
      }
    })

    townCityInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.town_city = editable.trim().toString()
        if (address.isValid()) {
          user.address = address
        }
      }
    })

    postcodeInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.post_code = editable.trim().toString()
        if (address.isValid()) {
          user.address = address
        }
      }
    })

    countryPicker.setOnClickListener {
      val countries = resources.getStringArray(R.array.country)
      AlertDialog.Builder(this).setTitle(getString(R.string.select_country))
        .setItems(countries) { _, which ->
          val country = countries[which]
          countryPicker.setTextColor(Color.BLACK)
          countryPicker.text = country
          address.country = country
          if (address.isValid()) {
            user.address = address
          }
        }.show()
    }

    emailInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        user.email_address = editable.trim().toString()
      }
    })

    documentTypePicker.setOnClickListener {
      val documentTypes = resources.getStringArray(R.array.document_type)
      AlertDialog.Builder(this).setTitle(getString(R.string.select_document_type))
        .setItems(documentTypes) { _, which ->
          val documentType = documentTypes[which]
          documentTypePicker.setTextColor(Color.BLACK)
          documentTypePicker.text = documentType
          user.document_type = documentType
        }.show()

    }

    documentNumberInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        user.document_number = editable.trim().toString()
      }
    })

    expiryDatePicker.setOnClickListener {
      showExpiryDateDialog()
    }

    idPhotoContainer.setOnClickListener {
      openCameraActivity(REQUEST_CODE_CAMERA_ID_FRONT)
    }

    idBackContainer.setOnClickListener {
      openCameraActivity(REQUEST_CODE_CAMERA_ID_BACK)
    }

    idSelfieContainer.setOnClickListener {
      openCameraActivity(REQUEST_CODE_CAMERA_SELFIE)
    }

    submitButton.setOnClickListener {
      verifyAndCreateNewUser()
    }

    val spannable = SpannableStringBuilder(termsConditions.text)
    val termsConditionStr = "Terms and Conditions"
    val spanIndex = spannable.indexOf(termsConditionStr)
    spannable.setSpan(DmcURLSpan(Constants.URL_TERMS_AND_CONDITIONS),
      spanIndex, spanIndex + termsConditionStr.length,
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(StyleSpan(Typeface.BOLD),
      spanIndex, spanIndex + termsConditionStr.length,
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    termsConditions.text = spannable
    termsConditions.movementMethod = LinkMovementMethod.getInstance()
  }

  private fun showNonEditableView() {
    // Enable back button in toolbar
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { onBackPressed() }
    toolbar.setTitle(R.string.personal_information)

    firstNameInput.visibility = GONE
    surnameInput.visibility = GONE
    addressFirstLineInput.visibility = GONE
    addressSecondLineInput.visibility = GONE
    townCityInput.visibility = GONE
    postcodeInput.visibility = GONE
    countryPicker.visibility = GONE
    emailInput.visibility = GONE
    documentNumberInput.visibility = GONE
    submitButton.visibility = GONE
    termsConditions.visibility = GONE
    dialogBg.visibility = GONE
    welcomeDialog.visibility = GONE

    dateOfBirthPicker.setOnClickListener(null)
    nationalityPicker.setOnClickListener(null)
    countryPicker.setOnClickListener(null)
    documentTypePicker.setOnClickListener(null)
    expiryDatePicker.setOnClickListener(null)
    idPhotoContainer.setOnClickListener(null)
    idSelfieContainer.setOnClickListener(null)

    firstNameText.visibility = VISIBLE
    surnameText.visibility = VISIBLE
    addressText.visibility = VISIBLE
    emailText.visibility = VISIBLE
    documentNumberText.visibility = VISIBLE

    firstNameText.text = user.first_name
    surnameText.text = user.last_name
    dateOfBirthPicker.text = user.birth_date
    nationalityPicker.text = user.nationality
    addressText.text = user.address.toString()
    emailText.text = user.email_address
    documentTypePicker.text = user.document_type
    documentNumberText.text = user.document_number
    expiryDatePicker.text = user.id_expiry_date

    dateOfBirthPicker.textColor = Color.BLACK
    nationalityPicker.textColor = Color.BLACK
    countryPicker.textColor = Color.BLACK
    documentTypePicker.textColor = Color.BLACK
    expiryDatePicker.textColor = Color.BLACK

    idPhotoCheck.setImageResource(if (user.id_photo_uploaded)
      R.drawable.ic_check_circle_accent_24dp else R.drawable.ic_circle_outline_palesky_24dp)
    idSelfieCheck.setImageResource(if (user.id_selfie_uploaded)
      R.drawable.ic_check_circle_accent_24dp else R.drawable.ic_circle_outline_palesky_24dp)
  }
}
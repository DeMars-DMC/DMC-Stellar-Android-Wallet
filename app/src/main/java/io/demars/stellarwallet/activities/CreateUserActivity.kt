package io.demars.stellarwallet.activities

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
import android.os.Build
import io.demars.stellarwallet.models.Address
import java.util.*


class CreateUserActivity : BaseActivity() {
  private var birthDateDialog: DatePickerDialog? = null
  private var expiryDateDialog: DatePickerDialog? = null
  lateinit var user: DmcUser
  private var address = Address()

  companion object {
    private const val REQUEST_CODE_CAMERA = 111
    private const val ARG_UID = "ARG_UID"
    private const val ARG_PHONE = "ARG_PHONE"

    fun newInstance(context: Context, uid: String, phoneNumber: String): Intent {
      val intent = Intent(context, CreateUserActivity::class.java)
      intent.putExtra(ARG_UID, uid)
      intent.putExtra(ARG_PHONE, phoneNumber)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ViewUtils.setTransparentStatusBar(this)
    ViewUtils.setActivityKeyboardHidden(this)
    setContentView(R.layout.activity_create_user)

    user = DmcUser(intent.getStringExtra(ARG_UID), intent.getStringExtra(ARG_PHONE))

    initUI()
  }

  private fun initUI() {
    dialogButton.setOnClickListener {
      hideWelcomeDialog()
      ViewUtils.showKeyboard(this, firstNameInput)
    }

    personalIdContainer.setOnClickListener {
      openCameraActivity(REQUEST_CODE_CAMERA)
    }

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
        address.firstLine = editable.trim().toString()
        if (address.isValid()) {
          user.address = address.toString()
        }
      }
    })

    addressSecondLineInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.secondLine = editable.trim().toString()
        if (address.isValid()) {
          user.address = address.toString()
        }
      }
    })

    townCityInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.townCity = editable.trim().toString()
        if (address.isValid()) {
          user.address = address.toString()
        }
      }
    })

    postcodeInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        address.postCode = editable.trim().toString()
        if (address.isValid()) {
          user.address = address.toString()
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
            user.address = address.toString()
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
    val useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    startActivityForResult(if (useCamera2) Camera2Activity.newInstance(this)
    else CameraActivity.newInstance(this), requestCode)
  }
}
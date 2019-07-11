package io.demars.stellarwallet.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.lifecycle.Observer
import io.demars.stellarwallet.BuildConfig
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.helpers.Constants.Companion.STELLAR_ADDRESS_LENGTH
import io.demars.stellarwallet.interfaces.ContactsRepository.ContactOperationStatus
import io.demars.stellarwallet.models.Contact
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import com.google.zxing.integration.android.IntentIntegrator
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat
import kotlinx.android.synthetic.main.activity_stellar_address.*
import timber.log.Timber

class StellarAddressActivity : BaseActivity(), View.OnClickListener {
  enum class Mode {
    PAY_TO, UPDATE_CONTACT, CREATE_CONTACT
  }

  private lateinit var mode: Mode
  /**
   * it will be set only in {@link Mode.UPDATE_CONTACT}
   */
  private lateinit var contact: Contact

  companion object {
    private const val RC_PAY = 222
    private const val ARG_MODE = "ARG_MODE"
    private const val ARG_CONTACT = "ARG_CONTACT"

    fun toPay(context: Context): Intent {
      val intent = Intent(context, StellarAddressActivity::class.java)
      intent.putExtra(ARG_MODE, Mode.PAY_TO)
      return intent
    }

    fun updateContact(context: Context, contactId: Contact): Intent {
      val intent = Intent(context, StellarAddressActivity::class.java)
      intent.putExtra(ARG_MODE, Mode.UPDATE_CONTACT)
      intent.putExtra(ARG_CONTACT, contactId)
      return intent
    }

    fun createContact(context: Context): Intent {
      val intent = Intent(context, StellarAddressActivity::class.java)
      intent.putExtra(ARG_MODE, Mode.CREATE_CONTACT)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stellar_address)

    if (intent.hasExtra(ARG_MODE) && intent.getSerializableExtra(ARG_MODE) != null) {
      mode = intent.getSerializableExtra(ARG_MODE) as Mode
    } else {
      throw IllegalStateException("Missing intent extra {$ARG_MODE}")
    }

    if (mode == Mode.UPDATE_CONTACT) {
      contact = intent.getParcelableExtra(ARG_CONTACT)
        ?: throw IllegalStateException("Missing intent extra {$ARG_CONTACT}")
    }

    setupUI()
  }

  override fun onResume() {
    super.onResume()

    if (mode == Mode.PAY_TO) updateAvailableBalance()
  }

  private fun initiateScan() {
    IntentIntegrator(this).setBeepEnabled(false).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).initiateScan()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
    if (result != null) {
      if (result.contents == null) {
        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show()
      } else {
        addressTextView.setText(result.contents)
        bottomButton.isEnabled = true
      }
    } else {
      when (requestCode) {
        ContactsActivity.RC_PAY_TO_CONTACT -> if (resultCode == RESULT_OK) finish()
        RC_PAY -> if (resultCode == RESULT_OK) finish()
        else -> super.onActivityResult(requestCode, resultCode, data)
      }
    }
  }

  //region User Interface
  private fun setupUI() {
    setSupportActionBar(toolbar)

    supportActionBar?.let {
      it.setDisplayHomeAsUpEnabled(true)
      when (mode) {
        Mode.PAY_TO -> it.title = getString(R.string.button_pay)
        Mode.UPDATE_CONTACT -> it.title = getString(R.string.update_contact_title, contact.name)
        Mode.CREATE_CONTACT -> it.title = getString(R.string.add_contact_title)
      }
    }

    when (mode) {
      Mode.PAY_TO -> {
        bottomButton.text = getString(R.string.next_button_text)
        sendToContactButton.visibility = View.VISIBLE
        contactNameText.visibility = View.GONE
        contactNameEditText.visibility = View.GONE
        addressTitleText.visibility = View.GONE
      }
      Mode.UPDATE_CONTACT -> {
        titleBalance.visibility = View.GONE
        bottomButton.text = getString(R.string.save_button)
        sendToContactButton.visibility = View.GONE
        contactNameText.visibility = View.GONE
        contactNameEditText.visibility = View.GONE
        addressTitleText.text = getString(R.string.stellar_address_title)
        addressTextView.setText(contact.stellarAddress)
      }
      Mode.CREATE_CONTACT -> {
        titleBalance.visibility = View.GONE
        sendToContactButton.visibility = View.GONE
        bottomButton.text = getString(R.string.create_button)
        addressTitleText.text = getString(R.string.stellar_address_title)
      }
    }

    cameraImageButton.setOnClickListener(this)
    bottomButton.setOnClickListener(this)
    sendToContactButton.setOnClickListener(this)
  }

  @SuppressLint("SetTextI18n")
  private fun updateAvailableBalance() {
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      var assetCode = DmcApp.userSession.getSessionAsset().assetCode
      assetCode = if (assetCode == "native") "XLM" else assetCode
      val available = AccountUtils.getAvailableBalance(assetCode)
      val decimalPlaces = AssetUtils.getMaxDecimals(assetCode)
      val availableStr = "$assetCode\n${StringFormat.truncateDecimalPlaces(available, decimalPlaces)}"
      titleBalance.text = availableStr

      val logo = AssetUtils.getLogo(assetCode)
      imageLogo.visibility = if (logo != 0) View.VISIBLE else View.GONE
      imageLogo.setImageResource(logo)
    })
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // This feature is disabled in release since it is not useful for the user.
    if (mode == Mode.UPDATE_CONTACT && BuildConfig.DEBUG) {
      menuInflater.inflate(R.menu.contact_details, menu)
    }
    return true
  }

  override fun onClick(v: View) {
    val address = addressTextView.text.toString()
    when (v.id) {
      R.id.cameraImageButton -> {
        initiateScan()
      }
      R.id.sendToContactButton -> {
        startActivityForResult(ContactsActivity.withKey(this), ContactsActivity.RC_PAY_TO_CONTACT)
      }
      R.id.bottomButton -> {
        when (mode) {
          Mode.PAY_TO -> {
            if (address.length == STELLAR_ADDRESS_LENGTH && address != DmcApp.wallet.getStellarAccountId()) {
              startActivityForResult(PayActivity.newIntent(this, address), RC_PAY)
              overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
            } else {
              // Shake animation on the text
              val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
              addressTextView.startAnimation(shakeAnimation)
            }
          }
          Mode.UPDATE_CONTACT -> {
            val status = ContactsRepositoryImpl(applicationContext).createOrUpdateStellarAddress(contact.name, address)
            when (status) {
              ContactOperationStatus.UPDATED -> {
                Timber.v("data updated")
                Toast.makeText(applicationContext, "stellar address updated", Toast.LENGTH_SHORT).show()
                finish()
              }
              ContactOperationStatus.INSERTED -> {
                Timber.v("data inserted")
                Toast.makeText(applicationContext, "stellar address inserted", Toast.LENGTH_SHORT).show()
                finish()
              }
              ContactOperationStatus.FAILED -> {
                Timber.v("failed to update contact")
                Toast.makeText(applicationContext, "stellar address failed to be added", Toast.LENGTH_SHORT).show()
                finish()
              }
            }
          }
          Mode.CREATE_CONTACT -> {
            val name = contactNameEditText.text.toString()
            if (name.isBlank() || address.isBlank()) {
              Toast.makeText(applicationContext, "one or more fields are empty", Toast.LENGTH_SHORT).show()
            } else {
              val contactId = ContactsRepositoryImpl(applicationContext).createContact(name, address)
              if (contactId == -1L) {
                Toast.makeText(applicationContext, "failed to create the new contact", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(applicationContext, "contact has been created", Toast.LENGTH_SHORT).show()
              }
              finish()
            }
          }
        }
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item != null) {
      if (item.itemId == R.id.nav_open_contact) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString())
        intent.data = uri
        startActivity(intent)
        finish()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }

  //endregion
}

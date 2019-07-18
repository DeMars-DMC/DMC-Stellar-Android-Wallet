package io.demars.stellarwallet.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.lifecycle.Observer
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.helpers.Constants.Companion.STELLAR_ADDRESS_LENGTH
import io.demars.stellarwallet.models.Contact
import com.google.zxing.integration.android.IntentIntegrator
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.mvvm.account.AccountRepository
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_pay_to.*

class PayToActivity : BaseActivity(), View.OnClickListener {

  private lateinit var assetCode: String

  companion object {
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val RC_PAY = 222

    fun toPay(context: Context, assetCode: String): Intent =
      Intent(context, PayToActivity::class.java).apply {
        putExtra(ARG_ASSET_CODE, assetCode)
      }

//    fun updateContact(context: Context, contactId: Contact): Intent {
//      val intent = Intent(context, PayToActivity::class.java)
//      intent.putExtra(ARG_MODE, Mode.UPDATE_CONTACT)
//      intent.putExtra(ARG_CONTACT, contactId)
//      return intent
//    }
//
//    fun createContact(context: Context): Intent {
//      val intent = Intent(context, PayToActivity::class.java)
//      intent.putExtra(ARG_MODE, Mode.CREATE_CONTACT)
//      return intent
//    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_pay_to)

    assetCode = intent.getStringExtra(ARG_ASSET_CODE)

    setupUI()
    loadAvailableBalance()
  }

  override fun onResume() {
    super.onResume()

    AccountRepository.refresh()
  }

  private fun openQrScanner() {
    IntentIntegrator(this).setBeepEnabled(false)
      .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).initiateScan()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
    if (result != null) {
      if (result.contents == null) {
        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show()
      } else {
        addressEditText.setText(result.contents)
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
    backButton.setOnClickListener {
      onBackPressed()
    }

    cameraImageButton.setOnClickListener {
      openQrScanner()
    }

    bottomButton.setOnClickListener {
      validateAndProceed()
    }
  }

  private fun validateAndProceed() {
    val address = addressEditText.text.toString()
    if (address.length == STELLAR_ADDRESS_LENGTH && address != DmcApp.wallet.getStellarAccountId()) {
      startActivityForResult(PayActivity.newIntent(this, assetCode, address), RC_PAY)
      overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
    } else {
      // Shake animation on the text
      val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
      addressEditText.startAnimation(shakeAnimation)
    }
  }

  @SuppressLint("SetTextI18n")
  private fun loadAvailableBalance() {
    AccountRepository.refreshData().observe(this, Observer<AccountRepository.AccountEvent> {
      val available = AccountUtils.getAvailableBalance(assetCode)
      val decimalPlaces = AssetUtils.getMaxDecimals(assetCode)
      val availableStr = StringFormat.truncateDecimalPlaces(available, decimalPlaces)
      assetName.text = assetCode
      assetBalance.text = availableStr

      val logo = AssetUtils.getLogo(assetCode)
      assetLogo.visibility = if (logo != 0) View.VISIBLE else View.GONE
      assetLogo.setImageResource(logo)
    })
  }

  override fun onClick(v: View) {
    when (v.id) {
//      R.id.sendToContactButton -> {
//        startActivityForResult(ContactsActivity.withKey(this), ContactsActivity.RC_PAY_TO_CONTACT)
//      }
//      R.id.bottomButton -> {
//        when (mode) {
//          Mode.UPDATE_CONTACT -> {
//            val status = ContactsRepositoryImpl(applicationContext).createOrUpdateStellarAddress(contact.name, address)
//            when (status) {
//              ContactOperationStatus.UPDATED -> {
//                Timber.v("data updated")
//                Toast.makeText(applicationContext, "stellar address updated", Toast.LENGTH_SHORT).show()
//                finish()
//              }
//              ContactOperationStatus.INSERTED -> {
//                Timber.v("data inserted")
//                Toast.makeText(applicationContext, "stellar address inserted", Toast.LENGTH_SHORT).show()
//                finish()
//              }
//              ContactOperationStatus.FAILED -> {
//                Timber.v("failed to update contact")
//                Toast.makeText(applicationContext, "stellar address failed to be added", Toast.LENGTH_SHORT).show()
//                finish()
//              }
//            }
//          }
//          Mode.CREATE_CONTACT -> {
//            val name = "" /*contactNameEditText.text.toString()*/
//            if (name.isBlank() || address.isBlank()) {
//              Toast.makeText(applicationContext, "one or more fields are empty", Toast.LENGTH_SHORT).show()
//            } else {
//              val contactId = ContactsRepositoryImpl(applicationContext).createContact(name, address)
//              if (contactId == -1L) {
//                Toast.makeText(applicationContext, "failed to create the new contact", Toast.LENGTH_SHORT).show()
//              } else {
//                Toast.makeText(applicationContext, "contact has been created", Toast.LENGTH_SHORT).show()
//              }
//              finish()
//            }
//          }
//        }
//      }
    }
  }

  //region Contacts
//  private fun populateList(list: ArrayList<Contact>, isFilteredList: Boolean = false) {
//    rv_contact_list?.adapter = ContactsAdapter(list)
//    when (rv_contact_list?.itemDecorationCount) {
//      0 -> rv_contact_list?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
//      2 -> rv_contact_list?.removeItemDecorationAt(1)
//    }
//    val item = RecyclerSectionItemDecoration(resources.getDimension(R.dimen.padding_vertical_double).toInt(), true, getSectionCallback(list))
//    rv_contact_list?.addItemDecoration(item)
//    progress_view?.visibility = View.GONE
//    if (list.size == 0) {
//      if (isFilteredList) {
//        empty_view?.text = getString(R.string.no_results_found)
//      } else {
//        empty_view?.text = getString(R.string.no_contacts_found)
//      }
//      empty_view?.visibility = View.VISIBLE
//      rv_contact_list?.visibility = View.GONE
//    } else {
//      empty_view?.visibility = View.GONE
//      rv_contact_list?.visibility = View.VISIBLE
//    }
//  }
  //endregion

  override fun onBackPressed() {
    super.onBackPressed()
    ViewUtils.hideKeyboard(this)
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }

  //endregion
}

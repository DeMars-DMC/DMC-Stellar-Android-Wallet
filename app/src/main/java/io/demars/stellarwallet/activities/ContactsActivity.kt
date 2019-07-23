package io.demars.stellarwallet.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.ContactsAdapter
import io.demars.stellarwallet.helpers.OnTextChanged
import io.demars.stellarwallet.interfaces.ContactListener
import io.demars.stellarwallet.interfaces.ContactsRepository
import io.demars.stellarwallet.interfaces.OnSearchStateListener
import io.demars.stellarwallet.models.Contact
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_contacts.*
import timber.log.Timber

class ContactsActivity : BaseActivity(), ContactListener {

  enum class Mode {
    ALL, STELLAR
  }

  private lateinit var mode: Mode

  private var assetCode = ""
  private var assetIssuer = ""

  private var currentContactList = ArrayList<Contact>()

  private lateinit var searchButton: MenuItem
  private lateinit var refreshButton: MenuItem
  private lateinit var addContactButton: MenuItem
  private lateinit var bottomSheet: BottomSheetDialog
  private lateinit var contactTitleView: TextView
  private lateinit var contactNameView: EditText
  private lateinit var contactAddressView: EditText
  private lateinit var contactSaveButton: Button

  private var menuItemsInitialized = false

  companion object {
    private const val RC_PAY_TO_CONTACT = 111
    private const val RC_PERMISSION = 222

    private const val ARG_MODE = "ARG_MODE"
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val ARG_ASSET_ISSUER = "ARG_ASSET_ISSUER"

    fun newInstance(context: Context, assetCode: String, assetIssuer: String): Intent =
      Intent(context, ContactsActivity::class.java).apply {
        putExtra(ARG_MODE, Mode.ALL)
        putExtra(ARG_ASSET_CODE, assetCode)
        putExtra(ARG_ASSET_ISSUER, assetIssuer)
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_contacts)

    checkIntent()

    initUI()
    initBottomSheet()
    checkRationale()
    requestContacts()

  }

  private fun checkIntent() {
    if (intent.hasExtra(ARG_MODE) && intent.getSerializableExtra(ARG_MODE) != null) {
      mode = intent.getSerializableExtra(ARG_MODE) as Mode
    }

    assetCode = intent.getStringExtra(ARG_ASSET_CODE)
    assetIssuer = intent.getStringExtra(ARG_ASSET_ISSUER)
  }

  private fun initUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { onBackPressed() }


    rv_contact_list.layoutManager = LinearLayoutManager(this)

    val clearButton = searchBar.findViewById<View>(R.id.mt_clear)
    clearButton.visibility = View.GONE
    searchBar.addTextChangeListener(
      object : OnTextChanged() {
        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
          filterResults(text.toString())
          if (text.isEmpty()) {
            clearButton.visibility = View.GONE
          } else {
            clearButton.visibility = View.VISIBLE
          }
        }
      })
  }

  private fun initBottomSheet() {
    bottomSheet = BottomSheetDialog(this).apply {
      val sheetView = layoutInflater.inflate(R.layout.dialog_contact, rootView, false)
      contactTitleView = sheetView.findViewById(R.id.dialogTitle)
      contactNameView = sheetView.findViewById(R.id.contactNameView)
      contactAddressView = sheetView.findViewById(R.id.contactAddressView)
      contactSaveButton = sheetView.findViewById(R.id.saveButton)
      setContentView(sheetView)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu to use in the action bar
    menuInflater.inflate(R.menu.contacts_fragment_menu, menu)
    refreshButton = menu.findItem(R.id.refresh_contacts)
    searchButton = menu.findItem(R.id.search_contacts)
    addContactButton = menu.findItem(R.id.add_contact)
    menuItemsInitialized = true
    setMenuItemsEnable(true)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    // Handle presses on the action bar menu items
    return when (item?.itemId) {
      R.id.refresh_contacts -> {
        refreshContacts()
        true
      }
      R.id.search_contacts -> {
        enableSearch()
        true
      }
      R.id.add_contact -> {
        addContact()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun refreshContacts() {
    if (checkNeedPermissions()) {
      toast("Please grant needed permissions to add refresh Contacts")
    } else {
      setInitialStateContacts()
      refreshButton.isEnabled = false
      showContacts(true)
    }
  }

  private fun enableSearch() {
    viewFlipper.showNext()
    searchBar.enableSearch()
    searchBar.setOnSearchActionListener(object : OnSearchStateListener() {
      override fun onSearchStateChanged(enabled: Boolean) {
        if (!enabled) {
          searchBar.text = null
          viewFlipper.showPrevious()
        }
      }
    })
  }

  private fun addContact() {
    if (checkNeedPermissions()) {
      toast("Please grant needed permissions to add new Contact")
    } else {
      showBottomSheet(null)
    }
  }

  private fun showBottomSheet(contact: Contact?) {
    if (contact == null) {
      contactTitleView.setText(R.string.add_contact)
      contactNameView.visibility = View.VISIBLE
      contactNameView.setText("")
      contactAddressView.setText("")
      contactSaveButton.setOnClickListener {
        createContact(contactNameView.text.toString(), contactAddressView.text.toString())
      }
    } else {
      contactTitleView.text = contact.name
      contactNameView.visibility = View.GONE
      contactAddressView.setText(contact.stellarAddress)
      contactSaveButton.setOnClickListener {
        updateContact(contact, contactAddressView.text.toString())
      }
    }

    bottomSheet.show()
  }
  private fun setEnablePermissionsState(shouldShowRationale: Boolean) {
    rv_contact_list.visibility = View.GONE
    empty_view.visibility = View.GONE
    progress_view.visibility = View.GONE
    enable_permissions.visibility = View.VISIBLE
    allow_permissions_button.setOnClickListener {
      if (shouldShowRationale) {
        requestContacts()
      } else {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Contact Permission Needed")
        builder.setMessage("Open Settings, then tap Permissions and turn on Contacts.")
        builder.setPositiveButton("Open Settings") { _, _ ->
          val intent = getAppSettingsIntent(packageName)
          startActivity(intent)
        }.create().show()

      }
    }
  }

  private fun setMenuItemsEnable(isEnabled: Boolean) {
    if (menuItemsInitialized) {
      refreshButton.isEnabled = isEnabled
      searchButton.isEnabled = isEnabled
      addContactButton.isEnabled = isEnabled
    }
  }

  private fun setInitialStateContacts() {
    setMenuItemsEnable(true)
    rv_contact_list?.visibility = View.GONE
    empty_view?.visibility = View.GONE
    progress_view?.visibility = View.VISIBLE
    enable_permissions?.visibility = View.GONE
  }

  private fun requestContacts() {
    if (checkNeedPermissions()) {
      requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), RC_PERMISSION)
    } else {
      showContacts()
    }
  }

  private fun checkNeedPermissions(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
      hasPermissions(this, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
  }

  private fun hasPermissions(context: Context, vararg permissions: String): Int {
    for (permission in permissions) {
      if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
        return PackageManager.PERMISSION_DENIED
      }
    }
    return PackageManager.PERMISSION_GRANTED
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == RC_PERMISSION) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        showContacts()
      } else {
        checkRationale()
        Timber.e("Permissions Access denied")
      }
    }
  }

  private fun checkRationale() {
    val shouldShowRationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
      (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) ||
        shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS))
    setEnablePermissionsState(shouldShowRationale)
  }

  private fun showContacts(forceRefresh: Boolean = false) {
    setInitialStateContacts()
    ContactsRepositoryImpl(this).getContactsListLiveData(forceRefresh).observe(this, Observer {
      Timber.d("Observer triggered {${it?.stellarContacts?.size}")
      it?.let { that ->
        when (mode) {
          Mode.ALL -> {
            currentContactList = ArrayList(that.contacts)
            currentContactList.addAll(0, that.stellarContacts)
          }
          Mode.STELLAR -> {
            currentContactList = ArrayList(that.stellarContacts)
          }
        }


        populateList(currentContactList)
        if (::refreshButton.isInitialized) {
          refreshButton.isEnabled = true
        }
      }
    })
  }

  private fun filterResults(input: String) {
    val filterList: ArrayList<Contact> = ArrayList()
    currentContactList.forEach {
      it.name.let { name ->
        if (name.toLowerCase().contains(input.toLowerCase())) {
          filterList.add(it)
        }
      }
    }
    if (filterList.isNotEmpty()) {
      populateList(filterList, true)
    }
  }


  private fun populateList(list: ArrayList<Contact>, isFilteredList: Boolean = false) {
    rv_contact_list?.adapter = ContactsAdapter(list, this)
    when (rv_contact_list?.itemDecorationCount) {
      0 -> rv_contact_list?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
      2 -> rv_contact_list?.removeItemDecorationAt(1)
    }

    progress_view?.visibility = View.GONE
    if (list.size == 0) {
      if (isFilteredList) {
        empty_view?.text = getString(R.string.no_results_found)
      } else {
        empty_view?.text = getString(R.string.no_contacts_found)
      }
      empty_view?.visibility = View.VISIBLE
      rv_contact_list?.visibility = View.GONE
    } else {
      empty_view?.visibility = View.GONE
      rv_contact_list?.visibility = View.VISIBLE
    }
  }

  override fun addAddressToContact(contact: Contact) {
    showBottomSheet(contact)
  }

  override fun onContactSelected(contact: Contact) {
    showBottomSheet(contact)
  }

  override fun onPayToContact(contact: Contact) {
    val address = contact.stellarAddress
    if (address.isNullOrEmpty()) {
      toast("Can't find stellar address for ${contact.name}")
    } else {
      startActivityForResult(PayActivity.newIntent(this, assetCode, assetIssuer, address),
        RC_PAY_TO_CONTACT)
    }
  }

  /**
   * Intent to show an applications details page in (Settings) com.android.settings
   *
   * @param packageName   The package name of the application
   * @return the intent to open the application info screen.
   */
  private fun getAppSettingsIntent(packageName: String): Intent {
    val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.parse("package:$packageName")
    return intent
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      RC_PAY_TO_CONTACT -> if (resultCode == RESULT_OK) {
        setResult(Activity.RESULT_OK)
        finish()
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    ViewUtils.hideKeyboard(this)
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }

  private fun updateContact(contact: Contact, address: String) {
    val status = ContactsRepositoryImpl(this).createOrUpdateStellarAddress(contact.name, address)
    if (status == ContactsRepository.ContactOperationStatus.FAILED) {
      toast("Failed to update ${contact.name}")
    } else {
      toast("${contact.name} address updated")
      bottomSheet.dismiss()
    }
  }

  private fun createContact(name: String, address: String) {
    if (name.isBlank() || address.isBlank()) {
      toast("All fields are necessary")
    } else {
      val contactId = ContactsRepositoryImpl(this).createContact(name, address)
      if (contactId == -1L) {
        toast("Failed to create the new contact")
      } else {
        toast("Contact has been created")
        bottomSheet.dismiss()
      }
    }
  }
}
package io.demars.stellarwallet.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import io.demars.stellarwallet.activities.StellarAddressActivity
import io.demars.stellarwallet.adapters.ContactsAdapter
import io.demars.stellarwallet.helpers.OnTextChanged
import io.demars.stellarwallet.interfaces.OnSearchStateListener
import io.demars.stellarwallet.models.Contact
import io.demars.stellarwallet.views.RecyclerSectionItemDecoration
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.fragment_contact_list.*
import timber.log.Timber
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.activities.ContactsActivity

/**
 * Fragment that holds the RecyclerView
 */
class ContactsFragment : Fragment() {
  private lateinit var mode: ContactsActivity.Mode

  companion object {
    // Request code for READ_CONTACTS. It can be any number > 0.
    private const val PERMISSIONS_REQUEST_CONTACTS = 100

    private const val ARG_MODE = "ARG_MODE"
    fun newInstance(mode: ContactsActivity.Mode) = ContactsFragment().apply {
      arguments = bundleOf(ARG_MODE to mode)
    }
  }

  // Defines a variable for the search string
  private lateinit var appContext: Context
  private var currentContactList = ArrayList<Contact>()

  private lateinit var searchButton: MenuItem
  private lateinit var refreshButton: MenuItem
  private lateinit var addContactButton: MenuItem
  private var menuItemsInitialized = false
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  // A UI Fragment must inflate its View
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View {
    mode = if (arguments?.get(ARG_MODE) == ContactsActivity.Mode.WITH_KEY)
      ContactsActivity.Mode.WITH_KEY else ContactsActivity.Mode.ALL
    return inflater.inflate(R.layout.fragment_contact_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appContext = view.context.applicationContext
    activity?.let {
      @Suppress("CAST_NEVER_SUCCEEDS")
      (it as AppCompatActivity).setSupportActionBar(toolbar)
      if (mode == ContactsActivity.Mode.WITH_KEY) {
        it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
      }

      rv_contact_list.layoutManager = LinearLayoutManager(it)
    }

    checkRationale()
    requestContacts()

    // This logic around mt_clear button is hack to fix #191, it should be removed if the bug is approved fix and released.
    // https://github.com/mancj/MaterialSearchBar/issues/104
    val clearButton = searchBar.findViewById<View>(R.id.mt_clear)
    clearButton.visibility = View.GONE
    searchBar.addTextChangeListener(object : OnTextChanged() {
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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    // Inflate the menu to use in the action bar
    inflater.inflate(R.menu.contacts_fragment_menu, menu)
    refreshButton = menu.findItem(R.id.refresh_contacts)
    searchButton = menu.findItem(R.id.search_contacts)
    addContactButton = menu.findItem(R.id.add_contact)
    setMenuItemsEnable(false)
    menuItemsInitialized = true
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle presses on the action bar menu items
    when (item.itemId) {
      android.R.id.home -> {
        activity?.onBackPressed()
        return true
      }
      R.id.refresh_contacts -> {
        setInitialStateContacts()
        refreshButton.isEnabled = false
        showContacts(true)
        return true
      }
      R.id.search_contacts -> {
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
        return true
      }
      R.id.add_contact -> {
        activity?.let {
          startActivity(StellarAddressActivity.createContact(it))
        }
        return true
      }
    }
    return super.onOptionsItemSelected(item)
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
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Contact Permission Needed")
        builder.setMessage("Open Settings, then tap Permissions and turn on Contacts.")
        builder.setPositiveButton("Open Settings") { _, _ ->
          context?.let {
            val intent = getAppSettingsIntent(appContext.packageName)
            startActivity(intent)
          }
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
    rv_contact_list.visibility = View.GONE
    empty_view.visibility = View.GONE
    progress_view.visibility = View.VISIBLE
    enable_permissions.visibility = View.GONE
  }

  private fun requestContacts() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermissions(appContext, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), PERMISSIONS_REQUEST_CONTACTS)
    } else {
      showContacts()
    }
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
    if (requestCode == PERMISSIONS_REQUEST_CONTACTS) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        showContacts()
      } else {
        checkRationale()
        Timber.e("Permissions Access denied")
      }
    }
  }

  private fun checkRationale() {
    val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)
    setEnablePermissionsState(shouldShowRationale)
  }

  private fun showContacts(forceRefresh: Boolean = false) {
    setInitialStateContacts()
    ContactsRepositoryImpl(appContext).getContactsListLiveData(forceRefresh).observe(viewLifecycleOwner, Observer {
      Timber.d("Observer triggered {${it?.stellarContacts?.size}")
      it?.let { that ->
        currentContactList = ArrayList(that.contacts)
        currentContactList.addAll(0, that.stellarContacts)
        if (mode == ContactsActivity.Mode.WITH_KEY) {
          val contactsWithKey = ArrayList<Contact>()
          currentContactList.forEach {
            if (!it.stellarAddress.isNullOrEmpty()) {
              contactsWithKey.add(it)
            }
          }

          currentContactList.clear()
          currentContactList.addAll(contactsWithKey)
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
    if (!filterList.isEmpty()) {
      populateList(filterList, true)
    }
  }


  private fun populateList(list: ArrayList<Contact>, isFilteredList: Boolean = false) {
    rv_contact_list.adapter = ContactsAdapter(list)
    when (rv_contact_list.itemDecorationCount) {
      0 -> rv_contact_list.addItemDecoration(DividerItemDecoration(rv_contact_list.context, DividerItemDecoration.VERTICAL))
      2 -> rv_contact_list.removeItemDecorationAt(1)
    }
    val item = RecyclerSectionItemDecoration(appContext.resources.getDimension(R.dimen.padding_vertical_double).toInt(), true, getSectionCallback(list))
    rv_contact_list.addItemDecoration(item)
    progress_view.visibility = View.GONE
    if (list.size == 0) {
      if (isFilteredList) {
        empty_view.text = getString(R.string.no_results_found)
      } else {
        empty_view.text = getString(R.string.no_contacts_found)
      }
      empty_view.visibility = View.VISIBLE
      rv_contact_list.visibility = View.GONE
    } else {
      empty_view.visibility = View.GONE
      rv_contact_list.visibility = View.VISIBLE
    }
  }


  private fun getSectionCallback(list: List<Contact>): RecyclerSectionItemDecoration.SectionCallback {
    return object : RecyclerSectionItemDecoration.SectionCallback {
      override fun isSection(position: Int): Boolean {
        if (position == 0) return true
        val addressIsEmpty = list[position].stellarAddress.isNullOrEmpty()
        val previousAddressIsEmpty = list[position - 1].stellarAddress.isNullOrEmpty()
        return addressIsEmpty xor previousAddressIsEmpty
      }

      override fun getSectionHeader(position: Int): CharSequence {
        return if (list[position].stellarAddress.isNullOrBlank()) {
          getString(R.string.contact_header)
        } else {
          getString(R.string.stellar_contact_header)
        }
      }
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
}

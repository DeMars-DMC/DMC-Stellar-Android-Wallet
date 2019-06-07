package io.demars.stellarwallet.activities

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.fragments.*
import io.demars.stellarwallet.utils.GlobalGraphHelper
import io.demars.stellarwallet.utils.KeyboardUtils
import timber.log.Timber
import android.content.Intent

class WalletActivity : BaseActivity(), KeyboardUtils.SoftKeyboardToggleListener {
  private enum class WalletFragmentType {
    START,
    WALLET,
    EXCHANGE,
    CONTACTS,
    SETTING
  }

  companion object {
    const val RC_ASSETS = 111
  }

  private lateinit var dialogTradeAlert: Dialog
  private lateinit var bottomNavigation: BottomNavigationView
  private var currentItemSelected: Int = -1
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_wallet)

    dialogTradeAlert = createTradingErrorDialog()

    WalletApplication.wallet.getStellarAccountId().let { address ->
      Firebase.getCurrentUserUid()?.let { uid ->
        Firebase.getUserStellarAddress(object : ValueEventListener {
          override fun onDataChange(data: DataSnapshot) {
            if (!data.exists() || data.getValue(String::class.java).isNullOrBlank()) {
              // User doesn't have stellar address attached yet so we add it to Firebase
              Firebase.getStellarAddressRef(uid).setValue(address)
            } else {
              val stellarAddressRemote = data.getValue(String::class.java)
              if (address != stellarAddressRemote) {
                // Address not matching show error and restart
                onNonMatchingWalletRecovered()
              } else {
                // All the checks passed user can use the app
              }
            }
          }

          override fun onCancelled(error: DatabaseError) {

          }
        })
      }
    }

    setupUI()
  }

  private fun onNonMatchingWalletRecovered() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(getString(R.string.non_matching_wallet_title))
      .setMessage(getString(R.string.non_matching_wallet_message))
      .setPositiveButton(getString(R.string.try_again)) { _, _ ->
        GlobalGraphHelper.wipeAndRestart(this@WalletActivity)
      }
      .setNeutralButton(getString(R.string.contact_us)) { _, _ ->
        val uid = Firebase.getCurrentUserUid()
        GlobalGraphHelper.wipeAndRestart(this@WalletActivity)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@demars.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Cannot access to Stellar Wallet: UID - $uid")
        startActivity(Intent.createChooser(intent, "Send email to DMC support"))
      }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
  }

  private fun getReusedFragment(tag: String): Fragment? {
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    if (fragment != null) {
      Timber.d("reused a cached fragment {$tag}")
    }
    return fragment
  }

  //region Navigation

  private fun createTradingErrorDialog(): Dialog {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(getString(R.string.exchange_alert_title))
    builder.setMessage(getString(R.string.exchange_alert_message))
    builder.setPositiveButton(getString(R.string.exchange_alert_positive_button)) { _, _ -> startActivity(AssetsActivity.newInstance(this)) }
    builder.setNegativeButton(getString(R.string.exchange_alert_negative_button)) { dialog, _ -> dialog.cancel() }
    val dialog = builder.create()

    dialog.setOnCancelListener {
      bottomNavigation.selectedItemId = R.id.nav_wallet
    }
    dialog.setCanceledOnTouchOutside(false)
    return dialog
  }

  private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
    //let's ignore item selection in the current item
    if (currentItemSelected != item.itemId) {
      currentItemSelected = item.itemId
      when (item.itemId) {
        R.id.nav_wallet -> {
          val walletFragment = getReusedFragment(WalletFragmentType.WALLET.name)
            ?: WalletFragment.newInstance()
          replaceFragment(walletFragment, WalletFragmentType.WALLET)
        }
        R.id.nav_exchange -> {
          // minimum two trades
          if (!enoughAssetsToTrade()) {
            dialogTradeAlert.show()
          }
          val tradingFragment = getReusedFragment(WalletFragmentType.EXCHANGE.name)
            ?: ExchangeFragment.newInstance()
          replaceFragment(tradingFragment, WalletFragmentType.EXCHANGE)
        }
        R.id.nav_contacts -> {
          replaceFragment(getReusedFragment(WalletFragmentType.CONTACTS.name)
            ?: ContactsFragment(), WalletFragmentType.CONTACTS)
        }
        R.id.nav_settings -> {
          val settingsFragment = getReusedFragment(WalletFragmentType.SETTING.name)
            ?: SettingsFragment.newInstance()
          replaceFragment(settingsFragment, WalletFragmentType.SETTING)
        }
        else -> throw IllegalAccessException("Navigation item not supported $item.title(${item.itemId})")
      }
    }
    return@OnNavigationItemSelectedListener true
  }

  private fun replaceFragment(fragment: Fragment, type: WalletFragmentType) {
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(R.id.content_container, fragment, type.name)
    //This is complete necessary to be able to reuse the fragments using the supportFragmentManager
    transaction.addToBackStack(null)
    transaction.commitAllowingStateLoss()
  }

  private fun setupUI() {
    bottomNavigation = findViewById(R.id.navigationView)
    bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    bottomNavigation.selectedItemId = R.id.nav_wallet
  }

  //endregion

  override fun onResume() {
    super.onResume()

    if (bottomNavigation.selectedItemId == R.id.nav_exchange) {
      if (!enoughAssetsToTrade()) {
        dialogTradeAlert.show()
      }
    }
    KeyboardUtils.addKeyboardToggleListener(this, this)
  }

  override fun onPause() {
    super.onPause()
    KeyboardUtils.removeKeyboardToggleListener(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (dialogTradeAlert.isShowing) {
      dialogTradeAlert.dismiss()
    }
  }

  private fun enoughAssetsToTrade(): Boolean {
    val balances = WalletApplication.wallet.getBalances()
    //minimum 2 assets to trade
    return balances.size > 1
  }

  /**
   * When the keyboard is opened the bottomNavigation gets pushed up.
   */
  override fun onToggleSoftKeyboard(isVisible: Boolean) {
    if (isVisible) {
      bottomNavigation.visibility = View.GONE
    } else {
      bottomNavigation.visibility = View.VISIBLE
    }
  }

  override fun onBackPressed() {
    if (bottomNavigation.selectedItemId != R.id.nav_wallet) {
      bottomNavigation.selectedItemId = R.id.nav_wallet
    } else {
      finish()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when {
      requestCode == RC_ASSETS && resultCode == RESULT_OK -> bottomNavigation.selectedItemId = R.id.nav_exchange
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  fun openAssetsActivity() {
    startActivityForResult(Intent(this, AssetsActivity::class.java), RC_ASSETS)
    overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
  }
}

package io.demars.stellarwallet.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.demars.stellarwallet.R
import io.demars.stellarwallet.fragments.ContactsFragment

class ContactsActivity : BaseActivity() {
  enum class Mode {
    ALL, STELLAR
  }

  private lateinit var mode: Mode
  private lateinit var fragment: ContactsFragment

  companion object {
    const val RC_PAY_TO_CONTACT = 111
    private const val ARG_MODE = "ARG_MODE"

    fun all(context: Context): Intent {
      val intent = Intent(context, ContactsActivity::class.java)
      intent.putExtra(ARG_MODE, Mode.ALL)
      return intent
    }

    fun withKey(context: Context): Intent {
      val intent = Intent(context, ContactsActivity::class.java)
      intent.putExtra(ARG_MODE, Mode.STELLAR)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_contacts)

    if (intent.hasExtra(ARG_MODE) && intent.getSerializableExtra(ARG_MODE) != null) {
      mode = intent.getSerializableExtra(ARG_MODE) as Mode
    }

    showFragment()
  }

  private fun showFragment() {
    fragment = ContactsFragment.newInstance(mode)
    val transaction = supportFragmentManager.beginTransaction()
    transaction.replace(R.id.content_container, fragment)
    transaction.commit()
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
}
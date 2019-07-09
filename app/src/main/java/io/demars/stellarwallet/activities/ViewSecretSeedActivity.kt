package io.demars.stellarwallet.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_view_secret_seed.*

class ViewSecretSeedActivity : BaseActivity() {

    companion object {
        private const val ARG_SECRET_SEED = "ARG_SECRET_SEED"
        fun newInstance(context: Context, secretSeed : String) : Intent {
            val intent = Intent(context, ViewSecretSeedActivity::class.java)
            intent.putExtra(ARG_SECRET_SEED, secretSeed)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_secret_seed)

         intent?.extras?.let{
            val secretSeed = it.getString(ARG_SECRET_SEED)
            secretSeed?.let {
                secretSeedTextView.text = secretSeed
                copyAddressButton.setOnClickListener { copyAddressToClipBoard(secretSeed)  }
            }
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun copyAddressToClipBoard(data: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("DMC Address", data)
        clipboard.primaryClip = clip

        Toast.makeText(this, getString(R.string.address_copied_message), Toast.LENGTH_LONG).show()
    }
}

package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity() {

    companion object {
        private const val ARGS_INTENT_TITLE = "ARGS_INTENT_TITLE"
        private const val ARGS_INTENT_URL = "ARGS_INTENT_URL"

        fun newIntent(context: Context, title: String, url : String): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(ARGS_INTENT_URL, url)
            intent.putExtra(ARGS_INTENT_TITLE, title)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(ARGS_INTENT_URL) && intent.hasExtra(ARGS_INTENT_TITLE)) {
            webview.loadUrl(intent.getStringExtra(ARGS_INTENT_URL))
            toolBar.title = intent.getStringExtra(ARGS_INTENT_TITLE)
        } else {
            throw IllegalStateException("missing intent arguments, please use " + WebViewActivity::class.java.simpleName + "#newIntent(...)")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            if (item.itemId == android.R.id.home) {
                finish()
                return true
            }
        }
        return false
    }
}

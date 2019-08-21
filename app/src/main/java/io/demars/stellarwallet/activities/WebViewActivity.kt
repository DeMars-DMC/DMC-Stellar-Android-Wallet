package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity() {

  companion object {
    private const val ARGS_INTENT_TITLE = "ARGS_INTENT_TITLE"
    private const val ARGS_INTENT_URL = "ARGS_INTENT_URL"

    fun newIntent(context: Context, title: String, url: String): Intent {
      val intent = Intent(context, WebViewActivity::class.java)
      intent.putExtra(ARGS_INTENT_URL, url)
      intent.putExtra(ARGS_INTENT_TITLE, title)
      return intent
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_web_view)

    backButton.setOnClickListener { onBackPressed()}

    if (intent.hasExtra(ARGS_INTENT_URL) && intent.hasExtra(ARGS_INTENT_TITLE)) {
      setupContent(intent.getStringExtra(ARGS_INTENT_URL))
      titleView.text = intent.getStringExtra(ARGS_INTENT_TITLE)
    } else {
      throw IllegalStateException("missing intent arguments, please use " + WebViewActivity::class.java.simpleName + "#newIntent(...)")
    }
  }

  private fun setupContent(url: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      webview.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
          view.loadUrl(request.url.toString())
          return true
        }
      }
    } else {
      webview.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
          view.loadUrl(url)
          return true
        }
      }
    }

    webview.loadUrl(url)
  }
}

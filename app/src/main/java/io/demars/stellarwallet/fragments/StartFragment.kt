package io.demars.stellarwallet.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.fragment_start.*
import android.webkit.WebView
import android.webkit.WebResourceRequest


class StartFragment : BaseFragment() {
  private lateinit var appContext: Context
  private val contentUrl = "https://docs.google.com/document/d/1qSSWxP9cxpyBLcXkB5Dv4ClIj_lam2CyfvTG9mpPgxs/edit?usp=sharing"

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_start, container, false)

  companion object {
    fun newInstance(): StartFragment = StartFragment()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appContext = view.context.applicationContext
    setupContent()
  }

  private fun setupContent() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      webViewStart.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
          view.loadUrl(request.url.toString())
          return true
        }
      }
    } else {
      webViewStart.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
          view.loadUrl(url)
          return true
        }
      }
    }

    webViewStart.loadUrl(contentUrl)
  }
}
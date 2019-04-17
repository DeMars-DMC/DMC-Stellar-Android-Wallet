package io.demars.stellarwallet.views

import android.text.TextPaint
import android.text.style.URLSpan

class DmcURLSpan(url: String) : URLSpan(url) {
  override fun updateDrawState(ds: TextPaint) {
    super.updateDrawState(ds)
    ds.isUnderlineText = false
    ds.isFakeBoldText
  }
}
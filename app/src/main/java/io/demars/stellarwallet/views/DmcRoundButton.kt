package io.demars.stellarwallet.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.demars.stellarwallet.R
import org.jetbrains.anko.textColor

class DmcRoundButton(context: Context, attrs: AttributeSet?) : ImageView(context, attrs) {
  init {
    setBackgroundResource(R.drawable.background_round_clickable)
  }

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    alpha = if (enabled) 1.0f else 0.5f
  }
}
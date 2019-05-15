package io.demars.stellarwallet.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.helpers.Constants

class ConfirmTradeDialog(context: Context,
                         private val sellingText: String,
                         private val buyingText: String,
                         private val sellingCode: String,
                         private val buyingCode: String,
                         private val listener: DialogListener) : AlertDialog(context) {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_confirm_trade)

    findViewById<TextView>(R.id.sellingAmount)?.text = sellingText
    findViewById<TextView>(R.id.buyingAmount)?.text = buyingText

    findViewById<TextView>(R.id.sellingCode)?.text = sellingCode
    findViewById<TextView>(R.id.buyingCode)?.text = buyingCode

    findViewById<ImageView>(R.id.sellingLogo)?.setImageResource(Constants.getLogo(sellingCode))
    findViewById<ImageView>(R.id.buyingLogo)?.setImageResource(Constants.getLogo(buyingCode))

    findViewById<Button>(R.id.negativeButton)?.setOnClickListener {
      dismiss()
    }

    findViewById<Button>(R.id.positiveButton)?.setOnClickListener {
      dismiss()
      listener.onConfirmed(sellingText, buyingText)
    }
  }

  interface DialogListener {
    fun onConfirmed(sellingText: String, buyingText: String)
  }
}
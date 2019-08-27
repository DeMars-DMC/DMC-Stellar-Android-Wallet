package io.demars.stellarwallet.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import io.demars.stellarwallet.R

class LinkifiedDialog(context: Context,
                      private val title: String,
                      private val message: String,
                      private val buttonText: String) : AlertDialog(context) {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_linkified)
    findViewById<TextView>(R.id.dialogTitle).text = title
    findViewById<TextView>(R.id.dialogMessage).text = message

    val positiveButton = findViewById<Button>(R.id.positiveButton)
    positiveButton.text = buttonText
    positiveButton.setOnClickListener { dismiss() }
  }

}
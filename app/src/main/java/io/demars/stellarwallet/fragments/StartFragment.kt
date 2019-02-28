package io.demars.stellarwallet.fragments

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.fragment_start.*

class StartFragment : BaseFragment() {
  private lateinit var appContext : Context
  private final val startContent = "Welcome to the first version of the DMC App. A proof of concept with limited functionality. This App provides you with access to the Stellar network, where you can transfer funds all around the world, within seconds, for only 100 Stroops. That’s fractions of a cent. This is a brief tutorial, to explain what you need to do to set up the wallet and how you can use it to convert bank money into crypto digital assets. \n" +
    "\n" +
    "Load Lumens (XLM)\n" +
    "\n" +
    "In order to deposit, send and exchange money, you first need to deposit lumens. You can buy Lumens for bank money at Coindirect or with other cryptocurrencies at Binance.\n" +
    "\n" +
    "Set Inflation\n" +
    "\n" +
    "Inflation is “free money”. It’s possible to receive 1% per annum of your Lumens (XLM) balance in inflation each week. You can read more about it over here. If you like to keep things simple, we recommend you join the Lumenouts inflation pool as they pay out 100% of the inflation you earn. Join the pool and set your XLM inflation in the App.\n" +
    "\n" +
    "Exchange\n" +
    "\n" +
    "Now you have some money in your account, you can send it instantly to other DMC Wallet holders or you can exchange it for other assets in the Stellar network. You can view the other assets over here and add them to your wallet so you can trade and exchange them. Just remember that for every asset you add, a small amount of XLM, …., is locked up as security.\n" +
    "\n" +
    "Coming soon\n" +
    "\n" +
    "We’re busy building functionality which will allow you to deposit cash and electronic South African Rands and receive a matching credit in DMC RAND assets. These RAND’s can be exchanged for other assets on the Stellar network, like Bitcoin, US Dollars or Euros. \n" +
    "\n" +
    "Or if you have family in neighbouring countries like Zimbabwe, you can buy Zimbabwean RGTS Dollars, send them instantly to your family member, who can withdraw them to their Ecocash account. \n" +
    "\n" +
    "All of this can happen within minutes. It’s no longer necessary to go to a bank, stand in a queue and pay high transfer fees.\n"

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
    textViewStart.text = startContent
  }
}
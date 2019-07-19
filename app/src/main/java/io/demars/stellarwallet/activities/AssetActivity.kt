package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.TransactionsAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.WalletHeterogeneousWrapper
import io.demars.stellarwallet.mvvm.WalletViewModel
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_asset.*
import org.jetbrains.anko.doAsync
import timber.log.Timber

class AssetActivity : BaseActivity() {

  companion object {
    const val RC_PAY = 111
    const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    const val ARG_ASSET_ISSUER = "ARG_ASSET_ISSUER"
    fun newInstance(context: Context, assetCode: String, assetIssuer: String): Intent =
       Intent(context, AssetActivity::class.java).apply {
         putExtra(ARG_ASSET_CODE, assetCode)
         putExtra(ARG_ASSET_ISSUER, assetIssuer)
    }
  }

  private var assetCode = ""
  private var assetIssuer = ""

  private lateinit var viewModel: WalletViewModel
  private var transactionsAdapter: TransactionsAdapter? = null
  private var lastOperationsListSize = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_asset)
    checkIntent()
    initUI()
    initButtons()
    initTransactions()
  }

  private fun checkIntent() {
    intent.extras?.let {
      if (it.containsKey(ARG_ASSET_CODE)) {
        assetCode = it.getString(ARG_ASSET_CODE, "")
      } else {
        finishWithToast("Asset code cannot be NULL")
      }

      if (it.containsKey(ARG_ASSET_ISSUER)) {
        assetIssuer = it.getString(ARG_ASSET_ISSUER, "")
      }
    }
  }

  private fun initUI() {
    backButton.setOnClickListener { onBackPressed() }

    val logo = AssetUtils.getLogo(assetCode)
    assetLogo.setImageResource(logo)
    assetLogo.visibility = if (logo == 0) View.GONE else View.VISIBLE

    assetName.text = assetCode

    assetBalance.text = getFormattedBalance()
    availableBalanceView.text = getFormattedAvailableBalance()

    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      viewModel.forceRefresh()
    }
  }

  private fun initButtons() {
    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        container1.visibility = View.VISIBLE
        container2.visibility = View.VISIBLE
        container3.visibility = View.VISIBLE
        container4.visibility = View.VISIBLE

        label1.setText(R.string.buy)
        label2.setText(R.string.set_inflation)

        button1.setOnClickListener { buyXLM() }
        button2.setOnClickListener { openSetInflationActivity() }
      }
      Constants.DMC_ASSET_CODE -> {
        container1.visibility = View.GONE
        container2.visibility = View.VISIBLE
        container3.visibility = View.VISIBLE
        container4.visibility = View.VISIBLE

        label2.setText(R.string.learn)

        button2.setOnClickListener { learnDMC() }
      }
      Constants.ZAR_ASSET_CODE,
      Constants.NGNT_ASSET_CODE -> {
        container1.visibility = View.VISIBLE
        container2.visibility = View.VISIBLE
        container3.visibility = View.VISIBLE
        container4.visibility = View.VISIBLE

        label1.setText(R.string.deposit)
        label2.setText(R.string.withdraw)

        button1.setOnClickListener { openDepositActivity() }
        button2.setOnClickListener { openWithdrawActivity() }
      }
      else -> {
        container1.visibility = View.GONE
        container2.visibility = View.GONE
        container3.visibility = View.VISIBLE
        container4.visibility = View.VISIBLE
      }
    }

    label3.setText(R.string.exchange)
    label4.setText(R.string.pay)

    button3.setOnClickListener { openExchangeActivity() }
    button4.setOnClickListener { openPayToActivity() }
  }

  private fun initTransactions() {
    swipeRefresh.isRefreshing = true

    transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

    transactionsAdapter = TransactionsAdapter(this)
    transactionsRecyclerView.adapter = transactionsAdapter

    ContactsRepositoryImpl(this).getContactsListLiveData(true)
      .observe(this, Observer {
        transactionsAdapter?.setContacts(ArrayList(it.stellarContacts))
        transactionsAdapter?.notifyDataSetChanged()
      })

    viewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
    viewModel.walletViewState(false).observe(this, Observer {
      it?.operationList?.let { operations ->
        doAsync {
          val numberEffects = operations.size
          Timber.d("ACTIVE operations = $numberEffects vs last event $lastOperationsListSize")
          lastOperationsListSize = numberEffects
          it.tradesList?.let { trades ->
            val numberTrades = trades.size
            Timber.d("ACTIVE operations + trades = $numberTrades vs last event $lastOperationsListSize")
            lastOperationsListSize += numberTrades
            val list = WalletHeterogeneousWrapper()
            list.updateOperationsList(assetCode, operations)
            list.updateTradesList(assetCode, trades)
            runOnUiThread {
              assetBalance?.text = getFormattedBalance()
              availableBalanceView?.text = getFormattedAvailableBalance()
              transactionsAdapter?.updateData(list.array)
              swipeRefresh?.isRefreshing = false
            }
          }
        }
      }
    })
  }

  private fun getFormattedBalance(): String =
    StringFormat.truncateDecimalPlaces(
      AccountUtils.getTotalBalance(assetCode), AssetUtils.getMaxDecimals(assetCode))

  private fun getFormattedAvailableBalance(): String =
    getString(R.string.pattern_available, StringFormat.truncateDecimalPlaces(
      AccountUtils.getAvailableBalance(assetCode), AssetUtils.getMaxDecimals(assetCode)))

  private fun openExchangeActivity() {
    startActivity(ExchangeActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun openPayToActivity() {
    startActivityForResult(PayToActivity.newInstance(this, assetCode, assetIssuer), RC_PAY)
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun openReceiveActivity() {
    startActivity(ReceiveActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun openDepositActivity() {
    startActivity(
      DepositActivity.newInstance(this, DepositActivity.Mode.DEPOSIT, assetCode, assetIssuer))
  }

  private fun openWithdrawActivity() {
    startActivity(
    DepositActivity.newInstance(this, DepositActivity.Mode.WITHDRAW, assetCode, assetIssuer))
  }

  private fun learnDMC() {
    toast("To implement")
  }

  private fun buyXLM() {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://bit.ly/XLMCEX")))
  }

  private fun openSetInflationActivity() {
    startActivity(InflationActivity.newInstance(this))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  override fun onResume() {
    super.onResume()
    viewModel.moveToForeGround()
  }

  override fun onStop() {
    super.onStop()
    viewModel.moveToBackground()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}
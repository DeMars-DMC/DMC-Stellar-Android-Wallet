package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.TransactionsAdapter
import io.demars.stellarwallet.interfaces.TransactionsListener
import io.demars.stellarwallet.models.WalletHeterogeneousWrapper
import io.demars.stellarwallet.api.horizon.model.Operation
import io.demars.stellarwallet.api.horizon.model.Trade
import io.demars.stellarwallet.mvvm.WalletViewModel
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.mvvm.contacts.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_asset.*
import org.jetbrains.anko.doAsync
import timber.log.Timber

class AssetActivity : BaseActivity(), TransactionsListener {
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
    button1.setOnClickListener { deposit() }
    button2.setOnClickListener { withdraw() }
    button3.setOnClickListener { exchange() }
    button4.setOnClickListener { pay() }
  }

  private fun initTransactions() {
    swipeRefresh.isRefreshing = true

    transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

    transactionsAdapter = TransactionsAdapter(this, this)
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

  private fun getFormattedBalance(): String = StringFormat.truncateDecimalPlaces(
    AccountUtils.getTotalBalance(assetCode), AssetUtils.getMaxDecimals(assetCode))

  private fun getFormattedAvailableBalance(): String =
    getString(R.string.pattern_available, StringFormat.truncateDecimalPlaces(
      AccountUtils.getAvailableBalance(assetCode), AssetUtils.getMaxDecimals(assetCode)))

  private fun exchange() {
    startActivity(ExchangeActivity.newInstance(this, assetCode, assetIssuer))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun pay() {
    startActivityForResult(PayToActivity.newInstance(this, assetCode, assetIssuer), RC_PAY)
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun deposit() {
    startActivity(ReceiveActivity.newInstance(this, assetCode, assetIssuer))
    overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
  }

  private fun withdraw() {
    startActivity(
      DepositActivity.newInstance(this, DepositActivity.Mode.WITHDRAW, assetCode, assetIssuer))
  }

  override fun onTransactionClicked(transaction: Any) {
    when (transaction) {
      is Operation -> {
        startActivity(TransactionDetailsActivity.newInstance(this,transaction))
      }
      is Trade -> {
        startActivity(TransactionDetailsActivity.newInstance(this,transaction))
      }
    }

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
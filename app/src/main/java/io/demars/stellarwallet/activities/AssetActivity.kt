package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import io.demars.stellarwallet.R
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.adapters.TransactionsAdapter
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.WalletHeterogeneousWrapper
import io.demars.stellarwallet.mvvm.WalletViewModel
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import kotlinx.android.synthetic.main.activity_asset.*
import kotlinx.android.synthetic.main.activity_asset.payButton
import kotlinx.android.synthetic.main.activity_asset.swipeRefresh
import kotlinx.android.synthetic.main.activity_asset.toolbar
import org.jetbrains.anko.doAsync
import timber.log.Timber

class AssetActivity : BaseActivity() {

  companion object {
    const val RC_PAY = 111
    const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    fun newInstance(context: Context, assetCode: String): Intent {
      val intent = Intent(context, AssetActivity::class.java)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  private lateinit var viewModel: WalletViewModel
  private lateinit var assetCode: String

  private var transactionsAdapter: TransactionsAdapter? = null

  private var lastOperationsListSize = 0
  private var appBarOffset = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_asset)
    checkIntent()
    initAppBar()
    initUI()
    initTransactions()
  }

  private fun checkIntent() {
    intent.extras?.getString(ARG_ASSET_CODE)?.let {
      assetCode = it
    } ?: finishWithToast("Asset code cannot be NULL")
  }

  private fun initAppBar() {
    appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->
      appBarOffset = -offset
      val toolbarHeight = toolbar.height
      val scrollRange = appBar.totalScrollRange
      if (appBarOffset in 0..toolbarHeight) {
        assetLogo.alpha = 1f - (appBarOffset * 1.2f) / toolbarHeight
        val balanceScale = 1f - 0.3f * (appBarOffset.toFloat() / toolbarHeight)
        assetBalanceContainer.scaleX = balanceScale
        assetBalanceContainer.scaleY = balanceScale
        assetBalanceContainer.translationY = 0f
      } else {
        assetLogo.alpha = 0f
        assetBalanceContainer.translationY = appBarOffset.toFloat() - toolbarHeight
      }

      val bottomAlpha = 1f - (appBarOffset * 2f) / scrollRange
      buttonsContainer.alpha = bottomAlpha

      walletDivider.scaleX = 1f + 0.2f * (appBarOffset.toFloat() / scrollRange)
    })
  }

  private fun initUI() {
    backButton.setOnClickListener { onBackPressed() }
    payButton.setOnClickListener {
      startActivityForResult(StellarAddressActivity.toPay(this), RC_PAY)
      overridePendingTransition(R.anim.slide_in_start, R.anim.slide_out_start)
    }

    assetLogo.setImageResource(AssetUtils.getLogo(assetCode))
    assetBalance.text = AssetUtils.getBalance(assetCode)
    assetName.text = assetCode

    swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    swipeRefresh.setOnRefreshListener {
      viewModel.forceRefresh()
    }

    buttonsContainer.visibility = View.VISIBLE
    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        leftLabel.setText(R.string.buy)
        rightLabel.setText(R.string.set_inflation)
      }
      Constants.DMC_ASSET_TYPE -> {
        leftLabel.setText(R.string.trade)
        rightLabel.setText(R.string.learn)
      }
      Constants.ZAR_ASSET_TYPE -> {
        initButtonsForCurrencies()
      }
      Constants.NGNT_ASSET_TYPE -> {
        initButtonsForCurrencies()
      }
      else -> {
        buttonsContainer.visibility = View.GONE
      }
    }
  }

  private fun initButtonsForCurrencies() {
    leftLabel.setText(R.string.deposit)
    leftButton.setOnClickListener {
      openDepositScreen(DepositActivity.Mode.DEPOSIT)
    }

    rightLabel.setText(R.string.withdraw)
    rightButton.setOnClickListener {
      openDepositScreen(DepositActivity.Mode.WITHDRAW)
    }
  }

  private fun openDepositScreen(mode: DepositActivity.Mode) {
    if (DmcApp.wallet.isVerified()) {
      startActivity(DepositActivity.newInstance(this, mode, assetCode))
    } else {
      ViewUtils.showToast(this, R.string.withdraw_not_verified)
    }
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
              assetBalance?.text = AssetUtils.getBalance(assetCode)
              transactionsAdapter?.updateData(list.array)
              swipeRefresh?.isRefreshing = false
            }
          }
        }
      }
    })
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
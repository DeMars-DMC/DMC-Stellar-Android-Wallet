package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.ExchangePagerAdapter
import io.demars.stellarwallet.fragments.tabs.TradeTabFragment
import io.demars.stellarwallet.interfaces.OnRefreshOrderBookListener
import io.demars.stellarwallet.interfaces.OnTradeCurrenciesChanged
import io.demars.stellarwallet.interfaces.OnUpdateOrderBook
import io.demars.stellarwallet.interfaces.OnUpdateTradeTab
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.SelectionModel
import io.demars.stellarwallet.remote.Horizon
import kotlinx.android.synthetic.main.activity_exchange.*
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber

class ExchangeActivity : BaseActivity(), ViewPager.OnPageChangeListener, OnTradeCurrenciesChanged, OnRefreshOrderBookListener {
  private lateinit var fragmentAdapter: ExchangePagerAdapter

  private var orderBookListener: OnUpdateOrderBook? = null
  private var tradeTabListener: OnUpdateTradeTab? = null

  private var currentSell: DataAsset? = null
  private var currentBuy: DataAsset? = null

  companion object {
    fun newInstance(context: Context): Intent = Intent(context, ExchangeActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_exchange)

    backButton.setOnClickListener {
      onBackPressed()
    }

    fragmentAdapter = ExchangePagerAdapter(supportFragmentManager)
    viewPager.adapter = fragmentAdapter
    viewPager.offscreenPageLimit = fragmentAdapter.count
    viewPager.addOnPageChangeListener(this)
    tabs.setupWithViewPager(viewPager)
  }

  override fun onAttachFragment(fragment: Fragment?) {
    Timber.d("onAttachFragment %s", fragment.toString())

    if (fragment is OnUpdateOrderBook) {
      orderBookListener = fragment
    }

    if (fragment is OnUpdateTradeTab) {
      tradeTabListener = fragment
    }
  }

  override fun onCurrencyChange(selling: SelectionModel, buying: SelectionModel) {
    currentSell = AssetUtils.toDataAssetFrom(selling)
    currentBuy = AssetUtils.toDataAssetFrom(buying)
    onRefreshOrderBook()
  }

  override fun onRefreshOrderBook() {
    currentSell?.let { sell ->
      currentBuy?.let { buy ->
        loadOrderBook(sell.code, buy.code, sell, buy)
      }
      return
    }
    orderBookListener?.failedToUpdate()
  }

  private fun loadOrderBook(sellingCode: String, buyingCode: String, sell: DataAsset, buy: DataAsset) {
    Timber.d("Loading order book %s %s", sellingCode, buyingCode)
    Horizon.getOrderBook(object : Horizon.OnOrderBookListener {
      override fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
        tradeTabListener?.onLastOrderBookUpdated(asks, bids)
        orderBookListener?.updateOrderBook(sellingCode, buyingCode, asks, bids)
      }

      override fun onFailed(errorMessage: String) {
        Timber.d("failed to load the order book %s", errorMessage)
        orderBookListener?.failedToUpdate()
      }

    }, buy, sell)
  }

  override fun onPageScrollStateChanged(state: Int) {}
  override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
  override fun onPageSelected(position: Int) {
    when (position) {
      0 -> (fragmentAdapter.getItem(position) as TradeTabFragment).updateAvailableBalance()
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_end)
  }
}
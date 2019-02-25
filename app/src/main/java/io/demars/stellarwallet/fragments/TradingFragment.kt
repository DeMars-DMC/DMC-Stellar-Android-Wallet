package io.demars.stellarwallet.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.TradingPagerAdapter
import io.demars.stellarwallet.interfaces.OnRefreshOrderBookListener
import io.demars.stellarwallet.interfaces.OnTradeCurrenciesChanged
import io.demars.stellarwallet.interfaces.OnUpdateOrderBook
import io.demars.stellarwallet.interfaces.OnUpdateTradeTab
import io.demars.stellarwallet.models.AssetUtil
import io.demars.stellarwallet.models.DataAsset
import io.demars.stellarwallet.models.SelectionModel
import io.demars.stellarwallet.remote.Horizon
import kotlinx.android.synthetic.main.fragment_trade.*
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber

class TradingFragment : Fragment(), OnTradeCurrenciesChanged, OnRefreshOrderBookListener {
    private lateinit var fragmentAdapter: TradingPagerAdapter

    private var orderBookListener : OnUpdateOrderBook? = null
    private var tradeTabListener : OnUpdateTradeTab? = null

    private var currentSell : DataAsset? = null
    private var currentBuy : DataAsset? = null

    companion object {
        fun newInstance(): TradingFragment = TradingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_trade, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentAdapter = TradingPagerAdapter(childFragmentManager)
        viewPager.adapter = fragmentAdapter
        viewPager.offscreenPageLimit = fragmentAdapter.count
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
        currentSell =  AssetUtil.toDataAssetFrom(selling)
        currentBuy = AssetUtil.toDataAssetFrom(buying)
        onRefreshOrderBook()
    }

    override fun onRefreshOrderBook() {
        currentSell?.let { sell -> currentBuy?.let { buy ->
            loadOrderBook(sell.code, buy.code, sell, buy) }
            return
        }
        orderBookListener?.failedToUpdate()
    }

    private fun loadOrderBook(sellingCode:String, buyingCode:String, sell : DataAsset, buy : DataAsset) {
        Timber.d("Loading order book %s %s", sellingCode, buyingCode)
        Horizon.getOrderBook(object: Horizon.OnOrderBookListener {
            override fun onOrderBook(asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
                if (asks.isNotEmpty() && bids.isNotEmpty()) {
                    tradeTabListener?.onLastOrderBookUpdated(bids, asks)
                }

                orderBookListener?.updateOrderBook(sellingCode, buyingCode, asks, bids)
            }

            override fun onFailed(errorMessage: String) {
                Timber.d("failed to load the order book %s", errorMessage)
                orderBookListener?.failedToUpdate()
            }

        }, buy, sell)
    }
}
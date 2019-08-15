package io.demars.stellarwallet.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.demars.stellarwallet.fragments.tabs.MyTradesTabFragment
import io.demars.stellarwallet.fragments.tabs.OrderBookTabFragment
import io.demars.stellarwallet.fragments.tabs.TradeTabFragment
import io.demars.stellarwallet.helpers.TradingTabs
import io.demars.stellarwallet.helpers.TradingTabs.*

class ExchangePagerAdapter(fm: FragmentManager, val assetCode: String, val assetIssuer:String) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return TradingTabs.values().size
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            Trade.ordinal -> TradeTabFragment(assetCode, assetIssuer)
            OrderBook.ordinal -> OrderBookTabFragment()
            MyTrades.ordinal -> MyTradesTabFragment()
            else -> throw IllegalStateException("position not valid for" + ExchangePagerAdapter::class.simpleName)
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            Trade.ordinal -> Trade.title
            OrderBook.ordinal -> OrderBook.title
            MyTrades.ordinal -> MyTrades.title
            else -> throw IllegalStateException("position not valid for" + ExchangePagerAdapter::class.simpleName)
        }
    }
}
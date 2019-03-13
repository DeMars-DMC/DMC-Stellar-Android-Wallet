package io.demars.stellarwallet.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.demars.stellarwallet.fragments.tabs.MyTradesTabFragment
import io.demars.stellarwallet.fragments.tabs.OrderBookTabFragment
import io.demars.stellarwallet.fragments.tabs.ExchangeTabFragment
import io.demars.stellarwallet.helpers.TradingTabs
import io.demars.stellarwallet.helpers.TradingTabs.*

class TradingPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return TradingTabs.values().size
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            Trade.ordinal -> ExchangeTabFragment()
            OrderBook.ordinal -> OrderBookTabFragment()
            MyTrades.ordinal -> MyTradesTabFragment()
            else -> throw IllegalStateException("position not valid for" + TradingPagerAdapter::class.simpleName)
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            Trade.ordinal -> Trade.title
            OrderBook.ordinal -> OrderBook.title
            MyTrades.ordinal -> MyTrades.title
            else -> throw IllegalStateException("position not valid for" + TradingPagerAdapter::class.simpleName)
        }
    }
}
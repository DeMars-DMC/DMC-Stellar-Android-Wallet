package io.demars.stellarwallet.fragments.tabs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.OrderBooksAdapter
import io.demars.stellarwallet.interfaces.OnRefreshOrderBookListener
import io.demars.stellarwallet.interfaces.OnUpdateOrderBook
import io.demars.stellarwallet.models.*
import com.brandongogetap.stickyheaders.StickyLayoutManager
import kotlinx.android.synthetic.main.fragment_tab_order_book.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.stellar.sdk.responses.OrderBookResponse
import timber.log.Timber
import java.util.*

class OrderBookTabFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, OnUpdateOrderBook {
    private var orderBooks = mutableListOf<OrderBook>()
    private lateinit var orderBooksAdapter: OrderBooksAdapter
    private lateinit var parentListener: OnRefreshOrderBookListener
    private var buyingAsset : DataAsset? = null
    private var sellingAsset : DataAsset? = null
    private var buyingCode : String? = null
    private var sellingCode : String? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_order_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dividerItemDecoration = DividerItemDecoration(context,
                LinearLayoutManager(context).orientation)
        orderBookRv.addItemDecoration(dividerItemDecoration)
        orderBooksAdapter = OrderBooksAdapter(view.context)
        val layout = StickyLayoutManager(context, orderBooksAdapter)
        orderBookRv.adapter = orderBooksAdapter
        orderBookRv.layoutManager = layout
        swipeRefresh.setOnRefreshListener(this)
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        Timber.d("setUserVisibleHint %s", isVisibleToUser)
        if (isVisibleToUser && orderBookRv != null && buyingCode != null && sellingCode != null) {
            updateList(orderBooks, sellingCode!!, buyingCode!!)
            Timber.d("setUserVisibleHint > Refreshing")
        }
    }

    override fun updateOrderBook(sellingCode: String, buyingCode: String, asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
        Timber.d("updating order book")
        activity?.let {
            if (!it.isFinishing) {
                loadOrderBook(sellingCode, buyingCode, asks, bids)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            parentListener = parentFragment as OnRefreshOrderBookListener
        } catch (e: ClassCastException) {
            Timber.e("the parent must implement: %s", OnRefreshOrderBookListener::class.java.simpleName)
        }
    }

    override fun onRefresh() {
        if (!isAdded || isDetached || !isVisible) {
            Timber.d("onRefreshOrderBook failed fragment not ready")
            return
        }

        parentListener.onRefreshOrderBook()
    }

    private fun loadOrderBook(sellingCode: String, buyingCode: String, asks: Array<OrderBookResponse.Row>, bids: Array<OrderBookResponse.Row>) {
        Timber.d("loadOrderBook")

        orderBooks.clear()
        val orderBooksTitle = OrderBook(type = OrderBookAdapterTypes.TITLE)
        val buyOffer = OrderBookStickyHeader(type = OrderBookAdapterTypes.BUY_HEADER)
        val sellOffer = OrderBookStickyHeader(type = OrderBookAdapterTypes.SELL_HEADER)
        val subHeader = OrderBook(type = OrderBookAdapterTypes.SUBHEADER)
        orderBooks.add(orderBooksTitle)
        orderBooks.add(buyOffer)
        orderBooks.add(subHeader)
        var id = 1
        bids.forEach {
            val item = OrderBook(id, Date(), it.price.toFloat(), it.amount.toFloat() / it.price.toFloat() , it.amount.toFloat(), OrderBookAdapterTypes.ITEM)
            orderBooks.add(item)
            id++
        }

        if (bids.isEmpty()) {
           orderBooks.add(OrderBook(type = OrderBookAdapterTypes.EMPTY))
        }

        orderBooks.add(sellOffer)
        orderBooks.add(subHeader)
        asks.forEach {
            val item = OrderBook(id, Date(), it.price.toFloat(),  it.amount.toFloat(), it.price.toFloat() * it.amount.toFloat(), OrderBookAdapterTypes.ITEM)
            orderBooks.add(item)
            id++

        }

        if (asks.isEmpty()) {
            orderBooks.add(OrderBook(type = OrderBookAdapterTypes.EMPTY))
        }


        Timber.d("loading order book complete items %s", orderBooks.size)

        runOnUiThread {
            updateList(orderBooks, sellingCode, buyingCode)
            empty_view_order_book?.visibility = View.GONE
        }

        if (swipeRefresh != null) {
            swipeRefresh.isRefreshing = false
        }
    }

    override fun updateTradingCurrencies(sellingModel: SelectionModel, buyingModel: SelectionModel) {
        val selling =  AssetUtils.toDataAssetFrom(sellingModel)
        val buying = AssetUtils.toDataAssetFrom(buyingModel)

        buyingAsset = buying
        sellingAsset = selling

        Timber.d("Updating objects in order book")
        updateList(orderBooks, sellingAsset!!.code, buyingAsset!!.code)
    }

    override fun failedToUpdate() {
        runOnUiThread {
            if (swipeRefresh != null) {
                swipeRefresh.isRefreshing = false
                orderBookRv.visibility = View.GONE
                empty_view_order_book.visibility = View.VISIBLE
            }
        }
    }

    private fun updateList(list : MutableList<OrderBook>, sellingCode: String, buyingCode: String) {
        Timber.d("updateTradingCurrencies %s %s", buyingCode, sellingCode)
        orderBookRv?.visibility = View.VISIBLE
        this.buyingCode = buyingCode
        this.sellingCode = sellingCode
        orderBooksAdapter.setOrderBookList(list)
        orderBooksAdapter.setCurrencies(sellingCode, buyingCode)
        orderBooksAdapter.notifyDataSetChanged()
    }
}
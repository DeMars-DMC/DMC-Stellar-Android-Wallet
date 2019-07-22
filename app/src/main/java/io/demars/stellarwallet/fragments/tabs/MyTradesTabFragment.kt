package io.demars.stellarwallet.fragments.tabs

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.MyOffersAdapter
import io.demars.stellarwallet.interfaces.OnDeleteRequest
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.models.Currency
import io.demars.stellarwallet.models.ui.MyOffer
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.AccountUtils
import kotlinx.android.synthetic.main.fragment_tab_my_offers.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.stellar.sdk.responses.OfferResponse
import timber.log.Timber
import java.util.*

class MyTradesTabFragment : Fragment(), OnDeleteRequest, SwipeRefreshLayout.OnRefreshListener {
    private lateinit var appContext : Context
    private var myOffers = mutableListOf<MyOffer>()
    private var offerResponses = mutableListOf<OfferResponse>()

    private lateinit var myOffersAdapter: MyOffersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_my_offers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appContext = view.context.applicationContext
        myOffersRv.layoutManager = LinearLayoutManager(appContext)
        myOffersAdapter = MyOffersAdapter(myOffers, view.context, this)
        myOffersRv.adapter = myOffersAdapter
        val dividerItemDecoration = DividerItemDecoration(context,
                LinearLayoutManager(context).orientation)
        myOffersRv.addItemDecoration(dividerItemDecoration)
        swipeRefresh.setOnRefreshListener(this)
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        onRefresh()
    }

    override fun onRefresh() {
        Timber.d("refreshing offers")
        Horizon.getOffers(object: Horizon.OnOffersListener {
            override fun onOffers(offers: ArrayList<OfferResponse>) {
                if (empty_view == null) return

                if (offers.size == 0) {
                    empty_view.visibility = View.VISIBLE
                } else {
                    empty_view.visibility = View.GONE
                }
                var id = 1
                offerResponses = offers
                myOffers.clear()
                offers.forEach {
                    val buyingCode : String = AssetUtils.getCode(it.buying)!!
                    val currencyBuy = Currency(1, buyingCode, "$buyingCode COIN", 0.0, null)
                    val sellingCode : String = AssetUtils.getCode(it.selling)!!
                    val currencySelling = Currency(2, sellingCode, "$sellingCode COIN", 0.0, null)
                    myOffers.add(MyOffer(it.id.toInt(), Date(), currencySelling, currencyBuy, it.amount.toFloat(), it.amount.toFloat() * it.price.toFloat()))
                    id++
                }
                myOffersAdapter.notifyDataSetChanged()

                setRefreshingFalse()
            }

            override fun onFailed(errorMessage: String) {
                Timber.e(errorMessage)
                setRefreshingFalse()
            }
        })
    }

    private fun setRefreshingFalse(){
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            if (swipeRefresh != null) {
                swipeRefresh.isRefreshing = false
            }
        }
        handler.post(runnable)
    }

    override fun onDialogOpen(offerId: Int) {
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle(getString(R.string.deleteDialogTitle))
                    .setMessage(getString(R.string.deleteDialogText, getText(R.string.offer)))
                    .setPositiveButton(getText(R.string.yes)) { _, _ -> deleteOffer(offerId)}
                    .setNegativeButton(getText(R.string.no)) { dialog, _ -> dialog.dismiss()}
                    .show()
        }
    }

    private fun deleteOffer(offerId: Int) {
        val index = myOffers.indexOf(myOffers.find { offer -> offer.id == offerId })
        if (index == -1) {
            Timber.e("failed to delete, the offer does not exist in the array")
            return
        }
        myOffers.removeAt(index)
        myOffersAdapter.notifyItemRemoved(index)

        if (myOffers.size == 0) {
            empty_view.visibility = View.VISIBLE
        }

        val offer = offerResponses.find {
            it.id.toInt() == offerId
        }

        if (offer != null) {
            val secretSeed = AccountUtils.getSecretSeed(appContext)
            Horizon.deleteOffer(offer.id, secretSeed, offer.selling, offer.buying, offer.price, object: Horizon.OnMarketOfferListener {
                override fun onExecuted() {
                    runOnUiThread {
                        activity?.let {
                            if (!it.isFinishing) {
                                Toast.makeText(appContext, "Order has been deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                }

                override fun onFailed(errorMessage: String) {
                    runOnUiThread {
                        activity?.let {
                            if (!it.isFinishing) {
                                Toast.makeText(appContext, "Failed to delete offer: $errorMessage", Toast.LENGTH_SHORT).show()

                                val snackbar = Snackbar.make(it.findViewById(R.id.rootView),
                                        "failed to delete the offer", Snackbar.LENGTH_INDEFINITE)
                                snackbar.setAction("retry") {
                                    deleteOffer(offerId)
                                }
                                snackbar.show()
                            }
                        }
                    }
                }
            })
        }
    }
}
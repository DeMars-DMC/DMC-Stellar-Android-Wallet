package io.demars.stellarwallet.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.adapters.TransactionsAdapter
import io.demars.stellarwallet.mvvm.WalletViewState
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.runOnUiThread
import timber.log.Timber
import android.graphics.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.activities.*
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.mvvm.WalletViewModel
import io.demars.stellarwallet.vmodels.ContactsRepositoryImpl
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.operations.OperationResponse

class WalletFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {
  private lateinit var viewModel: WalletViewModel
  private var state = WalletState.UNKNOWN
  private var lastOperationsListSize = 0
  private var activeAsset: String = Constants.LUMENS_ASSET_NAME
  private var qrRendered = false

  private var stellarContacts: ArrayList<Contact> = ArrayList()

  companion object {
    fun newInstance(): WalletFragment = WalletFragment()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_wallet, container, false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activity?.let {
      viewModel = ViewModelProviders.of(it).get(WalletViewModel::class.java)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    ContactsRepositoryImpl(activity!!).getContactsListLiveData(true)
      .observe(viewLifecycleOwner, Observer {
        stellarContacts = ArrayList(it.stellarContacts)
        if (walletRecyclerView.adapter is TransactionsAdapter) {
          (walletRecyclerView.adapter as TransactionsAdapter).setContacts(stellarContacts)
          walletRecyclerView.adapter?.notifyDataSetChanged()
        }
      })

    walletRecyclerView.layoutManager = LinearLayoutManager(context)
    walletRecyclerView.adapter = createAdapter()

    updateState(WalletState.UPDATING)

    lastOperationsListSize = 0

    initViewModels()

    swipeRefresh.setOnRefreshListener(this)
    swipeRefresh.setColorSchemeResources(R.color.colorAccent)

    receiveButton.setOnClickListener {
      receiveButton.isEnabled = false
      activity?.let { activityContext ->
        startActivity(Intent(activityContext, ReceiveActivity::class.java))
        activityContext.overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
      }
    }

    payButton.setOnClickListener {
      payButton.isEnabled = false
      activity?.let { activityContext ->
        startActivity(StellarAddressActivity.toPay(it.context))
        activityContext.overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
      }
    }

    activity?.let {
      fetching_wallet_image.setColorFilter(ContextCompat.getColor(it, R.color.colorPaleSky), PorterDuff.Mode.SRC_ATOP)
    }
  }

  override fun onRefresh() {
    updateState(WalletState.UPDATING)
    activity?.let {
      if (!it.isFinishing) {
        viewModel.forceRefresh()
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    qrRendered = false
  }

  private fun generateQRCode(data: String, imageView: ImageView, size: Int) {
    val barcodeEncoder = BarcodeEncoder()
    val bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)

    activity?.let {
      imageView.setImageBitmap(tintImage(bitmap, ContextCompat.getColor(it, R.color.colorPaleSky)))
    }
  }

  private fun initAddressCopyButton(secretSeed: String) {
    publicSeedTextView.text = secretSeed
    publicSeedCopyButton.setOnClickListener { copyAddressToClipBoard(secretSeed) }
  }

  private fun copyAddressToClipBoard(data: String) {
    activity?.let {
      val clipboard = (it.getSystemService(Context.CLIPBOARD_SERVICE)) as ClipboardManager
      val clip = ClipData.newPlainText("DMC Address", data)
      clipboard.primaryClip = clip

      Toast.makeText(it, getString(R.string.address_copied_message), Toast.LENGTH_LONG).show()
    }
  }

  private fun tintImage(bitmap: Bitmap, color: Int): Bitmap {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SCREEN)
    val bitmapResult = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapResult)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return bitmapResult
  }

  private fun createListWrapper(): WalletHeterogeneousWrapper {
    val time = System.currentTimeMillis()
    val list = WalletHeterogeneousWrapper()
    list.array.add(TotalBalance(WalletState.UPDATING, "Refreshing Wallet", "", "Updating..."))
    list.array.add(AvailableBalance("XLM", null, "-1"))
    list.array.add(Pair("Activity", "Amount"))
    val delta = System.currentTimeMillis() - time
    Timber.d("createListWrapper(), it took: $delta ms")
    return list
  }

  override fun onResume() {
    super.onResume()
    if (state == WalletState.ACTIVE) {
      receiveButton.isEnabled = true
      payButton.isEnabled = true
    }
    viewModel.moveToForeGround()

  }

  override fun onStop() {
    super.onStop()
    viewModel.moveToBackground()
  }

  //region User Interface

  private fun initViewModels() {
    Timber.d("initViewModels called")
    viewModel.walletViewState(false).observe(this, Observer {
      it?.let { that ->
        Timber.d("observed = ${it.status}")
        when (that.status) {
          WalletViewState.AccountStatus.ACTIVE -> updateState(WalletState.ACTIVE, it)
          WalletViewState.AccountStatus.ERROR -> updateState(WalletState.ERROR, it)
          WalletViewState.AccountStatus.UNFUNDED -> updateState(WalletState.NOT_FUNDED, it)
          else -> {
            //nothing
          }
        }
      }
    })
  }

  private fun updateState(newState: WalletState, viewState: WalletViewState? = null) {
    Timber.d("updating new state={$newState}")
    var listWrapper: WalletHeterogeneousWrapper = createListWrapper()
    doAsync {
      when (newState) {
        WalletState.ACTIVE -> {
          noTransactionsTextView.visibility = View.GONE
          viewState?.operationList?.let { operations ->
            val numberEffects = operations.size
            Timber.d("ACTIVE operations = $numberEffects vs last event $lastOperationsListSize")
            lastOperationsListSize = numberEffects
            viewState.tradesList?.let { trades ->
              val numberTrades = trades.size
              Timber.d("ACTIVE operations + trades = $numberTrades vs last event $lastOperationsListSize")
              lastOperationsListSize += numberTrades
              listWrapper = createListWithData(operations, trades, viewState.activeAssetCode,
                viewState.availableBalance!!, viewState.totalBalance!!)
            }
          }
        }
        WalletState.UPDATING -> {
          //we had to hide the list of effects :(
        }
        WalletState.ERROR -> {
        }
        WalletState.NOT_FUNDED -> {
        }
        else -> {
          //nothing
        }
      }
      runOnUiThread {
        activity?.let {
          if (!it.isFinishing && walletRecyclerView != null) {
            if (!qrRendered && viewState != null && qrCode != null) {
              generateQRCode(viewState.accountId!!, qrCode, 500)
              initAddressCopyButton(viewState.accountId!!)

              qrRendered = true
            }

            (walletRecyclerView.adapter as TransactionsAdapter)
              .updateData(listWrapper.array)
            updatePlaceHolders(newState)
          }
        }
      }
      state = newState
      viewState?.let {
        activeAsset = it.activeAssetCode
      }
    }
  }

  /**
   * run this in the ui thread
   */
  private fun updatePlaceHolders(newState: WalletState) {
    activity?.let {
      if (!it.isFinishing) {
        when (newState) {
          WalletState.NOT_FUNDED -> {
            payButton.isEnabled = false
            receiveButton.isEnabled = true
            swipeRefresh.isRefreshing = false
            noTransactionsTextView.visibility = View.GONE
            fetchingState.visibility = View.GONE
            fundingState.visibility = View.VISIBLE
            updateOpenAccountButton()
          }
          WalletState.ERROR -> {
            noTransactionsTextView.visibility = View.GONE
            payButton.isEnabled = false
            receiveButton.isEnabled = false
            swipeRefresh.isRefreshing = false
            fetchingState.visibility = View.VISIBLE
            fundingState.visibility = View.GONE
          }
          WalletState.UPDATING -> {
            noTransactionsTextView.visibility = View.GONE
            payButton.isEnabled = false
            receiveButton.isEnabled = false
            swipeRefresh.isRefreshing = false
            fetchingState.visibility = View.VISIBLE
            fundingState.visibility = View.GONE
          }
          WalletState.ACTIVE -> {
            payButton.isEnabled = true
            receiveButton.isEnabled = true
            swipeRefresh.isRefreshing = false
            noTransactionsTextView.visibility = View.GONE
            fetchingState.visibility = View.GONE
            fundingState.visibility = View.GONE
          }
          else -> {
            // nothing
          }
        }
      }
    }
  }

  private fun updateOpenAccountButton() {
    if (DmcApp.wallet.isRegistered()) {
      openAccountButton?.visibility = View.GONE
    } else {
      openAccountButton?.visibility = View.VISIBLE
      openAccountButton?.setOnClickListener {
        startActivity(CreateUserActivity.newInstance(context!!))
      }
    }
  }

  /**
   * It will reset the array list.
   */
  private fun createAdapter(): TransactionsAdapter {
    val adapter = TransactionsAdapter(activity!!)
    adapter.setContacts(stellarContacts)
    return adapter
  }

  private fun createListWithData(operations: ArrayList<Pair<OperationResponse, String?>>, trades: ArrayList<TradeResponse>,
                                 activeAsset: String, availableBalance: AvailableBalance,
                                 totalAssetBalance: TotalBalance): WalletHeterogeneousWrapper {
    val time = System.currentTimeMillis()
    val list = createListWrapper()
    list.updateOperationsList(activeAsset, operations)
    list.updateTradesList(activeAsset, trades)
    val delta = System.currentTimeMillis() - time
    Timber.d("createListWithData(list{${operations.size}}, $activeAsset), it took: $delta ms")
    return list
  }

  //endregion

}

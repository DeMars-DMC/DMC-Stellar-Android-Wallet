package io.demars.stellarwallet.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssetListener
import io.demars.stellarwallet.utils.StringFormat
import com.squareup.picasso.Picasso
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.models.ui.DmcAsset
import io.demars.stellarwallet.utils.AssetUtils
import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair
import java.util.*
import kotlin.collections.ArrayList

class AssetsAdapter(private var listener: AssetListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  companion object {
    const val TYPE_ASSET_ADDED = 0
    const val TYPE_HEADER = 1
    const val TYPE_ASSET_NOT_ADDED = 2
    const val TYPE_FOOTER = 3
  }

  private var items = ArrayList<Any>()
  private var isCustomizing = false

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)

    return when (viewType) {
      TYPE_ASSET_ADDED -> {
        val v = inflater.inflate(R.layout.item_asset, parent, false)
        AssetViewHolder(v)
      }
      TYPE_ASSET_NOT_ADDED -> {
        val v = inflater.inflate(R.layout.item_asset, parent, false)
        SupportedAssetViewHolder(v)
      }
      TYPE_FOOTER -> {
        val v = inflater.inflate(R.layout.item_asset_footer, parent, false)
        AssetFooterViewHolder(v)
      }
      else -> {
        val v = inflater.inflate(R.layout.item_asset_header, parent, false)
        AssetHeaderViewHolder(v)
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    val item = items[position]
    return when {
      item is DmcAsset && item.isAdded -> TYPE_ASSET_ADDED
      item is DmcAsset -> TYPE_ASSET_NOT_ADDED
      position == itemCount - 1 -> TYPE_FOOTER
      else -> TYPE_HEADER
    }
  }

  override fun getItemCount(): Int {
    return items.size
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder.itemViewType) {
      TYPE_ASSET_ADDED -> {
        val vh = holder as AssetViewHolder
        displayAsset(vh, position)
      }
      TYPE_ASSET_NOT_ADDED -> {
        val vh = holder as SupportedAssetViewHolder
        displaySupportedAsset(vh, position)
      }
      TYPE_FOOTER -> {
        val vh = holder as AssetFooterViewHolder
        displayAssetFooter(vh)
      }
      else -> {
        val vh = holder as AssetHeaderViewHolder
        displayAssetHeader(vh, position)
      }
    }
  }


  fun refreshAdapter() {
    items.clear()
    notifyDataSetChanged()

    updateAssets()
  }

  fun enableCustomization(enable: Boolean) {
    isCustomizing = enable
    updateAssets()
  }

  private fun updateAssets() {
    val isRefreshing = items.isEmpty()
    val balances = DmcApp.wallet.getBalances()
    val xlm = DmcAsset(Constants.LUMENS_ASSET_CODE, Constants.LUMENS_IMAGE_RES,
      "", Constants.LUMENS_ASSET_CODE, "", true, null)

    val dmc = DmcAsset(Constants.DMC_ASSET_CODE, Constants.DMC_IMAGE_RES,
      Constants.DMC_ASSET_ISSUER, Constants.DMC_ASSET_NAME, "", false, null)

    val eth = DmcAsset(Constants.ETH_ASSET_CODE, Constants.ETH_IMAGE_RES,
      Constants.ETH_ASSET_ISSUER, Constants.ETH_ASSET_NAME, "", false, null)

    val btc = DmcAsset(Constants.BTC_ASSET_CODE, Constants.BTC_IMAGE_RES,
      Constants.BTC_ASSET_ISSUER, Constants.BTC_ASSET_NAME, "", false, null)

    val zar = DmcAsset(Constants.ZAR_ASSET_CODE, Constants.ZAR_IMAGE_RES,
      Constants.ZAR_ASSET_ISSUER, Constants.ZAR_ASSET_NAME, "", false, null)

    val ngnt = DmcAsset(Constants.NGNT_ASSET_CODE, Constants.NGNT_IMAGE_RES,
      Constants.NGNT_ASSET_ISSUER, Constants.NGNT_ASSET_NAME, "", false, null)

    val eurt = DmcAsset(Constants.EURT_ASSET_CODE, Constants.EURT_IMAGE_RES,
      Constants.EURT_ASSET_ISSUER, Constants.EURT_ASSET_NAME, "", false, null)

    val xof = DmcAsset(Constants.XOF_ASSET_CODE, Constants.XOF_IMAGE_RES,
      Constants.XOF_ASSET_ISSUER, Constants.XOF_ASSET_NAME, "", false, null)

    val xaf = DmcAsset(Constants.XAF_ASSET_CODE, Constants.XAF_IMAGE_RES,
      Constants.XAF_ASSET_ISSUER, Constants.XAF_ASSET_NAME, "", false, null)

    val currencies = ArrayList<DmcAsset>()
    val customs = ArrayList<DmcAsset>()

    // We will remove assets from here if already added to the wallet
    val supported = ArrayList<DmcAsset>().apply {
      add(ngnt)
      add(btc)
      add(zar)
      add(eth)
      add(dmc)
      add(eurt)
      add(xof)
      add(xaf)
    }

    balances.forEach {
      when {
        it.assetType == Constants.LUMENS_ASSET_TYPE -> {
          xlm.amount = it.balance
        }
        it.assetCode.equals(Constants.BTC_ASSET_CODE, true) -> {
          btc.amount = it.balance
          btc.asset = it.asset
          btc.isAdded = true
          currencies.add(btc)
          supported.remove(btc)
        }
        it.assetCode.equals(Constants.NGNT_ASSET_CODE, true) -> {
          ngnt.amount = it.balance
          ngnt.asset = it.asset
          ngnt.isAdded = true
          currencies.add(ngnt)
          supported.remove(ngnt)
        }
        it.assetCode.equals(Constants.ZAR_ASSET_CODE, true) -> {
          zar.amount = it.balance
          zar.asset = it.asset
          zar.isAdded = true
          currencies.add(zar)
          supported.remove(zar)
        }
        it.assetCode.equals(Constants.ETH_ASSET_CODE, true) -> {
          eth.amount = it.balance
          eth.asset = it.asset
          eth.isAdded = true
          currencies.add(eth)
          supported.remove(eth)
        }
        it.assetCode.equals(Constants.DMC_ASSET_CODE, true) -> {
          dmc.amount = it.balance
          dmc.asset = it.asset
          dmc.isAdded = true
          currencies.add(dmc)
          supported.remove(dmc)
        }
        it.assetCode.equals(Constants.EURT_ASSET_CODE, true) -> {
          eurt.amount = it.balance
          eurt.asset = it.asset
          eurt.isAdded = true
          currencies.add(eurt)
          supported.remove(eurt)
        }
        it.assetCode.equals(Constants.XOF_ASSET_CODE, true) -> {
          xof.amount = it.balance
          xof.asset = it.asset
          xof.isAdded = true
          currencies.add(xof)
          supported.remove(xof)
        }
        it.assetCode.equals(Constants.XAF_ASSET_CODE, true) -> {
          xaf.amount = it.balance
          xaf.asset = it.asset
          xaf.isAdded = true
          currencies.add(xaf)
          supported.remove(xaf)
        }
        else -> {
          val custom = DmcAsset(it.assetCode.toLowerCase(Locale.getDefault()), 0,
            it.assetIssuer.accountId, it.assetCode, it.balance, true, it.asset)
          customs.add(custom)
        }
      }
    }

    items = items.apply {
      var index = 0
      if (isRefreshing) {
        currencies.addAll(customs)
        currencies.add(xlm)
        if (currencies.isNotEmpty()) {
          addAll(currencies)
          notifyItemRangeInserted(index, currencies.size)
          index += currencies.size
        }


        if (isCustomizing) {
          if (supported.isNotEmpty()) {
            addAll(supported)
            notifyItemRangeInserted(index, supported.size)
            index += supported.size
          }
        }

        add("Footer")
        notifyItemInserted(index)
      } else {
        currencies.addAll(customs)
        currencies.add(xlm)
        if (currencies.isNotEmpty()) {
          index += if (isCustomizing) {
            notifyItemRangeChanged(index, currencies.size)
            currencies.size
          } else {
            notifyItemRangeChanged(index, currencies.size)
            currencies.size
          }
        }

        if (supported.isNotEmpty()) {
          if (isCustomizing) {
            addAll(index, supported)
            notifyItemRangeInserted(index, supported.size)
          } else {
            removeAll(supported)
            notifyItemRangeRemoved(index, supported.size)
          }
        }

        notifyItemChanged(size - 1)
      }
    }
  }
  //region View Holders

  class AssetHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val title: TextView = v.findViewById(R.id.titleText)
  }

  class AssetFooterViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val tradeButton: View = v.findViewById(R.id.tradeButton)
    val button: View = v.findViewById(R.id.footerButton)
    val title: TextView = v.findViewById(R.id.footerTitle)
    val icon: ImageView = v.findViewById(R.id.footerIcon)
  }

  class AssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
    val rightIcon: ImageView = v.findViewById(R.id.rightIcon)
  }

  class SupportedAssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
    val rightIcon: ImageView = v.findViewById(R.id.rightIcon)
  }

  //endregion

  //region Bind View Holders
  private fun displayAssetHeader(viewHolder: AssetHeaderViewHolder, position: Int) {
    val titleText = items[position] as String
    viewHolder.title.text = titleText
  }

  private fun displayAssetFooter(viewHolder: AssetFooterViewHolder) {
    if (isCustomizing) {
      viewHolder.tradeButton.visibility = View.GONE

      viewHolder.title.setText(R.string.add_custom_asset)
      viewHolder.title.setTextColor(ContextCompat.getColor(
        viewHolder.title.context, R.color.colorGreen))
      viewHolder.icon.setImageResource(R.drawable.ic_add)
      viewHolder.button.setBackgroundResource(R.drawable.background_card_transparent_green)
      viewHolder.button.setOnClickListener {
        listener.addCustomAsset()
      }
    } else {
      viewHolder.tradeButton.visibility = View.VISIBLE
      viewHolder.tradeButton.setOnClickListener {
        listener.tradeAssets()
      }

      viewHolder.title.setText(R.string.customize_wallet)
      viewHolder.title.setTextColor(ContextCompat.getColor(
        viewHolder.title.context, R.color.colorAccent))
      viewHolder.icon.setImageResource(R.drawable.ic_edit_accent)
      viewHolder.button.setBackgroundResource(R.drawable.background_card_transparent_accent)
      viewHolder.button.setOnClickListener {
        listener.customizeWallet()
      }
    }
  }

  private fun displayAsset(holder: AssetViewHolder, position: Int) {
    val asset = items[position] as DmcAsset
    val assetCode = asset.code.toUpperCase()

    holder.assetCode.text = assetCode
    holder.assetBalance.text = StringFormat.truncateDecimalPlaces(asset.amount,
      AssetUtils.getMaxDecimals(assetCode))

    if (isCustomizing) {
      if (asset.amount!!.toDouble() == 0.0 &&
        assetCode != Constants.LUMENS_ASSET_CODE) {
        holder.rightIcon.visibility = View.VISIBLE
        holder.rightIcon.setImageResource(R.drawable.ic_remove)
        holder.rightIcon.setPadding(holder.rightIcon.context.resources
          ?.getDimensionPixelSize(R.dimen.padding_medium) ?: 0)
        holder.rightIcon.setOnClickListener {
          listener.changeTrustline(asset.asset!!, true)
        }
      } else {
        holder.rightIcon.visibility = View.GONE
      }
    } else {
      holder.rightIcon.visibility = View.VISIBLE
      holder.rightIcon.setImageResource(R.drawable.ic_chevron_right_transparent)
      holder.rightIcon.setPadding(0)
      holder.rightIcon.setOnClickListener(null)
    }

    // Image
    when {
      asset.image != 0 -> loadAssetImage(asset.image, holder.assetImage)
      else -> loadAssetImage(Constants.CUSTOM_ASSET_IMAGE_RES, holder.assetImage)
    }

    // Transition names
    ViewCompat.setTransitionName(holder.assetImage, "${assetCode}IMAGE")
    ViewCompat.setTransitionName(holder.assetCode, "${assetCode}CODE")
    ViewCompat.setTransitionName(holder.assetBalance, "${assetCode}BALANCE")

    if (isCustomizing) {
      holder.itemView.setOnClickListener(null)
    } else {
      holder.itemView.setOnClickListener {
        listener.assetSelected(assetCode, asset.issuer)
      }
    }
  }

  private fun displaySupportedAsset(holder: SupportedAssetViewHolder, position: Int) {
    val asset = items[position] as DmcAsset
    val assetCode = asset.code.toUpperCase()
    val trustLineAsset = Asset.createNonNativeAsset(assetCode, KeyPair.fromAccountId(asset.issuer))

    holder.assetCode.text = assetCode
    holder.assetBalance.text = asset.name
    holder.assetBalance.visibility = View.VISIBLE

    loadAssetImage(asset.image, holder.assetImage)

    holder.rightIcon.visibility = View.VISIBLE
    holder.rightIcon.setImageResource(R.drawable.ic_add)
    holder.rightIcon.setPadding(holder.rightIcon.context.resources
      ?.getDimensionPixelSize(R.dimen.padding_medium) ?: 0)
    holder.rightIcon.setOnClickListener {
      listener.changeTrustline(trustLineAsset, false)
    }
  }

  private fun loadAssetImage(image: Int, view: ImageView) {
    Picasso.get()
      .load(image)
      .resize(256, 256)
      .onlyScaleDown()
      .into(view)
  }
  //endregion
}

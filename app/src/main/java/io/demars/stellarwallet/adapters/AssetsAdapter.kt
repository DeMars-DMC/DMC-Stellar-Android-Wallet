package io.demars.stellarwallet.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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

  fun updateAdapter() {
    updateAssets()
  }

  private fun updateAssets() {
    val initial = items.isEmpty()
    val balances = DmcApp.wallet.getBalances()
    val xlm = DmcAsset(Constants.LUMENS_ASSET_CODE, Constants.LUMENS_IMAGE_RES,
      "", Constants.LUMENS_ASSET_CODE, "", true, null)

    val dmc = DmcAsset(Constants.DMC_ASSET_TYPE, Constants.DMC_IMAGE_RES,
      Constants.DMC_ASSET_ISSUER, Constants.DMC_ASSET_NAME, "", false, null)

    val zar = DmcAsset(Constants.ZAR_ASSET_TYPE, Constants.ZAR_IMAGE_RES,
      Constants.ZAR_ASSET_ISSUER, Constants.ZAR_ASSET_NAME, "", false, null)

    val ngnt = DmcAsset(Constants.NGNT_ASSET_TYPE, Constants.NGNT_IMAGE_RES,
      Constants.NGNT_ASSET_ISSUER, Constants.NGNT_ASSET_NAME, "", false, null)

    val currencies = ArrayList<DmcAsset>()
    val customs = ArrayList<DmcAsset>()

    val cryptos = ArrayList<DmcAsset>().apply {
      add(dmc)
      add(xlm)
    }

    // We will remove assets from here if already added to the wallet
    val supported = ArrayList<DmcAsset>().apply {
      add(zar)
      add(ngnt)
    }

    balances.forEach {
      when {
        it.assetType == Constants.LUMENS_ASSET_TYPE -> {
          xlm.amount = it.balance
        }
        it.assetCode.equals(Constants.DMC_ASSET_TYPE, true) -> {
          dmc.amount = it.balance
          dmc.asset = it.asset
          dmc.isAdded = true
        }
        it.assetCode.equals(Constants.ZAR_ASSET_TYPE, true) -> {
          zar.amount = it.balance
          zar.asset = it.asset
          zar.isAdded = true
          currencies.add(zar)
          supported.remove(zar)
        }
        it.assetCode.equals(Constants.NGNT_ASSET_TYPE, true) -> {
          ngnt.amount = it.balance
          ngnt.asset = it.asset
          ngnt.isAdded = true
          currencies.add(ngnt)
          supported.remove(ngnt)
        }
        else -> {
          val custom = DmcAsset(it.assetCode.toLowerCase(), 0,
            it.assetIssuer.accountId, it.assetCode, it.balance, true, it.asset)
          customs.add(custom)
        }
      }
    }

    items = items.apply {
      var index = 0
      if (initial) {
        if (currencies.isNotEmpty()) {
          addAll(currencies)
          notifyItemRangeInserted(index, currencies.size)
          index += currencies.size
        }

        if (customs.isNotEmpty()) {
          addAll(customs)
          notifyItemRangeInserted(index, customs.size)
          index += customs.size
        }

        addAll(cryptos)
        add("Footer")
        notifyItemRangeInserted(index,cryptos.size + 1)
      } else {
        if (currencies.isNotEmpty()) {
          index += if (isCustomizing) {
            add(index, "Currencies")
            notifyItemInserted(index)
            currencies.size + 1
          } else {
            remove("Currencies")
            notifyItemRemoved(index)
            currencies.size
          }
        }

        if (customs.isNotEmpty()) {
          index += if (isCustomizing) {
            add(index, "Custom assets")
            notifyItemInserted(index)
            customs.size + 1
          } else {
            remove("Custom assets")
            notifyItemRemoved(index)
            customs.size
          }
        }

        index += if (isCustomizing) {
          add(index, "Cryptos")
          notifyItemInserted(index)
          cryptos.size + 1
        } else {
          remove("Cryptos")
          notifyItemRemoved(index)
          cryptos.size
        }

        if (supported.isNotEmpty()) {
          index += if (isCustomizing) {
            add(index, "Add assets")
            notifyItemInserted(index)
            1
          } else {
            remove("Add assets")
            notifyItemRemoved(index)
            0
          }

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
    val button: View = v.findViewById(R.id.footerButton)
    val title: TextView = v.findViewById(R.id.footerTitle)
    val icon: ImageView = v.findViewById(R.id.footerIcon)
  }

  class AssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val defaultImage: TextView = v.findViewById(R.id.defaultImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
//    val leftButton: TextView = v.findViewById(R.id.leftButton)
//    val rightButton: TextView = v.findViewById(R.id.rightButton)
  }

  class SupportedAssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val defaultImage: TextView = v.findViewById(R.id.defaultImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
//    val leftButton: TextView = v.findViewById(R.id.leftButton)
//    val rightButton: TextView = v.findViewById(R.id.rightButton)
  }

  //endregion

  //region Bind View Holders
  private fun displayAssetHeader(viewHolder: AssetHeaderViewHolder, position: Int) {
    val titleText = items[position] as String
    viewHolder.title.text = titleText
  }

  private fun displayAssetFooter(viewHolder: AssetFooterViewHolder) {
    if (isCustomizing) {
      viewHolder.title.setText(R.string.add_custom_asset)
      viewHolder.title.setTextColor(ContextCompat.getColor(
        viewHolder.title.context, R.color.colorGreen))
      viewHolder.icon.setImageResource(R.drawable.ic_add_green)
      viewHolder.button.setBackgroundResource(R.drawable.background_card_transparent_green)
      viewHolder.button.setOnClickListener {
        isCustomizing = false
        updateAdapter()
//        listener.addCustomAsset()
      }
    } else {
      viewHolder.title.setText(R.string.customize_wallet)
      viewHolder.title.setTextColor(ContextCompat.getColor(
        viewHolder.title.context, R.color.colorAccent))
      viewHolder.icon.setImageResource(R.drawable.ic_edit_accent)
      viewHolder.button.setBackgroundResource(R.drawable.background_card_transparent_accent)
      viewHolder.button.setOnClickListener {
        isCustomizing = true
        updateAdapter()
      }
    }
  }

  private fun displayAsset(holder: AssetViewHolder, position: Int) {
    val asset = items[position] as DmcAsset
    val assetCode = asset.code.toUpperCase()

    holder.assetCode.text = assetCode
    holder.assetBalance.text = StringFormat.truncateDecimalPlaces(asset.amount,
      AssetUtils.getMaxDecimals(assetCode))

    // Buttons
//    when (assetCode) {
//      Constants.LUMENS_ASSET_CODE -> {
//        holder.leftButton.visibility = View.VISIBLE
//        holder.leftButton.setText(R.string.buy)
//
//        holder.rightButton.visibility = View.VISIBLE
//        holder.rightButton.setText(R.string.set_inflation)
//      }
//      Constants.DMC_ASSET_TYPE -> {
//        holder.leftButton.visibility = View.VISIBLE
//        holder.leftButton.setText(R.string.trade)
//
//        holder.rightButton.visibility = View.VISIBLE
//        holder.rightButton.setText(R.string.learn)
//      }
//      Constants.ZAR_ASSET_TYPE -> {
//        holder.leftButton.visibility = View.VISIBLE
//        holder.leftButton.setText(R.string.deposit)
//
//        val hasAvailableZAR = AccountUtils.hasAvailableAssets(asset.code)
//
//        holder.rightButton.visibility = if (hasAvailableZAR) View.VISIBLE else View.GONE
//        holder.rightButton.setText(R.string.withdraw)
//      }
//      Constants.NGNT_ASSET_TYPE -> {
//        holder.leftButton.visibility = View.VISIBLE
//        holder.leftButton.setText(R.string.deposit)
//
//        val hasNGNT = AccountUtils.hasAvailableAssets(asset.code)
//
//        holder.rightButton.visibility = if (hasNGNT) View.VISIBLE else View.GONE
//        holder.rightButton.setText(R.string.withdraw)
//      }
//      else -> {
//        holder.leftButton.visibility = View.GONE
//        holder.rightButton.visibility = View.GONE
//      }
//    }
//
//    holder.leftButton.setOnClickListener {
//      listener.deposit(assetCode)
//    }
//
//    holder.rightButton.setOnClickListener {
//      listener.withdraw(assetCode)
//    }
//
//    when {
//      asset.amount!!.toDouble() == 0.0 &&
//        assetCode != Constants.LUMENS_ASSET_CODE &&
//        assetCode != Constants.DMC_ASSET_TYPE -> {
//        holder.rightButton.visibility = View.VISIBLE
//        holder.rightButton.setText(R.string.remove_asset)
//        holder.rightButton.setTextColor(
//          ContextCompat.getColor(context, R.color.colorTerracotta))
//        holder.rightButton.setOnClickListener {
//          listener.changeTrustline(asset.asset!!, true)
//        }
//      }
//      else -> {
//        holder.rightButton.setTextColor(
//          ContextCompat.getColor(context, R.color.colorAccent))
//      }
//    }

    holder.defaultImage.visibility = View.GONE

    // Image
    when {
      assetCode == Constants.ZAR_ASSET_TYPE ->
        loadAssetImage(Constants.ZAR_IMAGE_RES, holder.assetImage)
      assetCode == Constants.DMC_ASSET_TYPE ->
        loadAssetImage(Constants.DMC_IMAGE_RES, holder.assetImage)
      assetCode == Constants.NGNT_ASSET_TYPE ->
        loadAssetImage(Constants.NGNT_IMAGE_RES, holder.assetImage)
      asset.image != 0 -> {
        loadAssetImage(asset.image, holder.assetImage)
      }
      else -> {
        holder.defaultImage.text = assetCode
        holder.defaultImage.visibility = View.VISIBLE
        holder.assetImage.setImageDrawable(null)
      }
    }

    // Transition names
    ViewCompat.setTransitionName(holder.assetImage, "${assetCode}IMAGE")
    ViewCompat.setTransitionName(holder.assetCode, "${assetCode}CODE")
    ViewCompat.setTransitionName(holder.assetBalance, "${assetCode}BALANCE")

    holder.itemView.setOnClickListener {
      listener.assetSelected(assetCode)
    }
  }

  private fun displaySupportedAsset(viewHolder: SupportedAssetViewHolder, position: Int) {
    val asset = items[position] as DmcAsset
    val assetCode = asset.code.toUpperCase()
    val trustLineAsset = Asset.createNonNativeAsset(assetCode, KeyPair.fromAccountId(asset.issuer))

    viewHolder.defaultImage.visibility = View.GONE
//    viewHolder.leftButton.visibility = View.GONE

    viewHolder.assetCode.text = assetCode
    viewHolder.assetBalance.text = asset.name
    viewHolder.assetBalance.visibility = View.VISIBLE

    loadAssetImage(asset.image, viewHolder.assetImage)

//    viewHolder.rightButton.visibility = View.VISIBLE
//    viewHolder.rightButton.setText(R.string.add_asset)
//    viewHolder.rightButton.setTextColor(
//      ContextCompat.getColor(context, R.color.colorGreen))
//    viewHolder.rightButton.setOnClickListener {
//      listener.changeTrustline(trustLineAsset, false)
//    }
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

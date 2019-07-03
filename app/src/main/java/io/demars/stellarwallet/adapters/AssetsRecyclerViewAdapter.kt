package io.demars.stellarwallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.AssetUtils
import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair

class AssetsRecyclerViewAdapter(private var context: Context,
                                private var listener: AssetListener,
                                private var items: ArrayList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  companion object {
    const val TYPE_ASSET = 0
    const val TYPE_HEADER = 1
    const val TYPE_SUPPORTED_ASSET = 2
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)

    return when (viewType) {
      TYPE_ASSET -> {
        val v = inflater.inflate(R.layout.item_asset, parent, false)
        AssetViewHolder(v)
      }
      TYPE_SUPPORTED_ASSET -> {
        val v = inflater.inflate(R.layout.item_asset, parent, false)
        SupportedAssetViewHolder(v)
      }
      else -> {
        val v = inflater.inflate(R.layout.item_asset_header, parent, false)
        AssetHeaderViewHolder(v)
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when {
      items[position] is SupportedAsset && (items[position] as
        SupportedAsset).type == SupportedAssetType.ADDED -> TYPE_ASSET
      items[position] is SupportedAsset -> TYPE_SUPPORTED_ASSET
      else -> TYPE_HEADER
    }
  }

  override fun getItemCount(): Int {
    return items.size
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder.itemViewType) {
      TYPE_ASSET -> {
        val vh = holder as AssetViewHolder
        displayAsset(vh, position)
      }
      TYPE_SUPPORTED_ASSET -> {
        val vh = holder as SupportedAssetViewHolder
        displaySupportedAsset(vh, position)
      }
      else -> {
        val vh = holder as AssetHeaderViewHolder
        displayAssetHeader(vh, position)
      }
    }
  }

  //region View Holders

  class AssetHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val title: TextView = v.findViewById(R.id.titleText)
  }

  class AssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val defaultImage: TextView = v.findViewById(R.id.defaultImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
    val leftButton: Button = v.findViewById(R.id.leftButton)
    val rightButton: Button = v.findViewById(R.id.rightButton)
  }

  class SupportedAssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val defaultImage: TextView = v.findViewById(R.id.defaultImage)
    val assetCode: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
    val leftButton: Button = v.findViewById(R.id.leftButton)
    val rightButton: Button = v.findViewById(R.id.rightButton)
  }

  //endregion

  //region Bind View Holders
  private fun displayAssetHeader(viewHolder: AssetHeaderViewHolder, position: Int) {
    val titleText = items[position] as String
    viewHolder.title.text = titleText
  }

  private fun displayAsset(holder: AssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset
    val assetCode = asset.code.toUpperCase()

    holder.assetCode.text = asset.name
    holder.assetBalance.text = StringFormat.truncateDecimalPlaces(asset.amount,
      AssetUtils.getMaxDecimals(assetCode))

    // Buttons
    when {
      asset.amount!!.toDouble() == 0.0 &&
        assetCode != Constants.LUMENS_ASSET_CODE &&
        assetCode != Constants.DMC_ASSET_TYPE -> {
        holder.rightButton.visibility = View.VISIBLE
        holder.rightButton.setText(R.string.remove_asset)
        holder.rightButton.setTextColor(
          ContextCompat.getColor(context, R.color.colorTerracotta))
        holder.rightButton.setOnClickListener {
          listener.changeTrustline(asset.asset!!, true)
        }
      }
      else -> {
        holder.rightButton.setTextColor(
          ContextCompat.getColor(context, R.color.colorAccent))
        holder.rightButton.visibility = View.GONE
      }
    }

    when (assetCode) {
      Constants.LUMENS_ASSET_CODE -> {
        holder.leftButton.visibility = View.VISIBLE
        holder.leftButton.setText(R.string.buy)

        holder.rightButton.visibility = View.VISIBLE
        holder.rightButton.setText(R.string.set_inflation)
      }
      Constants.DMC_ASSET_TYPE -> {
        holder.leftButton.visibility = View.VISIBLE
        holder.leftButton.setText(R.string.learn)

        holder.rightButton.visibility = View.VISIBLE
        holder.rightButton.setText(R.string.trade)
      }
      Constants.ZAR_ASSET_TYPE -> {
        holder.leftButton.visibility = View.VISIBLE
        holder.leftButton.setText(R.string.deposit)

        val hasAvailableZAR = AccountUtils.hasAvailableAssets(asset.code)

        holder.rightButton.visibility = if (hasAvailableZAR) View.VISIBLE else View.GONE
        holder.rightButton.setText(R.string.withdraw)
      }
      Constants.NGNT_ASSET_TYPE -> {
        holder.leftButton.visibility = View.VISIBLE
        holder.leftButton.setText(R.string.deposit)

        val hasNGNT = AccountUtils.hasAvailableAssets(asset.code)

        holder.rightButton.visibility = if (hasNGNT) View.VISIBLE else View.GONE
        holder.rightButton.setText(R.string.withdraw)
      }
      else -> {
        holder.leftButton.visibility = View.GONE
        holder.rightButton.visibility = View.GONE
      }
    }

    holder.leftButton.setOnClickListener {
      listener.deposit(assetCode)
    }

    holder.rightButton.setOnClickListener {
      listener.withdraw(assetCode)
    }


    holder.defaultImage.visibility = View.GONE
    holder.assetCode.text = asset.name

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
      listener.assetSelected(if (assetCode == Constants.LUMENS_ASSET_CODE) DefaultAsset()
      else SessionAssetImpl(assetCode, asset.name, asset.issuer),
        holder.assetImage, holder.assetCode, holder.assetBalance)
    }
  }

  private fun displaySupportedAsset(viewHolder: SupportedAssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset
    val assetCode = asset.code.toUpperCase()
    val trustLineAsset = Asset.createNonNativeAsset(assetCode, KeyPair.fromAccountId(asset.issuer))

    viewHolder.defaultImage.visibility = View.GONE
    viewHolder.leftButton.visibility = View.GONE

    viewHolder.assetCode.text = assetCode
    viewHolder.assetBalance.text = asset.name
    viewHolder.assetBalance.visibility = View.VISIBLE

    loadAssetImage(asset.image, viewHolder.assetImage)

    viewHolder.rightButton.visibility = View.VISIBLE
    viewHolder.rightButton.setText(R.string.add_asset)
    viewHolder.rightButton.setTextColor(
      ContextCompat.getColor(context, R.color.colorGreen))
    viewHolder.rightButton.setOnClickListener {
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

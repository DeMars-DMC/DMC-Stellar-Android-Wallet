package io.demars.stellarwallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    val assetName: TextView = v.findViewById(R.id.assetName)
    val assetBalance: TextView = v.findViewById(R.id.assetBalance)
    val leftButton: Button = v.findViewById(R.id.leftButton)
    val rightButton: Button = v.findViewById(R.id.rightButton)
  }

  class SupportedAssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImage)
    val defaultImage: TextView = v.findViewById(R.id.defaultImage)
    val assetName: TextView = v.findViewById(R.id.assetName)
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

  private fun displayAsset(viewHolder: AssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset

    viewHolder.assetName.text = asset.name
    viewHolder.assetBalance.text = StringFormat.truncateDecimalPlaces(asset.amount,
      AssetUtils.getMaxDecimals(asset.code))

    // Buttons
    when {
      asset.amount!!.toDouble() == 0.0 &&
        asset.code != Constants.LUMENS_ASSET_CODE &&
        asset.code != Constants.DMC_ASSET_TYPE -> {
        viewHolder.rightButton.visibility = View.VISIBLE
        viewHolder.rightButton.setText(R.string.remove_asset)
        viewHolder.rightButton.setTextColor(
          ContextCompat.getColor(context, R.color.colorTerracotta))
        viewHolder.rightButton.setOnClickListener {
          listener.changeTrustline(asset.asset!!, true)
        }
      }
      else -> {
        viewHolder.rightButton.setTextColor(
          ContextCompat.getColor(context, R.color.colorAccent))
        viewHolder.rightButton.visibility = View.GONE
      }
    }

    when {
      asset.code == Constants.LUMENS_ASSET_CODE -> {
        viewHolder.leftButton.visibility = View.VISIBLE
        viewHolder.leftButton.setText(R.string.buy)

        viewHolder.rightButton.visibility = View.VISIBLE
        viewHolder.rightButton.setText(R.string.set_inflation)
      }
      asset.code.equals(Constants.DMC_ASSET_TYPE, true) -> {
        viewHolder.leftButton.visibility = View.VISIBLE
        viewHolder.leftButton.setText(R.string.learn)

        viewHolder.rightButton.visibility = View.VISIBLE
        viewHolder.rightButton.setText(R.string.trade)
      }

      asset.code.equals(Constants.ZAR_ASSET_TYPE, true) -> {
        viewHolder.leftButton.visibility = View.VISIBLE
        viewHolder.leftButton.setText(R.string.deposit)

        val hasAvailableZAR = AccountUtils.hasAvailableAssets(asset.code)

        viewHolder.rightButton.visibility = if (hasAvailableZAR) View.VISIBLE else View.GONE
        viewHolder.rightButton.setText(R.string.withdraw)
      }

      asset.code.equals(Constants.NGNT_ASSET_TYPE, true) -> {
        viewHolder.leftButton.visibility = View.VISIBLE
        viewHolder.leftButton.setText(R.string.deposit)

        val hasNGNT = AccountUtils.hasAvailableAssets(asset.code)

        viewHolder.rightButton.visibility = if (hasNGNT) View.VISIBLE else View.GONE
        viewHolder.rightButton.setText(R.string.withdraw)
      }
      else -> {
        viewHolder.leftButton.visibility = View.GONE
        viewHolder.rightButton.visibility = View.GONE
      }
    }

    viewHolder.leftButton.setOnClickListener {
      listener.deposit(asset.code.toUpperCase())
    }

    viewHolder.rightButton.setOnClickListener {
      listener.withdraw(asset.code.toUpperCase())
    }


    viewHolder.defaultImage.visibility = View.GONE
    viewHolder.assetName.text = asset.name

    // Image
    when {
      asset.code.equals(Constants.ZAR_ASSET_TYPE, true) ->
        loadAssetImage(Constants.ZAR_IMAGE_RES, viewHolder.assetImage)
      asset.code.equals(Constants.DMC_ASSET_TYPE, true) ->
        loadAssetImage(Constants.DMC_IMAGE_RES, viewHolder.assetImage)
      asset.code.equals(Constants.NGNT_ASSET_TYPE, true) ->
        loadAssetImage(Constants.NGNT_IMAGE_RES, viewHolder.assetImage)
      asset.image != 0 -> {
        loadAssetImage(asset.image, viewHolder.assetImage)
      }
      else -> {
        viewHolder.defaultImage.text = asset.code.toUpperCase()
        viewHolder.defaultImage.visibility = View.VISIBLE
        viewHolder.assetImage.setImageDrawable(null)
      }
    }

    viewHolder.itemView.setOnClickListener {
      listener.assetSelected(if (asset.code == Constants.LUMENS_ASSET_CODE) DefaultAsset()
      else SessionAssetImpl(asset.code.toUpperCase(), asset.name, asset.issuer))
    }
  }

  private fun displaySupportedAsset(viewHolder: SupportedAssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset
    val trustLineAsset = Asset.createNonNativeAsset(asset.code.toUpperCase(), KeyPair.fromAccountId(asset.issuer))

    viewHolder.defaultImage.visibility = View.GONE
    viewHolder.leftButton.visibility = View.GONE

    viewHolder.assetName.text = asset.code.toUpperCase()
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

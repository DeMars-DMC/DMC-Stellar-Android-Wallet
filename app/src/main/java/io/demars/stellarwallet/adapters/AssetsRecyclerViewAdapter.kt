package io.demars.stellarwallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.AssertListener
import io.demars.stellarwallet.utils.StringFormat
import com.squareup.picasso.Picasso
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.AssetUtils
import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair

class AssetsRecyclerViewAdapter(private var context: Context,
                                private var listener: AssertListener,
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
        configureAssetViewHolder(vh, position)
      }
      TYPE_SUPPORTED_ASSET -> {
        val vh = holder as SupportedAssetViewHolder
        configureSupportedAssetViewHolder(vh, position)
      }
      else -> {
        val vh = holder as AssetHeaderViewHolder
        configureAssetHeaderViewHolder(vh, position)
      }
    }
  }

  //region View Holders

  class AssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImageView)
    val defaultImage: TextView = v.findViewById(R.id.defaultAssetView)
    val assetName: TextView = v.findViewById(R.id.assetNameTextView)
    val assetAmount: TextView = v.findViewById(R.id.assetAmountTextView)
    val assetIndicator: ImageView = v.findViewById(R.id.assetIndicator)
    val assetButton: ImageButton = v.findViewById(R.id.assetButton)
    val buyButton: Button = v.findViewById(R.id.buyButton)
  }

  class AssetHeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val title: TextView = v.findViewById(R.id.titleText)
  }

  class SupportedAssetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val assetImage: ImageView = v.findViewById(R.id.assetImageView)
    val defaultImage: TextView = v.findViewById(R.id.defaultAssetView)
    val assetName: TextView = v.findViewById(R.id.assetNameTextView)
    val assetAmount: TextView = v.findViewById(R.id.assetAmountTextView)
    val assetButton: ImageButton = v.findViewById(R.id.assetButton)
  }

  //endregion

  //region Bind View Holders

  private fun configureAssetViewHolder(viewHolder: AssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset

    viewHolder.assetButton.visibility = View.VISIBLE
    viewHolder.assetName.text = asset.name
    viewHolder.assetAmount.text = StringFormat.truncateDecimalPlaces(asset.amount,
      AssetUtils.getMaxDecimals(asset.code))
    viewHolder.assetIndicator.visibility = if (AssetUtils.isSessionAsset(asset.code))
      View.VISIBLE else View.GONE
    // Buttons
    when {
      asset.code == Constants.LUMENS_ASSET_CODE -> {
        viewHolder.buyButton.visibility = View.VISIBLE
        viewHolder.buyButton.setText(R.string.buy)
        viewHolder.buyButton.setOnClickListener {
          listener.buyXLM()
        }
      }
      asset.code.equals(Constants.DMC_ASSET_TYPE, true) -> {
        viewHolder.buyButton.visibility = View.VISIBLE
        viewHolder.buyButton.setText(R.string.trade)
        viewHolder.buyButton.setOnClickListener {
          listener.tradeDMC()
        }
      }
      asset.code.equals(Constants.ZAR_ASSET_TYPE, true) && Firebase.canDeposit() -> {
        viewHolder.buyButton.visibility = View.VISIBLE
        viewHolder.buyButton.setText(R.string.deposit)
        viewHolder.buyButton.setOnClickListener {
          listener.depositZAR()
        }
      }
      else -> viewHolder.buyButton.visibility = View.GONE
    }

    // Image
    if (asset.image != 0) {
      viewHolder.defaultImage.visibility = View.GONE
      Picasso.get()
        .load(asset.image)
        .resize(256, 256)
        .onlyScaleDown()
        .into(viewHolder.assetImage)
    } else {
      when {
        asset.code.equals(Constants.ZAR_ASSET_TYPE, true) -> {
          viewHolder.defaultImage.visibility = View.GONE
          viewHolder.assetName.text = asset.name
          Picasso.get()
            .load(Constants.ZAR_IMAGE_RES)
            .resize(256, 256)
            .onlyScaleDown()
            .into(viewHolder.assetImage)
        }
        asset.code.equals(Constants.DMC_ASSET_TYPE, true) -> {
          viewHolder.defaultImage.visibility = View.GONE
          Picasso.get()
            .load(Constants.DMC_IMAGE_RES)
            .resize(256, 256)
            .onlyScaleDown()
            .into(viewHolder.assetImage)
        }
        else -> {
          viewHolder.defaultImage.text = asset.name[0].toString()
          viewHolder.defaultImage.visibility = View.VISIBLE
          viewHolder.assetImage.setImageDrawable(null)
        }
      }
    }

    when {
      asset.code == Constants.LUMENS_ASSET_CODE -> {
        viewHolder.assetButton.visibility = View.GONE
      }
      asset.amount!!.toDouble() == 0.0 -> {
        viewHolder.assetButton.setImageResource(R.drawable.ic_clear)
        viewHolder.assetButton.setOnClickListener {
          listener.changeTrustline(asset.asset!!, true)
        }
      }
      else -> viewHolder.assetButton.visibility = View.GONE
    }

    viewHolder.itemView.setOnClickListener {
      listener.assetSelected(if (asset.code == Constants.LUMENS_ASSET_CODE) DefaultAsset()
      else SessionAssetImpl(asset.code.toUpperCase(), asset.name, asset.issuer))
    }
  }

  private fun configureAssetHeaderViewHolder(viewHolder: AssetHeaderViewHolder, position: Int) {
    val titleText = items[position] as String
    viewHolder.title.text = titleText
  }

  private fun configureSupportedAssetViewHolder(viewHolder: SupportedAssetViewHolder, position: Int) {
    val asset = items[position] as SupportedAsset
    val trustLineAsset = Asset.createNonNativeAsset(asset.code.toUpperCase(), KeyPair.fromAccountId(asset.issuer))

    viewHolder.assetName.text = String.format(context.getString(R.string.asset_template),
      asset.name, asset.code.toUpperCase())

    viewHolder.assetAmount.visibility = View.GONE
    viewHolder.defaultImage.visibility = View.GONE

    Picasso.get()
      .load(asset.image)
      .resize(256, 256)
      .onlyScaleDown()
      .into(viewHolder.assetImage)

    viewHolder.assetButton.setImageResource(R.drawable.ic_add)
    viewHolder.assetButton.setOnClickListener {
      listener.changeTrustline(trustLineAsset, false)
    }
    viewHolder.assetButton.visibility = View.VISIBLE
  }

  //endregion
}

package io.demars.stellarwallet.adapters

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.activities.InflationActivity
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.interfaces.ChangeTrustlineListener
import io.demars.stellarwallet.models.DefaultAsset
import io.demars.stellarwallet.models.SessionAssetImpl
import io.demars.stellarwallet.models.SupportedAsset
import io.demars.stellarwallet.models.SupportedAssetType
import io.demars.stellarwallet.utils.AccountUtils
import io.demars.stellarwallet.utils.StringFormat
import com.squareup.picasso.Picasso
import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair

class AssetsRecyclerViewAdapter(var context: Context, private var listener: ChangeTrustlineListener, private var items : ArrayList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_ASSET = 0
        const val TYPE_HEADER = 1
        const val TYPE_SUPPORTED_ASSET = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when(viewType) {
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
            items[position] is SupportedAsset && (items[position] as SupportedAsset).type == SupportedAssetType.ADDED -> TYPE_ASSET
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

    class AssetViewHolder(v : View) : RecyclerView.ViewHolder(v) {
        val assetImage : ImageView = v.findViewById(R.id.assetImageView)
        val defaultImage : TextView = v.findViewById(R.id.defaultAssetView)
        val assetName : TextView = v.findViewById(R.id.assetNameTextView)
        val assetAmount : TextView = v.findViewById(R.id.assetAmountTextView)
        val assetButton : Button = v.findViewById(R.id.assetButton)
    }

    class AssetHeaderViewHolder(v : View) : RecyclerView.ViewHolder(v) {
        val title : TextView = v.findViewById(R.id.titleText)
    }

    class SupportedAssetViewHolder(v : View) : RecyclerView.ViewHolder(v) {
        val assetImage : ImageView = v.findViewById(R.id.assetImageView)
        val defaultImage : TextView = v.findViewById(R.id.defaultAssetView)
        val assetName : TextView = v.findViewById(R.id.assetNameTextView)
        val assetAmount : TextView = v.findViewById(R.id.assetAmountTextView)
        val assetButton : Button = v.findViewById(R.id.assetButton)
    }

    //endregion

    //region Bind View Holders

    private fun configureAssetViewHolder(viewHolder : AssetViewHolder, position : Int) {
        val asset = items[position] as SupportedAsset

        viewHolder.assetButton.visibility = View.VISIBLE
        viewHolder.assetName.text = asset.name
        viewHolder.assetAmount.text = String.format(context.getString(R.string.balance_template),
                StringFormat.truncateDecimalPlaces(asset.amount), asset.code.toUpperCase())

        if (asset.image != 0) {
            viewHolder.defaultImage.visibility = View.GONE
            viewHolder.assetImage.visibility = View.VISIBLE
            Picasso.get().load(asset.image).into(viewHolder.assetImage)
        } else {
            when {
              asset.code.equals(Constants.RAND_ASSET_TYPE, true) -> {
                  viewHolder.defaultImage.visibility = View.GONE
                  viewHolder.assetImage.visibility = View.VISIBLE
                  Picasso.get().load(Constants.RAND_IMAGE_RES).into(viewHolder.assetImage)
              }
//              asset.code.equals(Constants.NKLS_ASSET_TYPE, true) -> {
//                  viewHolder.defaultImage.visibility = View.GONE
//                  viewHolder.assetImage.visibility = View.VISIBLE
//                  Picasso.get().load(Constants.NKLS_IMAGE_RES).into(viewHolder.assetImage)
//              }
              asset.code.equals(Constants.RGTS_ASSET_TYPE, true) -> {
                  viewHolder.defaultImage.visibility = View.GONE
                  viewHolder.assetImage.visibility = View.VISIBLE
                  Picasso.get().load(Constants.RGTS_IMAGE_RES).into(viewHolder.assetImage)
              }
              else -> {
                  viewHolder.defaultImage.text = asset.name[0].toString()
                  viewHolder.defaultImage.visibility = View.VISIBLE
                  viewHolder.assetImage.visibility = View.GONE
              }
            }
        }

        when {
          asset.code == Constants.LUMENS_ASSET_CODE -> {
              viewHolder.assetButton.text = context.getString(R.string.set_inflation_message)
              viewHolder.assetButton.background = ContextCompat.getDrawable(context, R.drawable.button_accent)
              viewHolder.assetButton.setOnClickListener {
                  if (WalletApplication.wallet.getBalances().isNotEmpty() &&
                    AccountUtils.getTotalBalance(Constants.LUMENS_ASSET_TYPE).toDouble() > 1.0) {
                      context.startActivity(Intent(context, InflationActivity::class.java))
                  } else {
                      showBalanceErrorDialog()
                  }
              }
          }
          asset.amount!!.toDouble() == 0.0 -> {
              viewHolder.assetButton.text = context.getString(R.string.remove_asset_message)
              viewHolder.assetButton.background = ContextCompat.getDrawable(context, R.drawable.button_apricot)
              viewHolder.assetButton.setOnClickListener {
                  listener.changeTrustline(asset.asset!!, true)
              }
          }
          else -> viewHolder.assetButton.visibility = View.GONE
        }

        viewHolder.itemView.setOnClickListener {
            if (asset.code == Constants.LUMENS_ASSET_CODE) {
                WalletApplication.userSession.setSessionAsset(DefaultAsset())
            } else {
                WalletApplication.userSession.setSessionAsset(SessionAssetImpl(asset.code.toUpperCase(), asset.name, asset.issuer))
            }
            (context as Activity).finish()
        }
    }

    private fun configureAssetHeaderViewHolder(viewHolder : AssetHeaderViewHolder, position : Int) {
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

        Picasso.get().load(asset.image).into(viewHolder.assetImage)

        viewHolder.assetButton.text = context.getString(R.string.add_asset)
        viewHolder.assetButton.background = ContextCompat.getDrawable(context, R.drawable.button_accent)
        viewHolder.assetButton.setOnClickListener {
            listener.changeTrustline(trustLineAsset, false)
        }
        viewHolder.assetButton.visibility = View.VISIBLE
    }

    //endregion

    //region User Interface
    private fun showBalanceErrorDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.no_balance_dialog_title))
                .setMessage(context.getString(R.string.no_balance_text_message))
                .setPositiveButton(context.getString(R.string.ok)) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
    }
    //endregion

}

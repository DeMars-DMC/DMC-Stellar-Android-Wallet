package io.demars.stellarwallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.interfaces.OnDeleteRequest
import io.demars.stellarwallet.models.ui.MyOffer
import kotlinx.android.synthetic.main.item_offer.view.*
import java.text.DecimalFormat
import java.text.NumberFormat

class MyOffersAdapter(private val myOffersList: MutableList<MyOffer>, private val context: Context?, private val onDeleteRequest: OnDeleteRequest)
    : RecyclerView.Adapter<MyOffersAdapter.ViewHolder>() {
    private val decimalFormat : NumberFormat = DecimalFormat("0.####")

    override fun getItemCount(): Int {
        return myOffersList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_offer, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val myOffer = myOffersList[position]

        holder.description.text = context?.getString(R.string.rowDescription, decimalFormat.format(myOffer.amountFrom),
                myOffer.currencyFrom.code, decimalFormat.format(myOffer.amountTo), myOffer.currencyTo.code, decimalFormat.format(myOffer.amountTo / myOffer.amountFrom),
                myOffer.currencyFrom.code , myOffer.currencyTo.code )
        holder.delete.setOnClickListener {
            onDeleteRequest.onDialogOpen(myOffer.id)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.descriptionView
        val delete: ImageButton = view.deleteButton
    }
}
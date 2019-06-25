package io.demars.stellarwallet.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R
import io.demars.stellarwallet.activities.PayActivity
import io.demars.stellarwallet.activities.StellarAddressActivity
import io.demars.stellarwallet.models.Contact
import com.github.abdularis.civ.CircleImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.demars.stellarwallet.activities.ContactsActivity

/**
 * Contains a Contact List Item
 */
class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val image: CircleImageView = itemView.findViewById<View>(R.id.rounded_iv_profile) as CircleImageView
    private val label: TextView = itemView.findViewById<View>(R.id.tv_label) as TextView
    private val letter: TextView = itemView.findViewById<View>(R.id.contact_letter) as TextView
    private val button: TextView = itemView.findViewById<View>(R.id.button_add_address) as TextView

    private var mBoundContact: Contact? = null // Can be null
    private val colors: IntArray = intArrayOf(R.color.lightBlue, R.color.puce, R.color.colorMantis, R.color.brown,
            R.color.purple, R.color.pink, R.color.lightBlue, R.color.colorPaleSky, R.color.colorTerracotta, R.color.cornflowerBlue)

    fun bind(contact: Contact) {
        mBoundContact = contact
        val appContext = label.context.applicationContext
        label.text = contact.name
        image.visibility = View.INVISIBLE
        Picasso.get().load(contact.profilePic)
          .resize(256,256)
          .onlyScaleDown()
          .into(image, object : Callback {
            override fun onSuccess() {
                letter.text = null
                image.visibility = View.VISIBLE
            }

            override fun onError(e: Exception?) {
                if (contact.name.length > 1) {
                    val firstLetter = contact.name[0]
                    letter.text = firstLetter.toString()
                    val width = appContext.resources.getDimension(R.dimen.button_height_big).toInt()
                    val height = appContext.resources.getDimension(R.dimen.button_height_big).toInt()
                    image.setImageBitmap(createImage(width, height, getColor(appContext, firstLetter)))
                    image.visibility = View.VISIBLE
                } else {
                    letter.text = null
                }
            }
        })

        val stellarAddress = contact.stellarAddress
        if (stellarAddress.isNullOrBlank()) {
            button.text = appContext.getString(R.string.add_stellar_address)
            button.background = ContextCompat.getDrawable(appContext, R.drawable.button_accent)
        } else {
            button.text = appContext.getString(R.string.send_payment)
            button.background = ContextCompat.getDrawable(appContext, R.drawable.button_green)
        }

        button.setOnClickListener {
            val context = it.context
            stellarAddress?.let { that ->
                (context as Activity).startActivityForResult(
                  PayActivity.newIntent(context, that), ContactsActivity.RC_PAY_TO_CONTACT)
            } ?: run {
                context.startActivity(StellarAddressActivity.updateContact(context, contact))
            }
        }

        itemView.setOnClickListener {
            val context = it.context
            if (mBoundContact != null) {
                context.startActivity(StellarAddressActivity.updateContact(context, contact))
            }
        }
    }

    fun getColor(context : Context, char : Char) : Int {
        val index = char.toInt() % colors.size
        return ContextCompat.getColor(context, colors[index])
    }

    fun createImage(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}

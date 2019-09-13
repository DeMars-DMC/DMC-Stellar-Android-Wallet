package io.demars.stellarwallet.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.abdularis.civ.CircleImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.demars.stellarwallet.R
import io.demars.stellarwallet.interfaces.ContactListener
import io.demars.stellarwallet.models.Contact
import org.jetbrains.anko.textColor

class ContactsAdapter(private val contacts: ArrayList<Contact>, val listener: ContactListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, pos: Int): RecyclerView.ViewHolder {
    val listItemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_contact, parent, false)
    return ContactViewHolder(listItemView)
  }

  override fun onBindViewHolder(contactViewHolder: RecyclerView.ViewHolder, pos: Int) {
    if (contactViewHolder is ContactViewHolder) contactViewHolder.bind(contacts[pos], listener)
  }

  override fun getItemCount(): Int = contacts.size

  class ContactViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    private val image: CircleImageView = v.findViewById<View>(R.id.rounded_iv_profile) as CircleImageView
    private val label: TextView = v.findViewById<View>(R.id.tv_label) as TextView
    private val letter: TextView = v.findViewById<View>(R.id.contact_letter) as TextView
    private val button: TextView = v.findViewById<View>(R.id.button_add_address) as TextView

    private val colors: IntArray = intArrayOf(R.color.blueLight, R.color.puce, R.color.colorMantis, R.color.brown,
      R.color.purple, R.color.pink, R.color.colorPaleSky, R.color.colorRed, R.color.cornflowerBlue)

    fun bind(contact: Contact, listener: ContactListener) {
      val appContext = label.context.applicationContext
      label.text = contact.name
      image.visibility = View.INVISIBLE
      Picasso.get().load(contact.profilePic)
        .resize(256, 256)
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
        button.background = ContextCompat.getDrawable(appContext, R.drawable.background_card_transparent_accent)
        button.textColor = ContextCompat.getColor(appContext, R.color.colorAccent)
        button.setOnClickListener {
          listener.addAddressToContact(contact)
        }
      } else {
        button.text = appContext.getString(R.string.send_payment)
        button.background = ContextCompat.getDrawable(appContext, R.drawable.background_card_transparent_green)
        button.textColor = ContextCompat.getColor(appContext, R.color.colorGreen)
        button.setOnClickListener {
          listener.onPayToContact(contact)
        }
      }


      itemView.setOnClickListener {
        listener.onContactSelected(contact)
      }
    }

    fun getColor(context: Context, char: Char): Int {
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
}
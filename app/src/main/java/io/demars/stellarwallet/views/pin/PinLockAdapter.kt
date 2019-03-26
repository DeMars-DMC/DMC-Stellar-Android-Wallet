package io.demars.stellarwallet.views.pin

import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R

class PinLockAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  override fun getItemCount(): Int = 12

  var customizationOptions: CustomizationOptionsBundle? = null
  var onItemClickListener: OnNumberClickListener? = null
  var onDeleteClickListener: OnDeleteClickListener? = null
  var pinLength: Int = 0

  private var mKeyValues: IntArray? = getAdjustKeyValues(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0))

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val viewHolder: RecyclerView.ViewHolder
    val inflater = LayoutInflater.from(parent.context)

    if (viewType == VIEW_TYPE_NUMBER) {
      val view = inflater.inflate(R.layout.layout_number_item, parent, false)
      viewHolder = NumberViewHolder(view)
    } else {
      val view = inflater.inflate(R.layout.layout_delete_item, parent, false)
      viewHolder = DeleteViewHolder(view)
    }
    return viewHolder
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (holder.itemViewType == VIEW_TYPE_NUMBER) {
      val vh1 = holder as NumberViewHolder
      configureNumberButtonHolder(vh1, position)
    } else if (holder.itemViewType == VIEW_TYPE_DELETE) {
      val vh2 = holder as DeleteViewHolder
      configureDeleteButtonHolder(vh2)
    }
  }

  private fun configureNumberButtonHolder(holder: NumberViewHolder?, position: Int) {
    if (holder != null) {
      if (position == 9) {
        holder.mNumberButton.visibility = View.GONE
      } else {
        holder.mNumberButton.text = mKeyValues!![position].toString()
        holder.mNumberButton.visibility = View.VISIBLE
        holder.mNumberButton.tag = mKeyValues!![position]
      }

      if (customizationOptions != null) {
        holder.mNumberButton.setTextColor(customizationOptions!!.textColor)
        if (customizationOptions!!.buttonBackgroundDrawable != null) {
          holder.mNumberButton.background = customizationOptions!!.buttonBackgroundDrawable
        }
        holder.mNumberButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,
          customizationOptions!!.textSize.toFloat())
        val params = FrameLayout.LayoutParams(
          customizationOptions!!.buttonSize,
          customizationOptions!!.buttonSize,
          Gravity.CENTER)
        holder.mNumberButton.layoutParams = params
      }
    }
  }

  private fun configureDeleteButtonHolder(holder: DeleteViewHolder?) {
    if (holder != null) {
      if (customizationOptions!!.isShowDeleteButton && pinLength > 0) {
        holder.mButtonImage.visibility = View.VISIBLE
        if (customizationOptions!!.deleteButtonDrawable != null) {
          holder.mButtonImage.setImageDrawable(customizationOptions!!.deleteButtonDrawable)
        }
        holder.mButtonImage.setColorFilter(customizationOptions!!.textColor,
          PorterDuff.Mode.SRC_ATOP)
        val params = FrameLayout.LayoutParams(
          customizationOptions!!.deleteButtonSize,
          customizationOptions!!.deleteButtonSize,
          Gravity.CENTER)
        holder.mButtonImage.layoutParams = params
      } else {
        holder.mButtonImage.visibility = View.GONE
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == itemCount - 1) {
      VIEW_TYPE_DELETE
    } else VIEW_TYPE_NUMBER
  }

  private fun getAdjustKeyValues(keyValues: IntArray): IntArray {
    val adjustedKeyValues = IntArray(keyValues.size + 1)
    for (i in keyValues.indices) {
      if (i < 9) {
        adjustedKeyValues[i] = keyValues[i]
      } else {
        adjustedKeyValues[i] = -1
        adjustedKeyValues[i + 1] = keyValues[i]
      }
    }
    return adjustedKeyValues
  }

  inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal var mNumberButton: Button = itemView.findViewById(R.id.button) as Button

    init {
      mNumberButton.setOnClickListener { v ->
        if (onItemClickListener != null) {
          onItemClickListener!!.onNumberClicked(v.tag as Int)
        }
      }
    }
  }

  inner class DeleteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var mDeleteButton: FrameLayout = itemView.findViewById(R.id.button) as FrameLayout
    internal var mButtonImage: ImageView = itemView.findViewById(R.id.buttonImage) as ImageView

    init {

      if (customizationOptions!!.isShowDeleteButton && pinLength > 0) {
        mDeleteButton.setOnClickListener {
          if (onDeleteClickListener != null) {
            onDeleteClickListener!!.onDeleteClicked()
          }
        }

        mDeleteButton.setOnLongClickListener {
          if (onDeleteClickListener != null) {
            onDeleteClickListener!!.onDeleteLongClicked()
          }
          true
        }

        mDeleteButton.setOnTouchListener(object : View.OnTouchListener {
          private var rect: Rect? = null

          override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
              mButtonImage.setColorFilter(customizationOptions!!
                .deleteButtonPressesColor)
              rect = Rect(v.left, v.top, v.right, v.bottom)
            }
            if (event.action == MotionEvent.ACTION_UP) {
              mButtonImage.clearColorFilter()
            }
            if (event.action == MotionEvent.ACTION_MOVE) {
              if (!rect!!.contains(v.left + event.x.toInt(),
                  v.top + event.y.toInt())) {
                mButtonImage.clearColorFilter()
              }
            }
            return false
          }
        })
      }
    }
  }

  interface OnNumberClickListener {
    fun onNumberClicked(keyValue: Int)
  }

  interface OnDeleteClickListener {
    fun onDeleteClicked()

    fun onDeleteLongClicked()
  }

  companion object {
    private const val VIEW_TYPE_NUMBER = 0
    private const val VIEW_TYPE_DELETE = 1
  }
}
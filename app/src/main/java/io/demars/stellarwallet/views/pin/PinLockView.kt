package io.demars.stellarwallet.views.pin

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.demars.stellarwallet.R

class PinLockView : RecyclerView {

  private var mPin = ""
  private var mPinLength: Int = 0
  private var mHorizontalSpacing: Int = 0
  private var mVerticalSpacing: Int = 0
  private var mTextColor: Int = 0
  private var mDeleteButtonPressedColor: Int = 0
  private var mTextSize: Int = 0
  private var mButtonSize: Int = 0
  private var mDeleteButtonSize: Int = 0
  private var mButtonBackgroundDrawable: Drawable? = null
  private var mDeleteButtonDrawable: Drawable? = null
  private var mShowDeleteButton: Boolean = false

  private var mIndicatorDots: IndicatorDots? = null
  private var mAdapter: PinLockAdapter? = null
  private var mPinLockListener: PinLockListener? = null
  private var mCustomizationOptionsBundle: CustomizationOptionsBundle? = null

  var mDialerListener: DialerListener? = null

  private val mOnNumberClickListener = object : PinLockAdapter.OnNumberClickListener {
    override fun onNumberClicked(keyValue: Int) {
      mDialerListener?.onDial(keyValue)
      if (mPin.length < pinLength) {
        mPin += keyValue.toString()

        if (isIndicatorDotsAttached) {
          mIndicatorDots!!.updateDot(mPin.length)
        }

        if (mPin.length == 1) {
          mAdapter!!.pinLength = mPin.length
          mAdapter!!.notifyItemChanged(mAdapter!!.itemCount - 1)
        }

        if (mPinLockListener != null) {
          if (mPin.length == mPinLength) {
            mPinLockListener!!.onComplete(mPin)
          } else {
            mPinLockListener!!.onPinChange(mPin.length, mPin)
          }
        }

      } else {
        if (!isShowDeleteButton) {
          resetPinLockView()
          mPin += keyValue.toString()

          if (isIndicatorDotsAttached) {
            mIndicatorDots!!.updateDot(mPin.length)
          }

          if (mPinLockListener != null) {
            mPinLockListener!!.onPinChange(mPin.length, mPin)
          }

        } else {
          if (mPinLockListener != null) {
            mPinLockListener!!.onComplete(mPin)
          }
        }
      }
    }
  }

  private val mOnDeleteClickListener = object : PinLockAdapter.OnDeleteClickListener {
    override fun onDeleteClicked() {
      mDialerListener?.onDelete()
      if (mPin.isNotEmpty()) {
        mPin = mPin.substring(0, mPin.length - 1)

        if (isIndicatorDotsAttached) {
          mIndicatorDots!!.updateDot(mPin.length)
        }

        if (mPin.isEmpty()) {
          mAdapter!!.pinLength = mPin.length
          mAdapter!!.notifyItemChanged(mAdapter!!.itemCount - 1)
        }

        if (mPinLockListener != null) {
          if (mPin.isEmpty()) {
            mPinLockListener!!.onEmpty()
            clearInternalPin()
          } else {
            mPinLockListener!!.onPinChange(mPin.length, mPin)
          }
        }
      } else {
        if (mPinLockListener != null) {
          mPinLockListener!!.onEmpty()
        }
      }
    }

    override fun onDeleteLongClicked() {
      resetPinLockView()
      mDialerListener?.onDeleteAll()
      if (mPinLockListener != null) {
        mPinLockListener!!.onEmpty()
      }
    }
  }

  /**
   * Get the length of the current pin length
   *
   * @return the length of the pin
   */
  /**
   * Sets the pin length dynamically
   *
   * @param pinLength the pin length
   */
  var pinLength: Int
    get() = mPinLength
    set(pinLength) {
      this.mPinLength = pinLength

      if (isIndicatorDotsAttached) {
        mIndicatorDots!!.pinLength = pinLength
      }
    }

  /**
   * Get the text color in the buttons
   *
   * @return the text color
   */
  /**
   * Set the text color of the buttons dynamically
   *
   * @param textColor the text color
   */
  var textColor: Int
    get() = mTextColor
    set(textColor) {
      this.mTextColor = textColor
      mCustomizationOptionsBundle!!.textColor = textColor
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the size of the text in the buttons
   *
   * @return the size of the text in pixels
   */
  /**
   * Set the size of text in pixels
   *
   * @param textSize the text size in pixels
   */
  var textSize: Int
    get() = mTextSize
    set(textSize) {
      this.mTextSize = textSize
      mCustomizationOptionsBundle!!.textSize = textSize
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the size of the pin buttons
   *
   * @return the size of the button in pixels
   */
  /**
   * Set the size of the pin buttons dynamically
   *
   * @param buttonSize the button size
   */
  var buttonSize: Int
    get() = mButtonSize
    set(buttonSize) {
      this.mButtonSize = buttonSize
      mCustomizationOptionsBundle!!.buttonSize = buttonSize
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the current background drawable of the buttons, can be null
   *
   * @return the background drawable
   */
  /**
   * Set the background drawable of the buttons dynamically
   *
   * @param buttonBackgroundDrawable the background drawable
   */
  var buttonBackgroundDrawable: Drawable?
    get() = mButtonBackgroundDrawable
    set(buttonBackgroundDrawable) {
      this.mButtonBackgroundDrawable = buttonBackgroundDrawable
      mCustomizationOptionsBundle!!.buttonBackgroundDrawable = buttonBackgroundDrawable
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the drawable of the delete button
   *
   * @return the delete button drawable
   */
  /**
   * Set the drawable of the delete button dynamically
   *
   * @param deleteBackgroundDrawable the delete button drawable
   */
  var deleteButtonDrawable: Drawable?
    get() = mDeleteButtonDrawable
    set(deleteBackgroundDrawable) {
      this.mDeleteButtonDrawable = deleteBackgroundDrawable
      mCustomizationOptionsBundle!!.deleteButtonDrawable = deleteBackgroundDrawable
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the delete button size in pixels
   *
   * @return size in pixels
   */
  /**
   * Set the size of the delete button in pixels
   *
   * @param deleteButtonSize size in pixels
   */
  var deleteButtonSize: Int
    get() = mDeleteButtonSize
    set(deleteButtonSize) {
      this.mDeleteButtonSize = deleteButtonSize
      mCustomizationOptionsBundle!!.deleteButtonSize = deleteButtonSize
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Is the delete button shown
   *
   * @return returns true if shown, false otherwise
   */
  /**
   * Dynamically set if the delete button should be shown
   *
   * @param showDeleteButton true if the delete button should be shown, false otherwise
   */
  var isShowDeleteButton: Boolean
    get() = mShowDeleteButton
    set(showDeleteButton) {
      this.mShowDeleteButton = showDeleteButton
      mCustomizationOptionsBundle!!.isShowDeleteButton = showDeleteButton
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Get the delete button pressed/focused state color
   *
   * @return color of the button
   */
  /**
   * Set the pressed/focused state color of the delete button
   *
   * @param deleteButtonPressedColor the color of the delete button
   */
  var deleteButtonPressedColor: Int
    get() = mDeleteButtonPressedColor
    set(deleteButtonPressedColor) {
      this.mDeleteButtonPressedColor = deleteButtonPressedColor
      mCustomizationOptionsBundle!!.deleteButtonPressesColor = deleteButtonPressedColor
      mAdapter!!.notifyDataSetChanged()
    }

  /**
   * Returns true if [IndicatorDots] are attached to [PinLockView]
   *
   * @return true if attached, false otherwise
   */
  val isIndicatorDotsAttached: Boolean
    get() = mIndicatorDots != null

  constructor(context: Context) : super(context) {
    init(null)
  }

  constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) {
    init(attrs)
  }

  constructor(context: Context, @Nullable attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    init(attrs)
  }

  private fun init(attributeSet: AttributeSet?) {

    val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.PinLockView)

    try {
      mPinLength = typedArray.getInt(R.styleable.PinLockView_pinLength, DEFAULT_PIN_LENGTH)
      mHorizontalSpacing = typedArray.getDimension(R.styleable.PinLockView_keypadHorizontalSpacing, 0f).toInt()
      mVerticalSpacing = typedArray.getDimension(R.styleable.PinLockView_keypadVerticalSpacing, 0f).toInt()
      mTextColor = typedArray.getColor(R.styleable.PinLockView_keypadTextColor, ContextCompat.getColor(context, R.color.white))
      mTextSize = typedArray.getDimension(R.styleable.PinLockView_keypadTextSize, context.resources.getDimension(R.dimen.text_size)).toInt()
      mButtonSize = typedArray.getDimension(R.styleable.PinLockView_keypadButtonSize, context.resources.getDimension(R.dimen.button_height_big)).toInt()
      mDeleteButtonSize = typedArray.getDimension(R.styleable.PinLockView_keypadDeleteButtonSize, context.resources.getDimension(R.dimen.button_height_big)).toInt()
      mButtonBackgroundDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadButtonBackgroundDrawable)
      mDeleteButtonDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadDeleteButtonDrawable)
      mShowDeleteButton = typedArray.getBoolean(R.styleable.PinLockView_keypadShowDeleteButton, true)
      mDeleteButtonPressedColor = typedArray.getColor(R.styleable.PinLockView_keypadDeleteButtonPressedColor, ContextCompat.getColor(context, R.color.greyish))
    } finally {
      typedArray.recycle()
    }

    mCustomizationOptionsBundle = CustomizationOptionsBundle()
    mCustomizationOptionsBundle!!.textColor = mTextColor
    mCustomizationOptionsBundle!!.textSize = mTextSize
    mCustomizationOptionsBundle!!.buttonSize = mButtonSize
    mCustomizationOptionsBundle!!.buttonBackgroundDrawable = mButtonBackgroundDrawable
    mCustomizationOptionsBundle!!.deleteButtonDrawable = mDeleteButtonDrawable
    mCustomizationOptionsBundle!!.deleteButtonSize = mDeleteButtonSize
    mCustomizationOptionsBundle!!.isShowDeleteButton = mShowDeleteButton
    mCustomizationOptionsBundle!!.deleteButtonPressesColor = mDeleteButtonPressedColor

    initView()
  }

  private fun initView() {
    layoutManager = LTRGridLayoutManager(context, 3)

    mAdapter = PinLockAdapter()
    mAdapter!!.onItemClickListener = mOnNumberClickListener
    mAdapter!!.onDeleteClickListener = mOnDeleteClickListener
    mAdapter!!.customizationOptions = mCustomizationOptionsBundle
    adapter = mAdapter

    addItemDecoration(ItemSpaceDecoration(mHorizontalSpacing, mVerticalSpacing, 3, false))
    overScrollMode = OVER_SCROLL_NEVER
  }

  private fun swap(array: IntArray, index: Int, change: Int) {
    val temp = array[index]
    array[index] = array[change]
    array[change] = temp
  }

  /**
   * Sets a [PinLockListener] to the to listen to pin update events
   *
   * @param pinLockListener the listener
   */
  fun setPinLockListener(pinLockListener: PinLockListener) {
    this.mPinLockListener = pinLockListener
  }

  private fun clearInternalPin() {
    mPin = ""
  }

  /**
   * Resets the [PinLockView], clearing the entered pin
   * and resetting the [IndicatorDots] if attached
   */
  fun resetPinLockView() {

    clearInternalPin()

    mAdapter!!.pinLength = mPin.length
    mAdapter!!.notifyItemChanged(mAdapter!!.itemCount - 1)

    if (mIndicatorDots != null) {
      mIndicatorDots!!.updateDot(mPin.length)
    }
  }

  /**
   * Attaches [IndicatorDots] to [PinLockView]
   *
   * @param mIndicatorDots the view to attach
   */
  fun attachIndicatorDots(mIndicatorDots: IndicatorDots) {
    this.mIndicatorDots = mIndicatorDots
  }

  companion object {
    private const val DEFAULT_PIN_LENGTH = 4
  }

  class LTRGridLayoutManager : GridLayoutManager {
    override fun isLayoutRTL(): Boolean = false
    constructor(context: Context, spanCount: Int) : super(context, spanCount)
    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    @Suppress("unused")
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout)
  }

  interface PinLockListener {
    /**
     * Triggers when the complete pin is entered,
     * depends on the pin length set by the user
     *
     * @param pin the complete pin
     */
    fun onComplete(pin: String)


    /**
     * Triggers when the pin is empty after manual deletion
     */
    fun onEmpty()

    /**
     * Triggers on a key press on the [PinLockView]
     *
     * @param pinLength       the current pin length
     * @param intermediatePin the intermediate pin
     */
    fun onPinChange(pinLength: Int, intermediatePin: String)
  }

  interface DialerListener {
    fun onDial(number: Int)
    fun onDelete()
    fun onDeleteAll()
  }

  class ItemSpaceDecoration(private val mHorizontalSpaceWidth: Int, private val mVerticalSpaceHeight: Int, private val mSpanCount: Int, private val mIncludeEdge: Boolean) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

      val position = parent.getChildAdapterPosition(view)
      val column = position % mSpanCount

      if (mIncludeEdge) {
        outRect.left = mHorizontalSpaceWidth - column * mHorizontalSpaceWidth / mSpanCount
        outRect.right = (column + 1) * mHorizontalSpaceWidth / mSpanCount

        if (position < mSpanCount) {
          outRect.top = mVerticalSpaceHeight
        }
        outRect.bottom = mVerticalSpaceHeight
      } else {
        outRect.left = column * mHorizontalSpaceWidth / mSpanCount
        outRect.right = mHorizontalSpaceWidth - (column + 1) * mHorizontalSpaceWidth / mSpanCount
        if (position >= mSpanCount) {
          outRect.top = mVerticalSpaceHeight
        }
      }
    }
  }
}

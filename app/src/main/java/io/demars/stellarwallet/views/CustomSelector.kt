package io.demars.stellarwallet.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import io.demars.stellarwallet.adapters.CustomArrayAdapter
import io.demars.stellarwallet.models.SelectionModel
import kotlinx.android.synthetic.main.view_custom_selector.view.*
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.*
import io.demars.stellarwallet.R


class CustomSelector @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

  private val INPUT_TYPE_NUMBER = 1
  private val INPUT_TYPE_DECIMAL = 2
  private val INPUT_TYPE_TEXT = 3
  private val INPUT_TYPE_EMAIL = 4
  private val INPUT_TYPE_NONE = 5
  val editText: EditText
  val imageView : ImageView
  val spinner: Spinner

  init {
    LayoutInflater.from(context)
      .inflate(R.layout.view_custom_selector, this, true)
    editText = findViewById(R.id.editTextView)
    editText.id = View.generateViewId()
    imageView = findViewById(R.id.imageView)
    imageView.id = View.generateViewId()
    spinner = findViewById(R.id.spinner)
    spinner.id = View.generateViewId()

    attrs?.let {
      val typedArray = context.obtainStyledAttributes(it,
        R.styleable.CustomSelector, 0, 0)
      setHint(typedArray.getString(R.styleable.CustomSelector_hint)!!)
      setInputType(typedArray.getInt(R.styleable.CustomSelector_inputType, 0))
      typedArray.recycle()
    }
  }

  private fun setHint(hint: String) {
    editText.hint = hint
  }

  private fun setInputType(inputType: Int) {
    when (inputType) {
      INPUT_TYPE_NUMBER -> editText.inputType = InputType.TYPE_CLASS_NUMBER
      INPUT_TYPE_DECIMAL -> editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
      INPUT_TYPE_TEXT -> editText.inputType = InputType.TYPE_CLASS_TEXT
      INPUT_TYPE_EMAIL -> editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
      INPUT_TYPE_NONE -> editText.inputType = 0
      else -> editText.inputType = InputType.TYPE_CLASS_TEXT
    }
  }

  fun setSelectionValues(values: MutableList<SelectionModel>) {
    val selected = spinner.selectedItemPosition
    val customArrayAdapter = CustomArrayAdapter(context, R.layout.view_generic_spinner_item, values)
    spinner.adapter = customArrayAdapter
    spinner.setSelection(selected)
  }

  public override fun onSaveInstanceState(): Parcelable? {
    return super.onSaveInstanceState()?.let {
      val state = SavedState(it)
      state.text = this.editText.text.toString()
      state.spinnerPos = this.spinner.selectedItemPosition
      state
    }
  }

  public override fun onRestoreInstanceState(state: Parcelable) {
    if (state !is SavedState) {
      super.onRestoreInstanceState(state)
      return
    }

    super.onRestoreInstanceState(state.superState)
    this.editText.setText(state.text ?: "")
    this.spinner.setSelection(state.spinnerPos)
  }

  internal class SavedState : BaseSavedState {
    var text: String? = null
    var spinnerPos: Int = 0

    constructor(superState: Parcelable) : super(superState)
    private constructor(`in`: Parcel) : super(`in`) {
      this.text = `in`.readString()
      this.spinnerPos = `in`.readInt()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
      super.writeToParcel(out, flags)
      out.writeString(this.text)
      out.writeInt(this.spinnerPos)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
        override fun createFromParcel(`in`: Parcel): SavedState = SavedState(`in`)
        override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
      }
    }
  }

}
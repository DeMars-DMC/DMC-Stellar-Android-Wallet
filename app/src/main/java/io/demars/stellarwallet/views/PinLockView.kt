package io.demars.stellarwallet.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.demars.stellarwallet.R

class PinLockView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

  private class PinLockAdapter() : RecyclerView.Adapter<PinLockViewHolder>() {
    override fun getItemCount(): Int = 12
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): PinLockViewHolder =
      PinLockViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pinlockview_item, parent))
    override fun onBindViewHolder(viewHolder: PinLockViewHolder, position: Int) = viewHolder.display(position)
  }

  private class PinLockViewHolder(itemView: View) : ViewHolder(itemView) {

    fun display(position: Int) {
      when (position) {
        in 0..8 -> {
        }
        9 -> {
        }
        10 -> {
        }
        11 -> {
        }
      }
    }
  }
}
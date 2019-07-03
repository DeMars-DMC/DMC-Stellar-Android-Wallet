package io.demars.stellarwallet.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class CustomBehavior(context: Context, attributeSet: AttributeSet)
  : CoordinatorLayout.Behavior<View>(context, attributeSet) {

  override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean =
    dependency is AppBarLayout

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {

    return true
  }
}
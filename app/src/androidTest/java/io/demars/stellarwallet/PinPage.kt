package io.demars.stellarwallet

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers

object PinPage : BasePage() {
    override fun onPageLoaded(): PinPage {
        onView(ViewMatchers.withId(R.id.pinLockView))
        return this
    }

    fun proceedWithPin(pin : String): PinPage {
      writePin(pin)
        return this
    }

    private fun writePin(pin : String) {

        if (pin.length != 4) {
            throw IllegalStateException("PIN has to have 4 characters, now it has " + pin.length)
        }

      writeIndividualPinNumber(pin[0])
      writeIndividualPinNumber(pin[1])
      writeIndividualPinNumber(pin[2])
      writeIndividualPinNumber(pin[3])
    }

    private fun writeIndividualPinNumber(number : Char) {
        Espresso.onView(ViewMatchers.withId(R.id.pinLockView))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder> (
                        ViewMatchers.hasDescendant(ViewMatchers.withText(number.toString())), ViewActions.click()))
    }
}

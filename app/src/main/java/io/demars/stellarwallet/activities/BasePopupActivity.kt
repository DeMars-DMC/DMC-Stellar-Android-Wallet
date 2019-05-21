package io.demars.stellarwallet.activities

import android.os.Bundle
import io.demars.stellarwallet.R

abstract class BasePopupActivity : BaseActivity() {

    abstract fun setContent() : Int
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setContent())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
    }
}

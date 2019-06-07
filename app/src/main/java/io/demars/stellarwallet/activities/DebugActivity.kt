package io.demars.stellarwallet.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.demars.stellarwallet.R
import io.demars.stellarwallet.fragments.DebugFragment
import kotlinx.android.synthetic.main.activity_debug_preference.*

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_preference)

        setupActionBar()

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.root_container, DebugFragment())
                    .commit()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolBar_debug)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolBar_debug.setNavigationOnClickListener { onBackPressed() }
    }
}

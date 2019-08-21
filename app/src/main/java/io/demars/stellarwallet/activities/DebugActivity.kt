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

        backButton.setOnClickListener { onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.root_container, DebugFragment())
                    .commit()
        }
    }

    private fun setupActionBar() {
    }
}

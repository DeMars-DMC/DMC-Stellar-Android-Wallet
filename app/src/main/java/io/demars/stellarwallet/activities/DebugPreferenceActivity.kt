package io.demars.stellarwallet.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.demars.stellarwallet.R
import io.demars.stellarwallet.fragments.DebugFragment

class DebugPreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_preference)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.root_container, DebugFragment())
                    .commit()
        }
    }
}

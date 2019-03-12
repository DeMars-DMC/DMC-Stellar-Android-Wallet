package io.demars.stellarwallet.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceFragmentCompat
import io.demars.stellarwallet.R
import io.demars.stellarwallet.utils.GlobalGraphHelper

class DebugFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.debug_preferences)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let {
           if (getString(R.string.preference_debug_test_server) == key) {
               GlobalGraphHelper.restart(activity as FragmentActivity)
           }
        }
    }
}
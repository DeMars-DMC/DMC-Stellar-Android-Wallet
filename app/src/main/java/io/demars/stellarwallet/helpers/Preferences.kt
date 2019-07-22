package io.demars.stellarwallet.helpers

import android.content.Context
import androidx.preference.PreferenceManager

class Preferences {
  companion object {
    const val PREF_REPORTING_TYPE = "PREF_REPORTING_TYPE"
    const val PREF_REPORTING_CODE = "PREF_REPORTING_CODE"
    const val PREF_REPORTING_ISSUER = "PREF_REPORTING_ISSUER"

    fun getReportingAssetType(context: Context): String {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      return prefs.getString(PREF_REPORTING_TYPE, "native") ?: ""
    }

    fun setReportingAssetType(context: Context, type: String) {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      prefs.edit().putString(PREF_REPORTING_TYPE, type).apply()
    }

    fun getReportingAssetCode(context: Context): String {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      return prefs.getString(PREF_REPORTING_CODE, "") ?: ""
    }

    fun setReportingAssetCode(context: Context, code: String) {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      prefs.edit().putString(PREF_REPORTING_CODE, code).apply()
    }

    fun getReportingAssetIssuer(context: Context): String {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      return prefs.getString(PREF_REPORTING_ISSUER, "") ?: ""
    }

    fun setReportingAssetIssuer(context: Context, issuer: String) {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      prefs.edit().putString(PREF_REPORTING_ISSUER, issuer).apply()
    }
  }
}
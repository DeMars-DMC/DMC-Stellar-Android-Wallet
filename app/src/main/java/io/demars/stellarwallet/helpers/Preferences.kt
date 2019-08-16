package io.demars.stellarwallet.helpers

import android.content.Context
import androidx.preference.PreferenceManager

object Preferences {
  private const val PREF_REPORTING_TYPE = "PREF_REPORTING_TYPE"
  private const val PREF_REPORTING_CODE = "PREF_REPORTING_CODE"
  private const val PREF_REPORTING_ISSUER = "PREF_REPORTING_ISSUER"
  private const val PREF_INFLATION_SET = "PREF_INFLATION_SET"
  private const val PREF_NGNT_SET = "PREF_NGNT_SET"
  private const val PREF_BTC_SET = "PREF_BTC_SET"

  fun getReportingAssetType(context: Context): String =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getString(PREF_REPORTING_TYPE, "native") ?: "native"

  fun setReportingAssetType(context: Context, type: String) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putString(PREF_REPORTING_TYPE, type).apply()
  }

  fun getReportingAssetCode(context: Context): String =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getString(PREF_REPORTING_CODE, "") ?: ""


  fun setReportingAssetCode(context: Context, code: String) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putString(PREF_REPORTING_CODE, code).apply()
  }

  fun getReportingAssetIssuer(context: Context): String =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getString(PREF_REPORTING_ISSUER, "") ?: ""

  fun setReportingAssetIssuer(context: Context, issuer: String) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putString(PREF_REPORTING_ISSUER, issuer).apply()
  }

  fun inflationSetOnce(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putBoolean(PREF_INFLATION_SET, true).apply()
  }

  fun isInflationSetOnce(context: Context): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(PREF_INFLATION_SET, false)

  fun ngntSetOnce(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putBoolean(PREF_NGNT_SET, true).apply()
  }

  fun isNgntSetOnce(context: Context): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(PREF_NGNT_SET, false)

  fun btcSetOnce(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit().putBoolean(PREF_BTC_SET, true).apply()
  }

  fun isBtcSetOnce(context: Context): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(PREF_BTC_SET, false)
}


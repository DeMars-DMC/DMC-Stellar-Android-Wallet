package io.demars.stellarwallet.utils

import io.demars.stellarwallet.helpers.Constants
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class StringFormat {
  companion object {
    fun getWordCount(word: String): Int = word.split(" ".toRegex()).size
    fun getFormattedDateTime(str: String, is24hours: Boolean): String {
      val pattern = if (is24hours) "MMM dd, yyyy - HH:mm" else "MMM dd, yyyy - h:mm a"
      val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .withZone(ZoneId.systemDefault())
      return formatter.format(Instant.parse(str))
    }

    fun truncateDecimalPlaces(string: String?, places: Int): String =
      if (string == null) Constants.DEFAULT_ACCOUNT_BALANCE
      else truncateDecimalPlaces(string.toDouble(), places)

    fun truncateDecimalPlaces(string: Double?, places: Int): String =
      if (string == null) Constants.DEFAULT_ACCOUNT_BALANCE
      else String.format(Locale.ENGLISH, "%.${places}f", string)

    /**
     * Calculate the number of decimals of a string.
     */
    fun getNumDecimals(num: String): Int {
      return if (!hasDecimalPoint(num)) {
        0
      } else {
        num.substring(num.indexOf('.') + 1, num.length).length
      }
    }

    /**
     * Checks whether a string has a comma.
     */
    fun hasDecimalPoint(text: String): Boolean {
      for (i in 0 until text.length) {
        if (text[i] == '.') {
          return true
        }
      }
      return false
    }

    /**
     * Converts native to xlm, otherwise returns the same asset code
     */
    fun formatAssetCode(assetCode: String): String =
      if (assetCode == Constants.LUMENS_ASSET_TYPE) Constants.LUMENS_ASSET_CODE else assetCode

    /**
     * Capitalizes the first character of a string
     */
    fun capitalize(s: String?): String = if (s.isNullOrEmpty()) "" else s.capitalize()
  }
}

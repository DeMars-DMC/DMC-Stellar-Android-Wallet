package io.demars.stellarwallet.helpers

import android.content.Context
import io.demars.stellarwallet.interfaces.LocalStore
import io.demars.stellarwallet.models.AvailableBalance
import org.stellar.sdk.responses.AccountResponse
import shadow.com.google.gson.Gson
import shadow.com.google.gson.JsonSyntaxException
import timber.log.Timber

class LocalStoreImpl(context: Context) : LocalStore {
  private companion object {
    const val PRIVATE_MODE = 0
    const val PREF_NAME = "io.demars.stellarwallet.PREFERENCE_FILE_KEY"

    const val KEY_ENCRYPTED_PHRASE = "kEncryptedPhrase"
    const val KEY_ENCRYPTED_PASSPHRASE = "kEncryptedPassphrase"
    const val KEY_PIN_DATA = "kPinData"
    const val KEY_STELLAR_ACCOUNT_PUBLIC_KEY = "kStellarAccountPublicKey"
    const val KEY_STELLAR_BALANCES_KEY = "kStellarBalancesKey"
    const val KEY_STELLAR_AVAILABLE_BALANCES_KEY = "kAvailableBalancesKey"
    const val KEY_IS_RECOVERY_PHRASE = "kIsRecoveryPhrase"
    const val KEY_PIN_SETTINGS_SEND = "kPinSettingsSend"
    const val KEY_IS_PASSPHRASE_USED = "kIsPassphraseUsed"
    const val KEY_USER_STATE = "kUserState"
  }

  private val sharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
  private val gson = Gson()

  override fun getEncryptedPhrase(): String? {
    return getString(KEY_ENCRYPTED_PHRASE)
  }

  override fun setEncryptedPhrase(encryptedPassphrase: String?) {
    set(KEY_ENCRYPTED_PHRASE, encryptedPassphrase)
  }

  override fun getEncryptedPassphrase(): String? {
    return getString(KEY_ENCRYPTED_PASSPHRASE)
  }

  override fun setEncryptedPassphrase(encryptedPassphrase: String) {
    set(KEY_ENCRYPTED_PASSPHRASE, encryptedPassphrase)
  }

  override fun getStellarAccountId(): String? {
    return getString(KEY_STELLAR_ACCOUNT_PUBLIC_KEY)
  }

  override fun setStellarAccountId(accountId: String) {
    return set(KEY_STELLAR_ACCOUNT_PUBLIC_KEY, accountId)
  }

  override fun getBalances(): Array<AccountResponse.Balance> {
    return get(KEY_STELLAR_BALANCES_KEY, Array<AccountResponse.Balance>::class.java) ?: arrayOf()
  }

  override fun setBalances(balances: Array<AccountResponse.Balance>?) {
    set(KEY_STELLAR_BALANCES_KEY, balances)
  }

  override fun getAvailableBalances(): Array<AvailableBalance> {
    return get(KEY_STELLAR_AVAILABLE_BALANCES_KEY, Array<AvailableBalance>::class.java) ?: arrayOf()
  }

  override fun setAvailableBalances(availableBalances: Array<AvailableBalance>?) {
    return set(KEY_STELLAR_AVAILABLE_BALANCES_KEY, availableBalances)
  }

  override fun getIsRecoveryPhrase(): Boolean {
    return getBoolean(KEY_IS_RECOVERY_PHRASE, true)
  }

  override fun setIsRecoveryPhrase(isRecoveryPhrase: Boolean) {
    set(KEY_IS_RECOVERY_PHRASE, isRecoveryPhrase)
  }

  override fun setShowPinOnSend(showPinOnSend: Boolean) {
    set(KEY_PIN_SETTINGS_SEND, showPinOnSend)
  }

  override fun getShowPinOnSend(): Boolean {
    return getBoolean(KEY_PIN_SETTINGS_SEND, true)
  }

  override fun getUserState(): Int {
    return getInt(KEY_USER_STATE)
  }

  override fun setUserState(state: Int) {
    set(KEY_USER_STATE, state)
  }

  override fun isRegistered(): Boolean = getUserState() > 0

  override fun isVerified(): Boolean = getUserState() > 1

  private operator fun set(key: String, value: String?) {
    sharedPreferences.edit().putString(key, value).apply()
  }

  private operator fun set(key: String, value: Boolean) {
    sharedPreferences.edit().putBoolean(key, value).apply()
  }

  private operator fun set(key: String, value: Int) {
    sharedPreferences.edit().putInt(key, value).apply()
  }

  private operator fun <T> set(key: String, obj: T) {
    val json = gson.toJson(obj)
    set(key, json)
  }

  private fun getString(key: String): String? {
    return sharedPreferences.getString(key, null)
  }

  private fun contains(key: String): Boolean {
    return sharedPreferences.contains(key)
  }

  private fun getBoolean(key: String, defValue: Boolean): Boolean {
    return sharedPreferences.getBoolean(key, defValue)
  }

  private fun getBoolean(key: String): Boolean {
    return getBoolean(key, false)
  }

  private fun getInt(key: String, defValue: Int): Int {
    return sharedPreferences.getInt(key, defValue)
  }

  private fun getInt(key: String): Int {
    return getInt(key, 0)
  }

  private operator fun <T> get(key: String, klass: Class<T>): T? {
    val json = getString(key) ?: return null
    return try {
      gson.fromJson(json, klass)
    } catch (e: JsonSyntaxException) {
      Timber.w("unable to convert json $e")
      null
    }
  }

  override fun clearLocalStore(): Boolean {
    val editor = sharedPreferences.edit()
    editor.remove(KEY_ENCRYPTED_PHRASE)
    editor.remove(KEY_ENCRYPTED_PASSPHRASE)
    editor.remove(KEY_PIN_DATA)
    editor.remove(KEY_STELLAR_ACCOUNT_PUBLIC_KEY)
    editor.remove(KEY_STELLAR_BALANCES_KEY)
    editor.remove(KEY_STELLAR_AVAILABLE_BALANCES_KEY)
    editor.remove(KEY_IS_RECOVERY_PHRASE)
    editor.remove(KEY_IS_PASSPHRASE_USED)
    return editor.commit()
  }
}

package io.demars.stellarwallet.utils

import android.content.Context
import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.encryption.CipherWrapper
import io.demars.stellarwallet.encryption.KeyStoreWrapper
import io.demars.stellarwallet.helpers.Constants
import com.soneso.stellarmnemonics.Wallet
import io.demars.stellarwallet.models.AvailableBalance
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.AccountResponse

class AccountUtils {

  companion object {
    private const val CIPHER_TRANSFORMATION: String = "RSA/ECB/PKCS1Padding"

    fun generateWallet(context: Context, mnemonic: String, passphrase: String?, pin: String) {
      encryptAndStoreWallet(context, mnemonic, passphrase, pin)

      val stellarKeyPair = getStellarKeyPair(mnemonic, passphrase)

      WalletApplication.wallet.setStellarAccountId(stellarKeyPair.accountId)
      WalletApplication.userSession.setPin(pin)
    }

    fun getSecretSeed(context: Context): CharArray {
      val encryptedPhrase = WalletApplication.wallet.getEncryptedPhrase()!!
      val encryptedPassphrase = WalletApplication.wallet.getEncryptedPassphrase()
      val masterKey = getPinMasterKey(context, WalletApplication.userSession.getPin()!!)!!

      val decryptedPhrase = getDecryptedString(encryptedPhrase, masterKey)

      var decryptedPassphrase: String? = null
      if (encryptedPassphrase != null) {
        decryptedPassphrase = getDecryptedString(encryptedPassphrase, masterKey)
      }

      return getStellarKeyPair(decryptedPhrase, decryptedPassphrase).secretSeed
    }

    fun encryptAndStoreWallet(context: Context, mnemonic: String, passphrase: String?, pin: String): Boolean {
      val keyStoreWrapper = KeyStoreWrapper(context)
      keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(pin)

      val masterKey = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(pin) ?: return false
      val cipherWrapper = CipherWrapper(CIPHER_TRANSFORMATION)
      val encryptedPhrase: String

      if (passphrase == null || passphrase.isEmpty()) {
        encryptedPhrase = cipherWrapper.encrypt(mnemonic, masterKey.public)
      } else {
        WalletApplication.wallet.setEncryptedPassphrase(cipherWrapper.encrypt(passphrase, masterKey.public))
        encryptedPhrase = cipherWrapper.encrypt(mnemonic, masterKey.public)
      }

      WalletApplication.wallet.setEncryptedPhrase(encryptedPhrase)
      return true

    }

    fun getDecryptedString(encryptedPhrase: String, masterKey: java.security.KeyPair): String {
      val cipherWrapper = CipherWrapper(CIPHER_TRANSFORMATION)
      return cipherWrapper.decrypt(encryptedPhrase, masterKey.private)
    }

    fun getTotalBalance(assetCode: String): String {
      return WalletApplication.wallet.getBalances().find {
        (it.assetCode.equals(assetCode, true) ||
          it.assetType == "native" && assetCode.equals("XLM", true))
      }?.balance ?: Constants.DEFAULT_ACCOUNT_BALANCE
    }

    fun getPostedForTrade(assetCode: String): String {
      WalletApplication.wallet.getBalances().forEach {
        if (it.assetCode == assetCode ||
          it.assetCode.isNullOrEmpty() && assetCode.equals("XLM", true)) {
          return it.sellingLiabilities
        }
      }
      return Constants.DEFAULT_ACCOUNT_BALANCE
    }

    fun getPinMasterKey(context: Context, pin: String): java.security.KeyPair? {
      val keyStoreWrapper = KeyStoreWrapper(context)

      return keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(pin)
    }

    fun getStellarKeyPair(mnemonic: String, passphrase: String?): KeyPair {
      return if (WalletApplication.wallet.getIsRecoveryPhrase()) {
        Wallet.createKeyPair(mnemonic.toCharArray(), passphrase?.toCharArray(), Constants.USER_INDEX)
      } else {
        KeyPair.fromSecretSeed(mnemonic)
      }
    }

    fun getAvailableBalance(assetCode: String): String {
      return WalletApplication.wallet.getAvailableBalances().find {
        it.assetCode.equals(assetCode, true)
      }?.balance ?: Constants.DEFAULT_ACCOUNT_BALANCE
    }

    fun calculateAvailableBalances(response: AccountResponse): Array<AvailableBalance>? {
      if (response.balances.isNullOrEmpty()) return null

      val availableBalances = ArrayList<AvailableBalance>()
      response.balances?.forEach {
        val assetCode = if (it.assetType == "native") "XLM" else it.assetCode
        val totalBalance = getTotalBalance(assetCode).toDouble()
        val postedForTrade = getPostedForTrade(assetCode).toDouble()
        val available = if (assetCode == "XLM") {
          val minimumBalance = WalletApplication.userSession.getMinimumBalance()!!
          (totalBalance - postedForTrade - minimumBalance.totalAmount).toString()
        } else {
          (totalBalance - postedForTrade).toString()
        }

        availableBalances.add(AvailableBalance(assetCode, null, available))
      }

      return availableBalances.toTypedArray()
    }
  }
}
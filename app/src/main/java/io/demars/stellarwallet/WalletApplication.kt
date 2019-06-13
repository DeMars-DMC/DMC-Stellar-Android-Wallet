package io.demars.stellarwallet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.google.firebase.auth.FirebaseAuth
import io.demars.stellarwallet.helpers.LocalStoreImpl
import io.demars.stellarwallet.helpers.WalletLifecycleListener
import io.demars.stellarwallet.interfaces.WalletStore
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.DebugPreferencesHelper
import io.demars.stellarwallet.mvvm.exchange.ExchangeRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.remote.ServerType
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import io.demars.stellarwallet.encryption.PRNGFixes
import io.demars.stellarwallet.models.stellar.MinimumBalance
import org.bouncycastle.jce.provider.BouncyCastleProvider
import shadow.okhttp3.OkHttpClient
import timber.log.Timber
import java.security.Provider
import java.security.Security
import java.util.logging.Level
import java.util.logging.Logger

class WalletApplication : MultiDexApplication() {
  companion object {
    const val CHANNEL_ID_ACC = "CHANNEL_ID_ACC"

    // Use LocalStoreImpl for SharedPreferences
    lateinit var wallet: WalletStore

    var userSession = object : UserSession {
      var impl = UserSessionImpl()

      override fun getSessionAsset(): SessionAsset {
        return impl.getSessionAsset()
      }

      override fun setSessionAsset(sessionAsset: SessionAsset) {
        impl.setSessionAsset(sessionAsset)
        assetSession.postValue(sessionAsset)
      }

      override fun getPin(): String? {
        return impl.getPin()
      }

      override fun setPin(pin: String?) {
        impl.setPin(pin)
      }

      override fun getFormattedCurrentAssetCode(): String? {
        return impl.getFormattedCurrentAssetCode()
      }

      override fun getFormattedCurrentAvailableBalance(context: Context): String? {
        return impl.getFormattedCurrentAvailableBalance(context)
      }

      override fun getAvailableBalance(): String? {
        return impl.getAvailableBalance()
      }

      override fun setMinimumBalance(minimumBalance: MinimumBalance) {
        impl.setMinimumBalance(minimumBalance)
      }

      override fun getMinimumBalance(): MinimumBalance? {
        return impl.getMinimumBalance()
      }
    }

    var assetSession: MutableLiveData<SessionAsset> = MutableLiveData()

    var showPin = false
  }

  override fun onCreate() {
    super.onCreate()
    Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

    //removing the default provider coming from Android SDK.
    Security.removeProvider("BC")
    Security.addProvider(BouncyCastleProvider() as Provider?)

    FirebaseAuth.getInstance().useAppLanguage()

    Stetho.initializeWithDefaults(this)

    PRNGFixes.apply()


    if (DebugPreferencesHelper(applicationContext).isTestNetServerEnabled) {
      Horizon.init(ServerType.TEST_NET)
    } else {
      Horizon.init(ServerType.PROD)
    }

    AndroidThreeTen.init(this)

    setupLifecycleListener()

    createNotificationChannels()

    wallet = DmcWallet(LocalStoreImpl(applicationContext))

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())

      if (DebugPreferencesHelper(applicationContext).isLeakCanaryEnabled) {
        Timber.d("Enabling leak canary")
        if (LeakCanary.isInAnalyzerProcess(this)) {
          // This process is dedicated to LeakCanary for heap analysis.
          // You should not initFcm your app in this process.
          return
        }
        LeakCanary.install(this)
        // Normal app initFcm code...
      } else {
        Timber.d("Leak canary is disabled")
      }
    }

    // exchange providers addresses are not very likely to change but let's refresh them during application startup.
    ExchangeRepository(this).getAllExchangeProviders(true)
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Create the NotificationChannel
      val name = getString(R.string.account_activity)
      val descriptionText = getString(R.string.channel_description)
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val mChannel = NotificationChannel(CHANNEL_ID_ACC, name, importance)
      mChannel.description = descriptionText
      mChannel.lightColor = getColor(R.color.colorAccent)
      mChannel.setShowBadge(true)

      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(mChannel)
    }
  }

  private fun setupLifecycleListener() {
    ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleListener)
  }

  private val lifecycleListener: WalletLifecycleListener by lazy {
    WalletLifecycleListener(applicationContext)
  }
}

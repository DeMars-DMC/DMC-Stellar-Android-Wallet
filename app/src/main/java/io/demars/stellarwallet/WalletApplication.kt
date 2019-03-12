package io.demars.stellarwallet

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import io.demars.stellarwallet.encryption.PRNGFixes
import io.demars.stellarwallet.helpers.LocalStoreImpl
import io.demars.stellarwallet.helpers.WalletLifecycleListener
import io.demars.stellarwallet.interfaces.WalletStore
import io.demars.stellarwallet.models.*
import io.demars.stellarwallet.utils.DebugPreferencesHelper
import io.demars.stellarwallet.mvvm.exchange.ExchangeRepository
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.remote.ServerType
import com.facebook.stetho.Stetho
import com.squareup.leakcanary.LeakCanary
import org.bouncycastle.jce.provider.BouncyCastleProvider
import shadow.okhttp3.OkHttpClient
import timber.log.Timber
import java.security.Provider
import java.security.Security
import java.util.logging.Level
import java.util.logging.Logger

class WalletApplication : MultiDexApplication() {
    companion object {
        // Use LocalStoreImpl for SharedPreferences
        lateinit var wallet: WalletStore

        var userSession = object : UserSession {
            var impl = UserSessionImpl()

            override fun getSessionAsset(): SessionAsset { return impl.getSessionAsset() }
            override fun setSessionAsset(sessionAsset: SessionAsset) {
                impl.setSessionAsset(sessionAsset)
                assetSession.postValue(sessionAsset)
            }
            override fun getPin(): String? { return impl.getPin() }
            override fun setPin(pin: String?) { impl.setPin(pin) }
            override fun getFormattedCurrentAssetCode(): String? { return impl.getFormattedCurrentAssetCode() }
            override fun getFormattedCurrentAvailableBalance(context: Context): String? { return impl.getFormattedCurrentAvailableBalance(context) }
            override fun getAvailableBalance(): String? { return impl.getAvailableBalance() }
            override fun setMinimumBalance(minimumBalance: MinimumBalance) { impl.setMinimumBalance(minimumBalance) }
            override fun getMinimumBalance(): MinimumBalance? { return impl.getMinimumBalance() }
        }

        var assetSession : MutableLiveData<SessionAsset> = MutableLiveData()

        var appReturnedFromBackground = false
    }

    override fun onCreate() {
        super.onCreate()
        Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

        //removing the default provider coming from Android SDK.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider() as Provider?)

        PRNGFixes.apply()

        setupLifecycleListener()

        wallet = DmcWallet(LocalStoreImpl(applicationContext))

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            Timber.plant(Timber.DebugTree())

            if (DebugPreferencesHelper(applicationContext).isLeakCanaryEnabled) {
                Timber.d("Enabling leak canary")
                if (LeakCanary.isInAnalyzerProcess(this)) {
                    // This process is dedicated to LeakCanary for heap analysis.
                    // You should not init your app in this process.
                    return
                }
                LeakCanary.install(this)
                // Normal app init code...
            } else {
                Timber.d("Leak canary is disabled")
            }

            if (DebugPreferencesHelper(applicationContext).isTestNetServerEnabled) {
                Horizon.init(ServerType.TEST_NET)
            } else {
                Horizon.init(ServerType.PROD)
            }
        } else {
            Horizon.init(ServerType.PROD)
        }

        // exchange providers addresses are not very likely to change but let's refresh them during application startup.
        ExchangeRepository(this).getAllExchangeProviders(true)
    }

    private fun setupLifecycleListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleListener)
    }

    private val lifecycleListener: WalletLifecycleListener by lazy {
        WalletLifecycleListener(applicationContext)
    }
}

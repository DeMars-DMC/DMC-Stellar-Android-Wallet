package io.demars.stellarwallet.encryption

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyStoreWrapperTest {
    private val aliases = arrayOf("1234", "5678", "0987")

    @Test
    fun clear_aliases() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val keyStoreWrapper = KeyStoreWrapper(context)

        aliases.forEach {
            keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(it)
        }

        aliases.forEach {
            assert(keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(it) != null)
        }

        keyStoreWrapper.clear()

        aliases.forEach {
            assert(keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(it) == null)
        }
    }
}
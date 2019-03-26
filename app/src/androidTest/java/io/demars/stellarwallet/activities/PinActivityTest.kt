package io.demars.stellarwallet.activities

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import io.demars.stellarwallet.PinPage
import junit.framework.TestCase.assertEquals
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PinActivityTest {
    @get:Rule
    val rule = object : ActivityTestRule<PinActivity>(PinActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return PinActivity.newInstance(InstrumentationRegistry.getInstrumentation().context, null)
        }
    }

    @Test
    fun enter_with_pin_result_ok() {
        PinPage.onPageLoaded().proceedWithPin("1111")
        assertThat<Instrumentation.ActivityResult>(rule.activityResult, hasResultCode(Activity.RESULT_OK))
        assertEquals(PinActivity.getPinFromIntent(rule.activityResult.resultData), "1111")
    }

    @Test
    fun enter_with_pin_result_cancel() {
        PinPage.onPageLoaded().pressBack()
        assertThat<Instrumentation.ActivityResult>(rule.activityResult, hasResultCode(Activity.RESULT_CANCELED))
        assertEquals(PinActivity.getPinFromIntent(rule.activityResult.resultData), null)
    }
}
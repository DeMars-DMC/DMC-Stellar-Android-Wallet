package blockeq.com.stellarwallet.activities

import android.os.Bundle
import blockeq.com.stellarwallet.R
import blockeq.com.stellarwallet.utils.DiagnosticUtils
import kotlinx.android.synthetic.main.activity_diagnostic.*


class DiagnosticActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)

        setupUI()
    }

    fun setupUI() {
        deviceModelTextView.text = DiagnosticUtils.getDeviceName()
        androidVersionTextView.text = DiagnosticUtils.getAndroidVersion()
        localeTextView.text = DiagnosticUtils.getLocale()
        appVersionTextView.text = DiagnosticUtils.getAppVersion()
    }
}

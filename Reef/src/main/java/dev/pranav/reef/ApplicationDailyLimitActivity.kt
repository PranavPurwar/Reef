package dev.pranav.reef

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.pranav.reef.databinding.ActivityDailyLimitBinding
import dev.pranav.reef.util.AppLimits
import dev.pranav.reef.util.applyDefaults
import dev.pranav.reef.util.applyWindowInsets

class ApplicationDailyLimitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyLimitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        binding = ActivityDailyLimitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(binding.root)

        val packageName = intent.getStringExtra("package_name") ?: packageName
        val application = packageManager.getApplicationInfo(packageName, 0)

        binding.apply {
            appIcon.setImageDrawable(packageManager.getApplicationIcon(application))
            appName.text = packageManager.getApplicationLabel(application)

            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            picker.minValue = 1
            picker.maxValue = 240 // you don't fucking need more than this
            picker.value = 30

            finish.setOnClickListener {
                AppLimits.setLimit(packageName, picker.value)
                AppLimits.saveLimits()
                finishAfterTransition()
            }
            removeLimits.setOnClickListener {
                AppLimits.removeLimit(packageName)
                AppLimits.saveLimits()
                finishAfterTransition()
            }
        }
    }
}

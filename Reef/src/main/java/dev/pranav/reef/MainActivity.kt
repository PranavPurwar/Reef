package dev.pranav.reef

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.reef.accessibility.FocusModeService
import dev.pranav.reef.accessibility.getFormattedTime
import dev.pranav.reef.databinding.ActivityMainBinding
import dev.pranav.reef.intro.PurelyIntro
import dev.pranav.reef.util.AppLimits
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.applyDefaults
import dev.pranav.reef.util.applyWindowInsets
import dev.pranav.reef.util.isAccessibilityServiceEnabledForBlocker
import dev.pranav.reef.util.prefs
import dev.pranav.reef.util.showAccessibilityDialog


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var pendingFocusModeStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        applyWindowInsets(binding.root)

        setContentView(binding.root)

        addExceptions()
        AppLimits.loadLimits(this)

        if (prefs.getBoolean("first_run", true)) {
            startActivity(Intent(this, PurelyIntro::class.java))
        } else {
            if (FocusModeService.isRunning) {
                Log.d("MainActivity", "Starting timer activity")
                startActivity(Intent(this, TimerActivity::class.java).apply {
                    putExtra("left", getFormattedTime(prefs.getLong("focus_time", 10 * 60 * 1000)))
                })
            } else {
                prefs.edit().putBoolean("focus_mode", false).apply()
            }
        }

        binding.start.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.focus_mode))
                .setMessage(getString(R.string.focus_mode_description))
                .setPositiveButton(getString(R.string.common_continue)) { _, _ ->
                    if (isAccessibilityServiceEnabledForBlocker()) {
                        startActivity(Intent(this, TimerActivity::class.java))
                    } else {
                        pendingFocusModeStart = true
                        showAccessibilityDialog()
                    }
                }
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show()
        }

        binding.appUsage.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }

        binding.whitelistApps.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }
    }


    override fun onResume() {
        super.onResume()
        if (pendingFocusModeStart && isAccessibilityServiceEnabledForBlocker()) {
            pendingFocusModeStart = false
            startActivity(Intent(this, TimerActivity::class.java))
        }
    }

    private fun addExceptions() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        packageManager.queryIntentActivities(intent, 0).forEach {
            val packageName = it.activityInfo.packageName
            if (!Whitelist.isWhitelisted(packageName)) {
                Whitelist.whitelist(packageName)
            }
        }
    }
}

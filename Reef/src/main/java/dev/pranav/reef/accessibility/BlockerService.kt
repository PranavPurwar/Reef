package dev.pranav.reef.accessibility

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.pranav.reef.R
import dev.pranav.reef.TimerActivity
import dev.pranav.reef.util.AppLimits
import dev.pranav.reef.util.CHANNEL_ID
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.isPrefsInitialized
import dev.pranav.reef.util.prefs

class BlockerService : AccessibilityService() {
    private val notificationId = 2

    override fun onServiceConnected() {
        if (!isPrefsInitialized) {
            prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
        ) {
            val packageName = event.packageName?.toString() ?: return

            if (Whitelist.isWhitelisted(packageName) || packageName == "dev.pranav.reef") {
                return
            }

            if (prefs.getBoolean("focus_mode", false)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                showBlockingNotification(packageName)
                return
            }

            if (AppLimits.hasLimit(packageName)) {
                val usageTime = AppLimits.getUsageTime(packageName, getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)
                val limit = AppLimits.getLimit(packageName)

                if (usageTime >= limit) {
                    showTimeLimitNotification(packageName)

                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    private fun showBlockingNotification(packageName: String) {
        val appName =
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Distraction Blocked")
            .setSmallIcon(R.drawable.round_hourglass_disabled_24)
            .setContentText("You were using $appName")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        } else {
            Log.w("BlockerService", "Missing notification permission")
        }
    }


    private fun showTimeLimitNotification(packageName: String) {
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val intent = Intent(this, TimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$appName blocked for exceeding limit")
            .setSmallIcon(R.drawable.round_hourglass_disabled_24)
            .setContentText(
                "You've used $appName for " + getFormattedTime(
                    AppLimits.getLimit(
                        packageName
                    )
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've used $appName for " + getFormattedTime(AppLimits.getLimit(packageName)))
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1, builder.build())
        } else {
            Log.w("AppLimitService", "Missing notification permission")
        }
    }

    override fun onInterrupt() {
        Log.d("BlockerService", "Accessibility service interrupted")
    }
}

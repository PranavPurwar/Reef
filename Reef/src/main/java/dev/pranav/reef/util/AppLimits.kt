package dev.pranav.reef.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

object AppLimits {
    private lateinit var sharedPreferences: SharedPreferences
    private val appLimits = mutableMapOf<String, Long>()
    private lateinit var usageStatsManager: UsageStatsManager

    fun setLimit(packageName: String, limit: Int) {
        appLimits[packageName] = limit * 60 * 1000L
    }

    fun getLimit(packageName: String): Long {
        return appLimits[packageName]!!
    }

    fun removeLimit(packageName: String) {
        appLimits.remove(packageName)
    }

    fun clearLimits() {
        appLimits.clear()
    }

    fun getLimits(): Map<String, Long> {
        return appLimits.toMap()
    }

    fun hasLimit(packageName: String): Boolean {
        return appLimits.containsKey(packageName)
    }
    fun getUsageTime(packageName: String, usageStatsManager: UsageStatsManager): Long {
        return getUsageStats(usageStatsManager).find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun getRawUsageStats(usageStatsManager: UsageStatsManager): List<UsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.systemDefault())
            .toInstant()
            .toEpochMilli()

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
    }

    fun getUsageStats(usageStatsManager: UsageStatsManager): List<AppUsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.systemDefault())
            .toInstant()
            .toEpochMilli()

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ).map { AppUsageStats(it.packageName, it.totalTimeInForeground) }
            .groupBy { it.packageName }
            .map { (_, statsList) ->
                statsList.reduce { acc, stats ->
                    acc.apply { totalTimeInForeground += stats.totalTimeInForeground }
                }
            }.sortedByDescending { it.totalTimeInForeground }
    }

    data class AppUsageStats(val packageName: String, var totalTimeInForeground: Long)

    fun saveLimits() {
        sharedPreferences.edit().apply {
            appLimits.forEach { (packageName, limit) ->
                putLong(packageName, limit)
            }
            apply()
        }
    }

    fun loadLimits(context: Context) {
        usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        sharedPreferences = context.getSharedPreferences("app_limits", Context.MODE_PRIVATE).apply {
            all.forEach { (packageName, limit) ->
                if (limit is Long) {
                    appLimits[packageName as String] = limit
                }
            }
        }
    }
}

object Whitelist {
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)

        if (sharedPreferences.all.isEmpty()) {
            whitelistAll(allowedApps)
        }
    }

    fun isWhitelisted(packageName: String): Boolean {
        return sharedPreferences.getBoolean(packageName, false)
    }

    fun whitelist(packageName: String) {
        sharedPreferences.edit().putBoolean(packageName, true).apply()
    }

    fun whitelistAll(set: Set<String>) {
        set.forEach { whitelist(it) }
    }

    fun unwhitelist(packageName: String) {
        sharedPreferences.edit().putBoolean(packageName, false).apply()
    }

    fun load(context: Context) {
        sharedPreferences = context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)
    }

    val allowedApps = hashSetOf(
        "com.android.systemui",
        "com.android.settings",
        "dev.pranav.reef",
        "com.android.calculator2",
        "com.android.dialer",
        "com.android.contacts",
        "com.android.mms",
        "com.android.phone",
        "com.android.camera",
        "com.android.camera2",
        "com.google.android.dialer",
        "com.google.android.contacts",
        "com.google.android.apps.messaging",
        "com.google.android.deskclock",
        "com.google.android.calendar",
        "com.google.android.keep",

        "com.google.android.apps.docs",
        "com.google.android.apps.drive",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.google.android.apps.maps",
        "com.google.android.apps.photos",
        "com.google.android.apps.photosgo",
        "com.google.android.apps.authenticator2",
        "com.google.android.apps.paidtasks",
        "com.google.android.apps.docs.editor.docs",
        "com.google.android.apps.docs.editor.sheets",
        "com.google.android.apps.classroom",
        "com.google.android.apps.giant",
        "com.google.android.apps.tachyon",
        "com.google.android.webview",
        "com.google.android.packageinstaller",
        "com.google.android.gms",

        "net.osmand",
        "com.fsck.k9",
        "com.google.android.inputmethod.latin",
        "com.google.android.apps.wellbeing",
        "com.android.documentsui",
        "bin.mt.plus.canary",
        "com.sadellie.calculator",
        "com.lineageos.aperture.dev",
        "com.lineageos.aperture",
        "com.shazam.android",
        "dev.patrickgold.florisboard",
        "dev.patrickgold.florisboard.debug",
        "com.synapsetech.compass",
        "hr.dtekac.prognoza",
        "me.jmh.authenticatorpro",

        "com.google.android.apps.nexuslauncher",
        "com.microsoft.launcher",
        "net.oneplus.launcher",
        "com.samsung.android.app.launcher",
        "com.android.launcher3",
        "app.lawnchair",
        "app.lawnchair.debug",

        "com.microsoft.office.officehubrow",
        "com.slack",
        "com.google.android.gm",
        "com.google.android.apps.meet",
        "com.microsoft.teams",
        "com.figma.mirror",
        "com.paypal.android.p2pmobile",
        "com.google.android.apps.chromecast.app",
        "com.linkedin.android",
        "com.adobe.lightroom",
        "com.google.android.apps.nbu.paisa.user",
        "com.whatsapp",
    )
}

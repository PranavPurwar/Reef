package dev.pranav.reef

import android.annotation.SuppressLint
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.material.snackbar.Snackbar
import dev.pranav.reef.databinding.ActivityUsageBinding
import dev.pranav.reef.databinding.AppUsageItemBinding
import dev.pranav.reef.util.AppLimits
import dev.pranav.reef.util.applyDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppUsageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUsageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        binding = ActivityUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val appUsageStats = AppLimits.getUsageStats(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)

        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

        val filteredAppUsageStats =
            appUsageStats.asSequence()
                .takeWhile { it.totalTimeInForeground > 5 * 1000 }.map { stats ->
                    Stats(
                        launcherApps.getApplicationInfo(
                            stats.packageName, 0, Process.myUserHandle()
                        ), stats
                    )
                }.toList()

        val adapter = AppUsageAdapter(filteredAppUsageStats)
        binding.appUsageRecyclerView.apply {
            addItemDecoration(
                DividerItemDecoration(
                    context, OrientationHelper.VERTICAL
                )
            )
            this.adapter = adapter
        }

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            val appUsageStats = AppLimits.getUsageStats(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)

            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

            val filteredAppUsageStats =
                appUsageStats.asSequence()
                    .takeWhile { it.totalTimeInForeground > 5 * 1000 }.map { stats ->
                        val applicationInfo = launcherApps.getApplicationInfo(
                            stats.packageName, 0, Process.myUserHandle()
                        )
                        Stats(applicationInfo, stats)
                    }.toList()

            launch(Dispatchers.Main) {
                val adapter = binding.appUsageRecyclerView.adapter as AppUsageAdapter
                adapter.updateData(filteredAppUsageStats)
            }
        }

        binding.adView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.adView.pause()
    }

    inner class AppUsageViewHolder(private val binding: AppUsageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: Stats, packageManager: PackageManager) {
            binding.appIcon.setImageDrawable(stats.applicationInfo.loadIcon(packageManager))
            binding.appName.text = stats.applicationInfo.loadLabel(packageManager)
            binding.appUsage.text = formatTime(stats.usageStats.totalTimeInForeground)

            binding.root.setOnClickListener {
                if (stats.applicationInfo.packageName == packageName) {
                    Snackbar.make(
                        binding.root, "Cannot set limit for Reef", Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                val intent = Intent(
                    this@AppUsageActivity, ApplicationDailyLimitActivity::class.java
                ).apply {
                    putExtra("package_name", stats.applicationInfo.packageName)
                }
                startActivity(intent)
            }
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = timeInMillis / (1000 * 60 * 60)
        val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeInMillis % (1000 * 60)) / 1000

        return when {
            hours > 0 -> "$hours hr" + (if (minutes > 0) " $minutes mins" else "") + (if (seconds > 0) " $seconds secs" else "")
            minutes > 0 -> "$minutes mins" + (if (seconds > 0) " $seconds secs" else "")
            else -> "$seconds secs"
        }
    }

    data class Stats(val applicationInfo: ApplicationInfo, val usageStats: AppLimits.AppUsageStats)

    inner class AppUsageAdapter(private var appUsageStats: List<Stats>) :
        RecyclerView.Adapter<AppUsageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
            val binding = AppUsageItemBinding.inflate(layoutInflater, parent, false)
            return AppUsageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
            holder.bind(appUsageStats[position], packageManager)
        }

        fun updateData(newAppUsageStats: List<Stats>) {
            appUsageStats = newAppUsageStats
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = appUsageStats.size
    }
}

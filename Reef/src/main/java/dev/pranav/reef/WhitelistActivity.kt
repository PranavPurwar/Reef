package dev.pranav.reef

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.reef.databinding.ActivityWhitelistBinding
import dev.pranav.reef.databinding.AppItemBinding
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.applyDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WhitelistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhitelistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        binding = ActivityWhitelistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val progressView = layoutInflater.inflate(R.layout.progress_view, null)

        val alertDialog = MaterialAlertDialogBuilder(
            this
        ).setCancelable(false).setView(progressView).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

            val apps = launcherApps.getActivityList(null, android.os.Process.myUserHandle()).mapNotNull {
                it.applicationInfo
            }.filter {
                it.packageName != packageName
            }.sortedBy {
                it.loadLabel(packageManager).toString()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                val adapter = ApplicationAdapter(apps)
                binding.appsRecyclerView.adapter = adapter
                alertDialog.dismiss()
            }
        }


        binding.appsRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this, OrientationHelper.VERTICAL
            )
        )

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        binding.adView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.adView.pause()
    }

    inner class ApplicationViewHolder(private val binding: AppItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: ApplicationInfo, packageManager: PackageManager) {
            binding.appIcon.setImageDrawable(app.loadIcon(packageManager))
            binding.appName.text = app.loadLabel(packageManager)

            binding.checkbox.isChecked = Whitelist.isWhitelisted(app.packageName)

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Whitelist.whitelist(app.packageName)
                } else {
                    Whitelist.unwhitelist(app.packageName)
                }
            }
        }
    }


    inner class ApplicationAdapter(private var packages: List<ApplicationInfo>) :
        RecyclerView.Adapter<ApplicationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
            val binding = AppItemBinding.inflate(layoutInflater, parent, false)
            return ApplicationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
            holder.bind(packages[position], packageManager)
        }

        override fun getItemCount(): Int = packages.size
    }
}

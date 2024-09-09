package dev.pranav.reef

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dev.pranav.reef.accessibility.FocusModeService
import dev.pranav.reef.databinding.ActivityTimerBinding
import dev.pranav.reef.util.AndroidUtilities
import dev.pranav.reef.util.applyDefaults
import dev.pranav.reef.util.applyWindowInsets
import dev.pranav.reef.util.prefs

class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        binding = ActivityTimerBinding.inflate(layoutInflater)

        applyWindowInsets(binding.root)

        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                timerReceiver, IntentFilter("dev.pranav.reef.TIMER_UPDATED"), RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                timerReceiver,
                IntentFilter("dev.pranav.reef.TIMER_UPDATED")
            )
        }

        if (intent.hasExtra("left")) {
            val left = intent.getStringExtra("left")
            binding.timer.text = left
            binding.start.visibility = View.GONE
            binding.picker.visibility = View.GONE
            binding.timer.visibility = View.VISIBLE
        }
        binding.picker.minValue = 1
        binding.picker.maxValue = 180
        binding.picker.value = 1

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,
            getString(R.string.inter_ad_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                }
            })

        binding.start.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()

                        startService()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        super.onAdFailedToShowFullScreenContent(error)

                        startService()
                    }
                }

                mInterstitialAd?.show(this)
            } else {
                startService()
            }
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (FocusModeService.isRunning) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    startActivity(intent)
                } else {
                    finish()
                }
            }
        })

        binding.adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        binding.adView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.adView.pause()
    }

    private fun startService() {
        prefs.edit().apply {
            putBoolean("focus_mode", true)
            putLong("focus_time", binding.picker.value * 60 * 1000L)
            apply()
        }

        val intent = Intent(this, FocusModeService::class.java)
        startForegroundService(intent)
        binding.start.visibility = View.GONE
        binding.picker.visibility = View.GONE
        binding.timer.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timerReceiver)
        prefs.edit().putBoolean("focus_mode", false).apply()
    }

    private val timerReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val left = intent.getStringExtra("left")
            binding.timer.text = left

            if (left == "00:00") {
                binding.start.visibility = View.VISIBLE
                binding.picker.visibility = View.VISIBLE
                binding.timer.visibility = View.GONE
                val androidUtilities = AndroidUtilities()

                androidUtilities.vibrate(context, 500)
            }
        }
    }
}

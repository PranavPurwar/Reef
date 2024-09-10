package dev.pranav.reef

import android.app.Application
import com.google.android.material.color.DynamicColors
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        Whitelist.init(this)
    }
}

package dev.pranav.reef.util

import android.content.SharedPreferences

const val CHANNEL_ID = "reef"
lateinit var prefs: SharedPreferences

val isPrefsInitialized: Boolean
    get() = ::prefs.isInitialized

enum class Mode {
    EAST, MIDDLE, HARD
}

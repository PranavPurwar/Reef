package dev.pranav.reef.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.pranav.reef.R


fun Activity.showAccessibilityDialog() {
    if (!isAccessibilityServiceEnabledForBlocker()) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.accessibility_service_name)
            .setMessage(R.string.accessibility_service_description)
            .setPositiveButton(getString(R.string.agree)) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }.show()
    }
}

fun Activity.showUsageAccessDialog(onAgreeClick: () -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle(R.string.usage_access)
        .setMessage(R.string.usage_access_description)
        .setPositiveButton(getString(R.string.agree)) { _, _ ->
            onAgreeClick()
        }.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
            finish()
        }.show()
}

fun Context.isAccessibilityServiceEnabledForBlocker(): Boolean {
    val accessibilityServiceName = "$packageName/$packageName.accessibility.BlockerService"
    val enabledServices = Settings.Secure.getString(
        contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(accessibilityServiceName) == true
}


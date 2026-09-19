package com.lisa.app.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.lisa.app.service.ScreenContentAccessibilityService

object PermissionUtils {

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${ScreenContentAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        if (TextUtils.isEmpty(enabledServices)) return false

        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    fun hasAllPermissions(context: Context): Boolean =
        canDrawOverlays(context) && isAccessibilityServiceEnabled(context)
}

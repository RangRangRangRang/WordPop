package com.rang.wordpop.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Trạng thái các quyền cần cho WordPop (mục 2 trong spec).
 * Lưu ý: "Display over other apps" và "Mở cửa sổ mới khi chạy trong nền" trong spec
 * thực chất CÙNG là 1 quyền Android duy nhất: SYSTEM_ALERT_WINDOW (Settings.canDrawOverlays).
 * Android không có quyền runtime riêng cho "hiển thị trên màn hình khóa" — overlay
 * TYPE_APPLICATION_OVERLAY tự động có thể vẽ đè lên trên, miễn có quyền overlay này.
 */
data class PermissionStatus(
    val hasOverlay: Boolean,
    val hasBatteryExemption: Boolean,
    val hasNotification: Boolean
) {
    val allGranted: Boolean get() = hasOverlay && hasBatteryExemption && hasNotification
}

object PermissionHelper {

    fun check(context: Context): PermissionStatus = PermissionStatus(
        hasOverlay = hasOverlayPermission(context),
        hasBatteryExemption = hasBatteryOptimizationExemption(context),
        hasNotification = hasNotificationPermission(context)
    )

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasBatteryOptimizationExemption(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Chỉ Android 13+ (API 33) mới cần xin quyền POST_NOTIFICATIONS để hiện
     * notification của Foreground Service. Bản thấp hơn coi như luôn có sẵn.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun overlaySettingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    fun batteryOptimizationIntent(context: Context): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
}
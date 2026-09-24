package com.rang.wordpop.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.rang.wordpop.MainActivity
import com.rang.wordpop.util.PermissionHelper

/**
 * Quick Settings Tile: bật/tắt nhanh overlay service.
 * - State ACTIVE: service đang chạy.
 * - State INACTIVE: service dừng.
 * - Bấm tile khi chưa đủ quyền → mở app để user cấp quyền.
 *
 * Người dùng phải tự tay thêm tile vào Quick Settings panel
 * (kéo thanh từ trên xuống → Edit → kéo WordPop vào). Android không
 * cho app tự thêm tile.
 */
@RequiresApi(Build.VERSION_CODES.N)
class WordPopTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val status = PermissionHelper.check(this)
        if (!status.allGranted) {
            // Chưa đủ quyền → mở MainActivity để user cấp quyền
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        val serviceIntent = Intent(this, WordPopOverlayService::class.java)
        if (WordPopOverlayService.isRunning) {
            stopService(serviceIntent)
        } else {
            ContextCompat.startForegroundService(this, serviceIntent)
        }

        // Cập nhật state ngay (Service sẽ set isRunning trong onCreate/onDestroy).
        // Có thể trễ vài chục ms, nhưng hệ thống sẽ tự gọi onStartListening
        // khi panel mở lại, nên không cần delay thủ công.
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val running = WordPopOverlayService.isRunning
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "WordPop"
        tile.subtitle = if (running) "Đang bật" else "Đã tắt"
        tile.updateTile()
    }
}
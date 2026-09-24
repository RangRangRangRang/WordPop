package com.rang.wordpop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AlarmManager gọi tới đây đúng giờ đã đặt (mục 6 spec).
 * 1) Yêu cầu WordPopOverlayService hiện overlay thành ngữ ngay lập tức.
 * 2) Đặt lại alarm cho đúng giờ này vào ngày mai, chỉ nếu mốc giờ vẫn đang bật.
 */
class IdiomAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        if (scheduleId == -1L) return

        // BroadcastReceiver chỉ tồn tại rất ngắn, nên phải dùng goAsync() để
        // được phép chạy tiếp coroutine (query Room là hàm suspend) trước khi
        // hệ thống có thể hủy tiến trình.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = SettingsRepository(context.applicationContext)
                if (!settingsRepo.idiomFeatureEnabled.first()) return@launch

                val db = AppDatabase.getInstance(context.applicationContext)
                val schedule = db.idiomScheduleDao().getById(scheduleId)
                if (schedule != null && schedule.enabled) {
                    val showIntent = Intent(context, WordPopOverlayService::class.java).apply {
                        action = WordPopOverlayService.ACTION_SHOW_IDIOM
                    }
                    ContextCompat.startForegroundService(context, showIntent)

                    AlarmScheduler.schedule(context.applicationContext, scheduleId, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

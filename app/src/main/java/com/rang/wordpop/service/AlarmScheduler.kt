package com.rang.wordpop.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Mỗi mốc giờ thành ngữ tương ứng 1 alarm riêng (mục 6 spec), dùng
 * setExactAndAllowWhileIdle để bắn đúng giờ kể cả khi máy đang ở chế độ Doze.
 * requestCode = scheduleId (ép về Int) để mỗi mốc giờ có PendingIntent riêng biệt.
 */
object AlarmScheduler {

    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    /**
     * @return true nếu đặt báo thức thành công, false nếu thiếu quyền exact alarm
     * (thường do người dùng chưa cấp quyền "Alarms & reminders" trong Settings).
     */
    fun schedule(context: Context, scheduleId: Long, hour: Int, minute: Int): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerMillis(hour, minute)
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                buildPendingIntent(context, scheduleId, hour, minute)
            )
            true
        } catch (e: SecurityException) {
            // Chưa được cấp quyền exact alarm (Android 12+). Không crash app,
            // chỉ báo lại false để UI nhắc người dùng cấp quyền.
            false
        }
    }

    fun cancel(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, scheduleId, 0, 0))
    }

    private fun buildPendingIntent(
        context: Context,
        scheduleId: Long,
        hour: Int,
        minute: Int
    ): PendingIntent {
        val intent = Intent(context, IdiomAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}

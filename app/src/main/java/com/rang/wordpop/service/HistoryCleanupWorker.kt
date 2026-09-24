package com.rang.wordpop.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rang.wordpop.data.AppDatabase
import java.util.Calendar

/**
 * Worker xóa các row history_today có viewedAt < startOfDay(hôm nay).
 * Chạy:
 *  - Định kỳ mỗi 24h lúc 00:05 (qua HistoryCleanupScheduler).
 *  - One-shot catch-up mỗi lần mở app, phòng khi periodic bị miss
 *    do tắt máy / Doze mode.
 */
class HistoryCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            db.historyTodayDao().clearOlderThan(startOfDayMillis())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun startOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
package com.rang.wordpop.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Lên lịch chạy HistoryCleanupWorker.
 * Gọi 1 lần trong MainActivity.onCreate() (hoặc Application.onCreate()).
 */
object HistoryCleanupScheduler {

    private const val PERIODIC_WORK_NAME = "history_cleanup_periodic"
    private const val CATCHUP_WORK_NAME = "history_cleanup_catchup"

    /**
     * - Periodic: chạy mỗi 24h, bắt đầu từ 00:05 sáng mai.
     *   Dùng KEEP để không reset lại lịch mỗi lần mở app.
     * - Catch-up: chạy ngay 1 lần khi mở app, dọn dẹp các row cũ
     *   nếu periodic bị miss do tắt máy / Doze.
     */
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Delay đến 00:05 sáng mai
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelayMs = next.timeInMillis - now.timeInMillis

        val periodicRequest = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        // Catch-up one-shot mỗi lần app mở
        val catchUpRequest = OneTimeWorkRequestBuilder<HistoryCleanupWorker>().build()
        workManager.enqueueUniqueWork(
            CATCHUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            catchUpRequest
        )
    }
}
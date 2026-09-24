package com.rang.wordpop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AlarmManager mất hết alarm đã đặt sau khi máy khởi động lại. Nhận
 * BOOT_COMPLETED để đặt lại toàn bộ mốc giờ đang bật (mục 6 spec, mục 2 quyền).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = SettingsRepository(context.applicationContext)
                if (!settingsRepo.idiomFeatureEnabled.first()) return@launch

                val db = AppDatabase.getInstance(context.applicationContext)
                db.idiomScheduleDao().getAllEnabled().forEach { schedule ->
                    AlarmScheduler.schedule(
                        context.applicationContext,
                        schedule.id,
                        schedule.hour,
                        schedule.minute
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

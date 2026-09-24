package com.rang.wordpop.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.IdiomSchedule
import com.rang.wordpop.data.SettingsRepository
import com.rang.wordpop.service.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

private val ALL_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val selectedLevels by settingsRepo.selectedLevels.collectAsState(initial = setOf("B2"))
    var wordCounts by remember { mutableStateOf(mapOf<String, Int>()) }

    val wordCardEnabled by settingsRepo.wordCardEnabled.collectAsState(initial = true)
    val idiomFeatureEnabled by settingsRepo.idiomFeatureEnabled.collectAsState(initial = false)
    val idiomSchedules by remember { db.idiomScheduleDao().observeAll() }
        .collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        wordCounts = ALL_LEVELS.associateWith { level -> db.wordBankDao().countByLevel(level) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Cài đặt",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // ===== Nút gạt bật/tắt word card khi mở khóa máy =====
        Spacer(Modifier.padding(top = 16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hiện thẻ khi mở khóa máy",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tắt khi chơi game hoặc cần mở máy liên tục. " +
                                "Bật lại bất cứ lúc nào, không ảnh hưởng các quyền đã cấp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = wordCardEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsRepo.setWordCardEnabled(enabled) }
                    }
                )
            }
        }

        // ===== Chọn cấp độ =====
        Text(
            text = "Chọn cấp độ:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        ALL_LEVELS.forEach { level ->
            val checked = selectedLevels.contains(level)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        val updated = if (isChecked) selectedLevels + level else selectedLevels - level
                        scope.launch { settingsRepo.setSelectedLevels(updated) }
                    }
                )
                Text(text = level, modifier = Modifier.padding(start = 4.dp))
                Text(
                    text = "  (${wordCounts[level] ?: 0} từ)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // --- Tickbox bật/tắt tổng cho cả tính năng thành ngữ ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = idiomFeatureEnabled,
                onCheckedChange = { isEnabled ->
                    scope.launch {
                        settingsRepo.setIdiomFeatureEnabled(isEnabled)
                        idiomSchedules.filter { it.enabled }.forEach { s ->
                            if (isEnabled) {
                                AlarmScheduler.schedule(context, s.id, s.hour, s.minute)
                            } else {
                                AlarmScheduler.cancel(context, s.id)
                            }
                        }
                    }
                }
            )
            Text(
                text = "Thành ngữ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // --- Phần chọn giờ, mờ đi và không bấm được khi tính năng đang tắt ---
        Column(modifier = Modifier.alpha(if (idiomFeatureEnabled) 1f else 0.4f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cài đặt giờ",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    enabled = idiomFeatureEnabled,
                    onClick = {
                        requestExactAlarmPermissionIfNeeded(context, isFirstSchedule = idiomSchedules.isEmpty())
                        showTimePicker(context) { hour, minute ->
                            scope.launch {
                                val existing = db.idiomScheduleDao().countAtTime(hour, minute)
                                if (existing > 0) {
                                    Toast.makeText(context, "Đã có mốc giờ này rồi", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val newSchedule = IdiomSchedule(hour = hour, minute = minute, enabled = true)
                                val id = db.idiomScheduleDao().insert(newSchedule)
                                val ok = AlarmScheduler.schedule(context, id, hour, minute)
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "Chưa có quyền đặt báo thức chính xác, vào Settings cấp quyền rồi thử lại",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("+")
                }
            }

            if (idiomSchedules.isEmpty()) {
                Text(
                    text = "Chưa có mốc giờ nào. Bấm \"+\" để thêm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                idiomSchedules.forEach { schedule ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Click vào cả thanh (trừ Switch và nút X) để mở time picker.
                                .clickable(enabled = idiomFeatureEnabled) {
                                    showTimePicker(
                                        context,
                                        initialHour = schedule.hour,
                                        initialMinute = schedule.minute
                                    ) { hour, minute ->
                                        scope.launch {
                                            val existing = db.idiomScheduleDao()
                                                .countAtTimeExcluding(hour, minute, schedule.id)
                                            if (existing > 0) {
                                                Toast.makeText(
                                                    context,
                                                    "Đã có mốc giờ này rồi",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }
                                            AlarmScheduler.cancel(context, schedule.id)
                                            val updated = schedule.copy(hour = hour, minute = minute)
                                            db.idiomScheduleDao().update(updated)
                                            if (updated.enabled) {
                                                val ok = AlarmScheduler.schedule(context, updated.id, hour, minute)
                                                if (!ok) {
                                                    Toast.makeText(
                                                        context,
                                                        "Chưa có quyền đặt báo thức chính xác, vào Settings cấp quyền rồi thử lại",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime12h(schedule.hour, schedule.minute),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                enabled = idiomFeatureEnabled,
                                checked = schedule.enabled,
                                onCheckedChange = { isEnabled ->
                                    val updated = schedule.copy(enabled = isEnabled)
                                    scope.launch { db.idiomScheduleDao().update(updated) }
                                    if (isEnabled) {
                                        AlarmScheduler.schedule(context, schedule.id, schedule.hour, schedule.minute)
                                    } else {
                                        AlarmScheduler.cancel(context, schedule.id)
                                    }
                                }
                            )
                            IconButton(
                                enabled = idiomFeatureEnabled,
                                onClick = {
                                    scope.launch { db.idiomScheduleDao().delete(schedule) }
                                    AlarmScheduler.cancel(context, schedule.id)
                                }
                            ) {
                                Text("✕")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime12h(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    return "%d:%02d %s".format(hour12, minute, amPm)
}

private fun showTimePicker(
    context: Context,
    initialHour: Int? = null,
    initialMinute: Int? = null,
    onTimeSelected: (Int, Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    val hour = initialHour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val minute = initialMinute ?: calendar.get(Calendar.MINUTE)
    TimePickerDialog(
        context,
        { _, h, m -> onTimeSelected(h, m) },
        hour,
        minute,
        false
    ).show()
}

private fun requestExactAlarmPermissionIfNeeded(context: Context, isFirstSchedule: Boolean) {
    if (!isFirstSchedule) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}
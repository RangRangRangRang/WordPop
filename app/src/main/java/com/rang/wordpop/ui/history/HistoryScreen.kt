package com.rang.wordpop.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.HistoryToday
import com.rang.wordpop.data.VocabularySaved
import com.rang.wordpop.util.TtsManager
import kotlinx.coroutines.launch
import java.util.Calendar

private val ALL_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
private val FILTER_ALL = "Tất cả"
private val FILTER_WORD = "Từ"
private val FILTER_IDIOM = "Thành ngữ"

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val startOfDay = remember { startOfDayMillis() }
    val historyFlow = remember { db.historyTodayDao().observeToday(startOfDay) }
    val historyList by historyFlow.collectAsState(initial = emptyList())

    var typeFilter by remember { mutableStateOf(FILTER_ALL) }
    var levelFilter by remember { mutableStateOf(FILTER_ALL) }
    var selectedEntry by remember { mutableStateOf<HistoryToday?>(null) }

    fun saveToNotebook(entry: HistoryToday) {
        scope.launch {
            db.vocabularySavedDao().insert(
                VocabularySaved(
                    type = entry.type,
                    text = entry.text,
                    pronunciation = entry.pronunciation,
                    meanings = entry.meanings,
                    example = entry.example,
                    level = entry.level,
                    savedAt = System.currentTimeMillis()
                )
            )
        }
    }

    val filteredList = historyList.filter { entry ->
        val matchType = when (typeFilter) {
            FILTER_WORD -> entry.type == "word"
            FILTER_IDIOM -> entry.type == "idiom"
            else -> true
        }
        val matchLevel = levelFilter == FILTER_ALL || entry.level == levelFilter
        matchType && matchLevel
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Lịch sử hôm nay",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(FILTER_ALL, FILTER_WORD, FILTER_IDIOM).forEach { option ->
                FilterChip(
                    selected = typeFilter == option,
                    onClick = { typeFilter = option },
                    label = { Text(option) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf(FILTER_ALL) + ALL_LEVELS).forEach { option ->
                FilterChip(
                    selected = levelFilter == option,
                    onClick = { levelFilter = option },
                    label = { Text(option) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Chưa có mục nào trong hôm nay.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredList, key = { it.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { selectedEntry = entry },
                        onSave = { saveToNotebook(entry) }
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        VocabDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onSpeak = { TtsManager.speak(entry.text) },
            onSave = {
                saveToNotebook(entry)
                selectedEntry = null
            }
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryToday, onClick: () -> Unit, onSave: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(entry.level)
                        append(" • ")
                        append(if (entry.type == "word") "Từ" else "Thành ngữ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.meanings.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Lưu vào sổ",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VocabDetailDialog(
    entry: HistoryToday,
    onDismiss: () -> Unit,
    onSpeak: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Phát âm"
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Lưu vào sổ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (entry.pronunciation.isNotBlank()) {
                    Text(
                        text = entry.pronunciation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${entry.level} • ${if (entry.type == "word") "Từ" else "Thành ngữ"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                entry.meanings.forEachIndexed { index, meaning ->
                    Text(
                        text = "${index + 1}. $meaning",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                }
                entry.example?.let { example ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
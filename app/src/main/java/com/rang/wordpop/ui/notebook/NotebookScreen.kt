package com.rang.wordpop.ui.notebook

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.VocabularySaved
import com.rang.wordpop.util.TtsManager
import kotlinx.coroutines.launch

private val ALL_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
private const val FILTER_ALL = "Tất cả"
private const val FILTER_WORD = "Từ"
private const val FILTER_IDIOM = "Thành ngữ"
private const val FILTER_STAR = "⭐"

@Composable
fun NotebookScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val savedList by remember { db.vocabularySavedDao().observeAll() }
        .collectAsState(initial = emptyList())

    var typeFilter by remember { mutableStateOf(FILTER_ALL) }
    var levelFilter by remember { mutableStateOf(FILTER_ALL) }
    var selectedEntry by remember { mutableStateOf<VocabularySaved?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun deleteWithUndo(entry: VocabularySaved) {
        scope.launch {
            db.vocabularySavedDao().delete(entry)
            val result = snackbarHostState.showSnackbar(
                message = "Đã xóa \"${entry.text}\"",
                actionLabel = "Hoàn tác",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                db.vocabularySavedDao().insert(entry)
            }
        }
    }

    fun toggleStar(entry: VocabularySaved) {
        scope.launch {
            db.vocabularySavedDao().setStarred(entry.id, !entry.isStarred)
        }
    }

    val filteredList = savedList.filter { entry ->
        val matchType = when (typeFilter) {
            FILTER_WORD -> entry.type == "word"
            FILTER_IDIOM -> entry.type == "idiom"
            FILTER_STAR -> entry.isStarred
            else -> true
        }
        val matchLevel = levelFilter == FILTER_ALL || entry.level == levelFilter
        matchType && matchLevel
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Sổ từ vựng",
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
                listOf(FILTER_ALL, FILTER_WORD, FILTER_IDIOM, FILTER_STAR).forEach { option ->
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
                        text = if (typeFilter == FILTER_STAR) "Chưa có từ nào được đánh dấu ⭐"
                        else "Sổ từ vựng đang trống",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredList, key = { it.id }) { entry ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    deleteWithUndo(entry)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 32.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Xóa",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            NotebookRow(
                                entry = entry,
                                onClick = { selectedEntry = entry },
                                onDelete = { deleteWithUndo(entry) },
                                onToggleStar = { toggleStar(entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        NotebookDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onSpeak = { TtsManager.speak(entry.text) },
            onDelete = {
                deleteWithUndo(entry)
                selectedEntry = null
            },
            onToggleStar = { toggleStar(entry) }
        )
    }
}

@Composable
private fun NotebookRow(
    entry: VocabularySaved,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit
) {
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
                if (entry.pronunciation.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.pronunciation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.meanings.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (entry.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (entry.isStarred) "Bỏ yêu thích" else "Yêu thích",
                    tint = if (entry.isStarred) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun NotebookDetailDialog(
    entry: VocabularySaved,
    onDismiss: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit
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
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Phát âm")
                    }
                    IconButton(onClick = onToggleStar) {
                        Icon(
                            imageVector = if (entry.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Yêu thích",
                            tint = if (entry.isStarred) Color(0xFFFFC107)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error
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
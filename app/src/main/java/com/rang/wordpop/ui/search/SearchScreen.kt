package com.rang.wordpop.ui.search

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.rang.wordpop.data.IdiomBank
import com.rang.wordpop.data.VocabularySaved
import com.rang.wordpop.data.WordBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SearchResultItem(
    val id: Long,
    val type: String,
    val text: String,
    val pronunciation: String,
    val meanings: List<String>,
    val example: String?,
    val level: String
)

private fun WordBank.toSearchResult() = SearchResultItem(
    id = id,
    type = "word",
    text = word,
    pronunciation = pronunciation,
    meanings = meanings,
    example = null,
    level = level
)

private fun IdiomBank.toSearchResult() = SearchResultItem(
    id = id,
    type = "idiom",
    text = idiom,
    pronunciation = "",
    meanings = listOf(meaningVi),
    example = exampleEn.takeIf { it.isNotBlank() },
    level = level
)

@Composable
fun SearchResultsScreen(
    query: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf(emptyList<SearchResultItem>()) }
    var loading by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SearchResultItem?>(null) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(250)
        val words = db.wordBankDao().search(trimmed).map { it.toSearchResult() }
        val idioms = db.idiomBankDao().search(trimmed).map { it.toSearchResult() }
        results = (words + idioms).sortedBy { it.text }
        loading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            query.trim().length < 2 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nhập ít nhất 2 ký tự để tìm...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Không tìm thấy \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it.type + "_" + it.id }) { item ->
                        SearchResultRow(
                            item = item,
                            onOpenDetail = { selectedItem = item },
                            onSave = {
                                scope.launch {
                                    db.vocabularySavedDao().insert(
                                        VocabularySaved(
                                            type = item.type,
                                            text = item.text,
                                            pronunciation = item.pronunciation,
                                            meanings = item.meanings,
                                            example = item.example,
                                            level = item.level,
                                            savedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        selectedItem?.let { item ->
            SearchDetailDialog(
                item = item,
                onDismiss = { selectedItem = null },
                onSave = {
                    scope.launch {
                        db.vocabularySavedDao().insert(
                            VocabularySaved(
                                type = item.type,
                                text = item.text,
                                pronunciation = item.pronunciation,
                                meanings = item.meanings,
                                example = item.example,
                                level = item.level,
                                savedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            )
        }
    }
}

/**
 * Fix UI: level + loại gộp thành 1 Text, không dùng Row nhiều Text
 * để tránh wrap từng ký tự xuống dòng.
 */
@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onSave: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpenDetail)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(item.level)
                        append(" • ")
                        append(if (item.type == "idiom") "Thành ngữ" else "Từ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.pronunciation.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.pronunciation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.meanings.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.meanings.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onSave) {
                Text("Lưu")
            }
        }
    }
}

@Composable
private fun SearchDetailDialog(
    item: SearchResultItem,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.shutdown() }
    }

    var saved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, "search_tts")
                    }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Phát âm")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(item.level)
                        append(" • ")
                        append(if (item.type == "idiom") "Thành ngữ" else "Từ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.pronunciation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.pronunciation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                item.meanings.forEachIndexed { index, meaning ->
                    Text(
                        text = "${index + 1}. $meaning",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (!item.example.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "\"${item.example}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đóng")
                    }
                    Spacer(Modifier.width(12.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            onSave()
                            saved = true
                        },
                        enabled = !saved,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (saved) "Đã lưu ✓" else "Lưu vào sổ")
                    }
                }
            }
        }
    }
}
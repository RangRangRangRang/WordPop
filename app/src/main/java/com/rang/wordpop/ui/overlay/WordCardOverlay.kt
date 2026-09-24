package com.rang.wordpop.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Thẻ hiện đè lên màn hình khóa/màn hình chính. Dùng chung cho cả
 * từ vựng (mục 3) lẫn thành ngữ (mục 6) trong spec.
 */
@Composable
fun WordCardOverlay(
    word: String,
    pronunciation: String,
    meanings: List<String>,
    onSpeak: () -> Unit,
    onDone: () -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean = false,
    literalHint: String? = null,
    example: String? = null,
    exampleTranslation: String? = null
) {
    Box(
        modifier = Modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Phát âm"
                        )
                    }
                    IconButton(onClick = onSave, enabled = !isSaved) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Đã lưu" else "Lưu vào sổ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (pronunciation.isNotBlank()) {
                    Text(
                        text = pronunciation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
                meanings.forEachIndexed { index, meaning ->
                    Text(
                        text = "${index + 1}. $meaning",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (!literalHint.isNullOrBlank()) {
                    Text(
                        text = "Nghĩa gốc: $literalHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (!example.isNullOrBlank()) {
                    Text(
                        text = "\"$example\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!exampleTranslation.isNullOrBlank()) {
                        Text(
                            text = exampleTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hoàn tất")
                }
            }
        }
    }
}
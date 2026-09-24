package com.rang.wordpop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sổ từ vựng: các từ/thành ngữ người dùng chủ động bấm "Lưu vào sổ từ vựng"
 * từ màn hình Lịch sử hôm nay. Lưu vĩnh viễn cho tới khi người dùng tự xóa.
 */
@Entity(tableName = "vocabulary_saved")
data class VocabularySaved(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "word", // "word" | "idiom"
    val text: String,
    val pronunciation: String,
    val meanings: List<String>,
    val example: String? = null,
    val level: String,
    val savedAt: Long, // epoch millis
    val isStarred: Boolean = false
)

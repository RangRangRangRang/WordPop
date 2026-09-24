package com.rang.wordpop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lịch sử từ/thành ngữ đã hiện overlay trong ngày hôm nay (history_today).
 * type phân biệt "word" (từ vựng) hay "idiom" (thành ngữ, làm ở mục 6 sau).
 * Bảng này tự làm rỗng vào đầu ngày mới (xem HistoryTodayDao.clearOlderThan
 * hoặc lọc trực tiếp bằng viewedAt khi hiển thị).
 */
@Entity(tableName = "history_today")
data class HistoryToday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "word", // "word" | "idiom"
    val itemId: Long, // tham chiếu WordBank.id hoặc IdiomBank.id tùy loại
    val text: String, // từ hoặc thành ngữ
    val pronunciation: String,
    val meanings: List<String>,
    val example: String? = null, // chỉ idiom mới có câu ví dụ
    val level: String,
    val viewedAt: Long // epoch millis, thời điểm bấm "Hoàn tất"
)

package com.rang.wordpop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ngân hàng từ vựng gốc (word_bank).
 * level dùng chuẩn CEFR: A1, A2, B1, B2, C1, C2.
 * Hiện tại chỉ seed dữ liệu mức B2, các mức khác thêm sau không cần đổi schema.
 */
@Entity(tableName = "word_bank")
data class WordBank(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val pronunciation: String, // phiên âm IPA, ví dụ "/əˈkɒmplɪʃ/"
    val meanings: List<String>, // đúng 3 nghĩa tiếng Việt
    val level: String // "A1".."C2"
)

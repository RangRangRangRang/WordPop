package com.rang.wordpop.data

import androidx.room.TypeConverter

/**
 * Room không lưu trực tiếp List<String> được, nên chuyển thành 1 chuỗi
 * nối bằng dấu phân cách "|||" (không xuất hiện trong nghĩa tiếng Việt bình thường).
 */
class Converters {
    @TypeConverter
    fun fromMeanings(meanings: List<String>): String = meanings.joinToString("|||")

    @TypeConverter
    fun toMeanings(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split("|||")
}

package com.rang.wordpop.data

import android.content.Context
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader

/**
 * Đọc file CSV trong assets/vocab/{level}.csv (ví dụ assets/vocab/a1.csv) và
 * chuyển thành List<WordBank>. Dùng cách này thay vì viết cứng từ trong code
 * Kotlin, để thêm/sửa/mở rộng hàng trăm-hàng nghìn từ chỉ cần sửa file CSV,
 * không cần đụng vào code hay build lại logic gì cả.
 *
 * Định dạng mỗi file CSV, dòng đầu là header (sẽ tự bỏ qua):
 * word,pronunciation,meaning1,meaning2,meaning3
 * - pronunciation, meaning2, meaning3 có thể để trống nếu chưa có dữ liệu.
 * - meaning1 bắt buộc phải có (là nghĩa chính).
 * - Field chứa dấu phẩy thì bọc trong dấu ngoặc kép "..." như CSV chuẩn.
 */
object CsvWordLoader {

    fun loadLevel(context: Context, level: String): List<WordBank> {
        val fileName = "vocab/${level.lowercase()}.csv"
        return try {
            context.assets.open(fileName).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    lines.drop(1) // bỏ dòng header
                        .filter { it.isNotBlank() }
                        .mapNotNull { line -> parseLine(line, level) }
                        .toList()
                }
            }
        } catch (e: FileNotFoundException) {
            // Chưa có file CSV cho cấp này -> coi như chưa có dữ liệu, không crash app.
            emptyList()
        }
    }

    private fun parseLine(line: String, level: String): WordBank? {
        val fields = splitCsvLine(line)
        if (fields.isEmpty()) return null

        val word = fields.getOrNull(0)?.trim().orEmpty()
        val pronunciation = fields.getOrNull(1)?.trim().orEmpty()
        val meanings = listOf(
            fields.getOrNull(2)?.trim().orEmpty(),
            fields.getOrNull(3)?.trim().orEmpty(),
            fields.getOrNull(4)?.trim().orEmpty()
        ).filter { it.isNotBlank() }

        if (word.isBlank() || meanings.isEmpty()) return null

        return WordBank(
            word = word,
            pronunciation = pronunciation,
            meanings = meanings,
            level = level
        )
    }

    /** Tách 1 dòng CSV theo dấu phẩy, có hỗ trợ field bọc trong dấu ngoặc kép. */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        for (c in line) {
            when {
                c == '"' -> insideQuotes = !insideQuotes
                c == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }
}

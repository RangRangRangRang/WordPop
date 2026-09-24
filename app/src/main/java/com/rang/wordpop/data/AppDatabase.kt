package com.rang.wordpop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WordBank::class,
        HistoryToday::class,
        VocabularySaved::class,
        IdiomBank::class,
        IdiomSchedule::class
    ],
    version = 4,                 // <-- bump từ 3 lên 4 (do thêm cột isStarred)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordBankDao(): WordBankDao
    abstract fun historyTodayDao(): HistoryTodayDao
    abstract fun vocabularySavedDao(): VocabularySavedDao
    abstract fun idiomBankDao(): IdiomBankDao
    abstract fun idiomScheduleDao(): IdiomScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wordpop_database"
                )
                    // Đang trong giai đoạn phát triển, schema còn có thể đổi.
                    // Khi đổi schema mà chưa viết Migration, cho phép Room xóa
                    // và tạo lại DB thay vì crash. Trước khi phát hành chính thức
                    // cần thay bằng Migration thật để không mất dữ liệu người dùng.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Seed dữ liệu ngay khi tạo database lần đầu (bảng rỗng).
                // Từ vựng đọc từ file CSV trong assets/vocab/, mỗi cấp seed
                // độc lập - cấp nào đã có dữ liệu thì bỏ qua, không ghi đè.
                CoroutineScope(Dispatchers.IO).launch {
                    val wordDao = instance.wordBankDao()
                    listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                        val csvWords = CsvWordLoader.loadLevel(context.applicationContext, level)
                        if (csvWords.isNotEmpty()) {
                            // So sánh với DB hiện có, CHỈ thêm từ mới (chưa tồn tại theo tên từ,
                            // không phân biệt hoa/thường) -> chạy an toàn mỗi lần mở app, không
                            // tạo trùng, không đụng tới từ cũ đã có trong Lịch sử/Sổ từ vựng.
                            val existing = wordDao.getWordsByLevel(level)
                                .mapTo(HashSet()) { it.lowercase() }
                            val newWords = csvWords.filter { it.word.lowercase() !in existing }
                            if (newWords.isNotEmpty()) wordDao.insertAll(newWords)
                        }
                    }
                    val idiomDao = instance.idiomBankDao()
                    if (idiomDao.count() == 0) {
                        idiomDao.insertAll(SeedIdiomData.idioms)
                    }
                }
                instance
            }
        }
    }
}
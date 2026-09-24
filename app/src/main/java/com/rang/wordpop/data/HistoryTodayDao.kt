package com.rang.wordpop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryTodayDao {

    @Insert
    suspend fun insert(entry: HistoryToday)

    /**
     * Flow để màn hình "Lịch sử hôm nay" tự cập nhật realtime khi có từ mới.
     * Truyền vào startOfDayMillis = mốc 00:00 hôm nay để chỉ lấy đúng từ trong ngày.
     */
    @Query("SELECT * FROM history_today WHERE viewedAt >= :startOfDayMillis ORDER BY viewedAt DESC")
    fun observeToday(startOfDayMillis: Long): Flow<List<HistoryToday>>

    /**
     * Dọn các bản ghi cũ hơn hôm nay. Gọi từ WorkManager job lúc 00:00
     * hoặc gọi mỗi khi mở màn hình Lịch sử hôm nay (mục 4 trong spec).
     */
    @Query("DELETE FROM history_today WHERE viewedAt < :startOfDayMillis")
    suspend fun clearOlderThan(startOfDayMillis: Long)
}
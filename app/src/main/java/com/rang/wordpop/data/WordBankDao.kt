package com.rang.wordpop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordBankDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordBank>)

    @Query("SELECT COUNT(*) FROM word_bank")
    suspend fun count(): Int

    /**
     * Lấy 1 từ ngẫu nhiên thuộc bất kỳ cấp nào trong [levels] mà CHƯA xuất hiện
     * trong history_today kể từ [startOfDayMillis]. Hỗ trợ chọn nhiều cấp cùng lúc
     * (mục 1, 7 trong spec).
     */
    @Query(
        """
        SELECT * FROM word_bank
        WHERE level IN (:levels)
        AND id NOT IN (
            SELECT itemId FROM history_today WHERE type = 'word' AND viewedAt >= :startOfDayMillis
        )
        ORDER BY RANDOM() LIMIT 1
        """
    )
    suspend fun getRandomUnseenWord(levels: List<String>, startOfDayMillis: Long): WordBank?

    /**
     * Hết từ chưa xem trong ngày ở các cấp đã chọn -> lặp lại ngẫu nhiên
     * (mục 3 bước 4 trong spec).
     */
    @Query("SELECT * FROM word_bank WHERE level IN (:levels) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWordAnyState(levels: List<String>): WordBank?

    /**
     * Đếm số từ có sẵn theo từng cấp, hiện ở màn hình Cài đặt (mục 7 spec).
     */
    @Query("SELECT COUNT(*) FROM word_bank WHERE level = :level")
    suspend fun countByLevel(level: String): Int

    /**
     * Lấy danh sách các từ (chỉ cột "word") đã có sẵn trong DB ở 1 cấp, dùng để
     * so sánh với CSV lúc khởi động app, chỉ thêm những từ MỚI (chưa có), không
     * đụng tới từ cũ -> không ảnh hưởng Lịch sử / Sổ từ vựng đã lưu.
     */
    @Query("SELECT word FROM word_bank WHERE level = :level")
    suspend fun getWordsByLevel(level: String): List<String>
    /**
     * Tìm kiếm tổng thể trong word_bank (mọi cấp độ đã tiêm vào), theo tên từ
     * HOẶC theo nghĩa tiếng Việt. Giới hạn 50 kết quả để không lag khi gõ.
     */
    @Query(
        """
        SELECT * FROM word_bank
        WHERE word LIKE '%' || :query || '%' OR meanings LIKE '%' || :query || '%'
        ORDER BY word LIMIT 50
        """
    )
    suspend fun search(query: String): List<WordBank>
}

package com.rang.wordpop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IdiomBankDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(idioms: List<IdiomBank>)

    @Query("SELECT COUNT(*) FROM idiom_bank")
    suspend fun count(): Int

    /**
     * Ưu tiên thành ngữ chưa xem trong ngày, thuộc cấp đã chọn (mục 6 spec).
     */
    @Query(
        """
        SELECT * FROM idiom_bank
        WHERE level IN (:levels)
        AND id NOT IN (
            SELECT itemId FROM history_today WHERE type = 'idiom' AND viewedAt >= :startOfDayMillis
        )
        ORDER BY RANDOM() LIMIT 1
        """
    )
    suspend fun getRandomUnseenIdiom(levels: List<String>, startOfDayMillis: Long): IdiomBank?

    /**
     * Không có thành ngữ nào ở cấp đã chọn -> random toàn bộ ngân hàng (mục 6 spec).
     */
    @Query("SELECT * FROM idiom_bank ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomIdiomAnyState(): IdiomBank?

    /** Tìm kiếm tổng thể trong idiom_bank, theo thành ngữ HOẶC nghĩa tiếng Việt. */
    @Query(
        """
        SELECT * FROM idiom_bank
        WHERE idiom LIKE '%' || :query || '%' OR meaningVi LIKE '%' || :query || '%'
        ORDER BY idiom LIMIT 50
        """
    )
    suspend fun search(query: String): List<IdiomBank>
}

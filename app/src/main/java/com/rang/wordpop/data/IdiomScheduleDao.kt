package com.rang.wordpop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IdiomScheduleDao {

    @Insert
    suspend fun insert(schedule: IdiomSchedule): Long

    @Update
    suspend fun update(schedule: IdiomSchedule)

    @Delete
    suspend fun delete(schedule: IdiomSchedule)

    @Query("SELECT * FROM idiom_schedule ORDER BY hour, minute")
    fun observeAll(): Flow<List<IdiomSchedule>>

    @Query("SELECT * FROM idiom_schedule WHERE id = :id")
    suspend fun getById(id: Long): IdiomSchedule?

    @Query("SELECT * FROM idiom_schedule WHERE enabled = 1")
    suspend fun getAllEnabled(): List<IdiomSchedule>

    /**
     * Kiểm tra trùng giờ trước khi thêm mốc mới (mục 6 spec: "Không cho thêm trùng giờ").
     */
    @Query("SELECT COUNT(*) FROM idiom_schedule WHERE hour = :hour AND minute = :minute")
    suspend fun countAtTime(hour: Int, minute: Int): Int

    /**
     * Giống countAtTime nhưng loại trừ chính mốc đang sửa, dùng khi chỉnh sửa
     * giờ của 1 mốc có sẵn (không tự báo trùng với chính nó).
     */
    @Query("SELECT COUNT(*) FROM idiom_schedule WHERE hour = :hour AND minute = :minute AND id != :excludeId")
    suspend fun countAtTimeExcluding(hour: Int, minute: Int, excludeId: Long): Int
}

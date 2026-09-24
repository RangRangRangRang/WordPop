package com.rang.wordpop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mốc giờ nhận thành ngữ do người dùng tự thêm (idiom_schedule, mục 6 spec).
 * Rỗng khi mới cài app - không có mốc mặc định nào.
 */
@Entity(tableName = "idiom_schedule")
data class IdiomSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

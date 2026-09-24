package com.rang.wordpop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ngân hàng thành ngữ (idiom_bank, mục 6 spec).
 * level là mức ước lượng khi seed dữ liệu vì thành ngữ thường không có
 * phân cấp CEFR chính thức (ghi chú giống spec).
 */
@Entity(tableName = "idiom_bank")
data class IdiomBank(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idiom: String,
    val meaningVi: String,
    val literalHint: String? = null,
    val exampleEn: String,
    val exampleVi: String,
    val level: String
)

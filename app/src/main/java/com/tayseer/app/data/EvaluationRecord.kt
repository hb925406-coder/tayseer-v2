package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EvaluationType(val labelAr: String, val isPositive: Boolean) {
    MEMORIZED("حفظه", true),
    NOT_MEMORIZED("لم يحفظ", false),
    DISTINGUISHED("مميز", true),
    POLITE("مؤدب", true),
    FORGOT_BOOKS("نسيان الكتب", false),
    CUSTOM("مخصص", true)
}

@Entity(
    tableName = "evaluation_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"]), Index(value = ["date"])]
)
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val type: EvaluationType,
    val note: String = ""
)

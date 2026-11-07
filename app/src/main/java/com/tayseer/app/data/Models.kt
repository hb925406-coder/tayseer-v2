package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceStatus(val label: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    ESCAPED("هارب")
}

enum class UserRole {
    ADMIN,
    TEACHER,
    PARENT
}

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val grade: String,
    val section: String,
    val academicYear: String = "1446-1447",
    val parentPhone: String = "",
    val isArchived: Boolean = false
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studentId: Long,
    val date: String,
    val statusEnum: AttendanceStatus = AttendanceStatus.PRESENT,
    val reasonNote: String = "",
    val isFollowedUp: Boolean = true,
    val followUpMethod: String = "",
    val followUpNote: String = ""
)

@Entity(tableName = "evaluation_records")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studentId: Long,
    val date: String,
    val title: String,
    val scoreType: String,
    val note: String = ""
)

@Entity(tableName = "custom_evaluations")
data class CustomEvaluation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val isPositive: Boolean = true
)

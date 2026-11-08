package com.tayseer.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus): String {
        return status.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus {
        return try {
            AttendanceStatus.valueOf(value)
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
    }

    @TypeConverter
    fun fromEvaluationType(type: EvaluationType): String {
        return type.name
    }

    @TypeConverter
    fun toEvaluationType(value: String): EvaluationType {
        return try {
            EvaluationType.valueOf(value)
        } catch (e: Exception) {
            EvaluationType.MEMORIZED
        }
    }
}

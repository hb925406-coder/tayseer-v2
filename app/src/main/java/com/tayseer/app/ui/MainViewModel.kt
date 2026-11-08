package com.tayseer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.EvaluationType
import com.tayseer.app.data.Student
import com.tayseer.app.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()
    private val evaluationDao = database.evaluationDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = MutableStateFlow(dateFormat.format(Date()))

    val currentUserRole = MutableStateFlow(UserRole.ADMIN)

    val allStudents: StateFlow<List<Student>> = studentDao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = attendanceDao.getPendingAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEvaluations: StateFlow<List<EvaluationRecord>> = evaluationDao.getEvaluationsByDate(dateFormat.format(Date()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyAttendance = MutableStateFlow>(emptyMap())
    val dailyAttendance: StateFlow> = _dailyAttendance.asStateFlow()

    fun updateStudentAttendance(studentId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            _dailyAttendance.value = _dailyAttendance.value.toMutableMap().apply {
                put(studentId, status)
            }
            val record = AttendanceRecord(
                studentId = studentId,
                date = currentDate.value,
                statusEnum = status,
                needsFollowUp = (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LATE || status == AttendanceStatus.ESCAPED)
            )
            attendanceDao.insertAttendance(record)
        }
    }

    fun markAllPresent(students: List<Student>) {
        viewModelScope.launch {
            val updatedMap = _dailyAttendance.value.toMutableMap()
            students.forEach { student ->
                updatedMap[student.id] = AttendanceStatus.PRESENT
                val record = AttendanceRecord(
                    studentId = student.id,
                    date = currentDate.value,
                    statusEnum = AttendanceStatus.PRESENT,
                    needsFollowUp = false
                )
                attendanceDao.insertAttendance(record)
            }
            _dailyAttendance.value = updatedMap
        }
    }

    fun addStudent(student: Student) {
        viewModelScope.launch {
            studentDao.insertStudent(student)
        }
    }

    fun addEvaluation(studentId: Long, type: EvaluationType, note: String = "") {
        viewModelScope.launch {
            val evaluation = EvaluationRecord(
                studentId = studentId,
                date = currentDate.value,
                type = type,
                note = note
            )
            evaluationDao.insertEvaluation(evaluation)
        }
    }

    fun deleteEvaluation(evaluation: EvaluationRecord) {
        viewModelScope.launch {
            evaluationDao.deleteEvaluation(evaluation)
        }
    }

    fun resolveAlert(recordId: Long, excuseType: String, note: String) {
        viewModelScope.launch {
            attendanceDao.resolveAlert(recordId, excuseType, note)
        }
    }
}

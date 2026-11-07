package com.tayseer.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.CustomEvaluation
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.data.TayseerRepository
import com.tayseer.app.data.UserRole
import com.tayseer.app.util.ExcelImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TayseerRepository

    val allStudents: StateFlow<List<Student>>
    val pendingAlerts: StateFlow<List<AttendanceRecord>>
    val customEvaluations: StateFlow<List<CustomEvaluation>>

    val currentUserRole: MutableStateFlow<UserRole> = MutableStateFlow(UserRole.ADMIN)
    val currentDate: MutableStateFlow<String> = MutableStateFlow(getTodayDateString())
    val selectedGrade: MutableStateFlow<String> = MutableStateFlow("الصف الأول")
    val selectedSection: MutableStateFlow<String> = MutableStateFlow("أ")
    val dailyAttendance: MutableStateFlow> = MutableStateFlow(emptyMap())

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = TayseerRepository(database)

        allStudents = repository.allStudents
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        pendingAlerts = repository.pendingAlerts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        customEvaluations = repository.allCustomEvaluations
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        loadAttendanceForCurrentDate()
    }

    fun setUserRole(role: UserRole) {
        currentUserRole.value = role
    }

    fun setGradeAndSection(grade: String, section: String) {
        selectedGrade.value = grade
        selectedSection.value = section
    }

    private fun loadAttendanceForCurrentDate() {
        viewModelScope.launch {
            repository.getAttendanceByDate(currentDate.value).collect { records ->
                val map = records.associate { it.studentId to it.statusEnum }
                dailyAttendance.value = map
            }
        }
    }

    fun updateStudentAttendance(studentId: Long, status: AttendanceStatus, note: String = "") {
        viewModelScope.launch {
            val isAlert = (status == AttendanceStatus.ABSENT || status == AttendanceStatus.ESCAPED || status == AttendanceStatus.LATE)
            val record = AttendanceRecord(
                studentId = studentId,
                date = currentDate.value,
                statusEnum = status,
                reasonNote = note,
                isFollowedUp = !isAlert
            )
            repository.recordAttendance(record)
            val currentMap = dailyAttendance.value.toMutableMap()
            currentMap[studentId] = status
            dailyAttendance.value = currentMap
        }
    }

    fun markAllPresent(students: List<Student>) {
        viewModelScope.launch {
            val records = students.map { student ->
                AttendanceRecord(
                    studentId = student.id,
                    date = currentDate.value,
                    statusEnum = AttendanceStatus.PRESENT,
                    isFollowedUp = true
                )
            }
            repository.recordAttendanceBulk(records)
            val newMap = students.associate { it.id to AttendanceStatus.PRESENT }
            dailyAttendance.value = newMap
        }
    }

    fun resolveAlert(recordId: Long, method: String, note: String) {
        viewModelScope.launch {
            repository.resolveFollowUp(recordId, method, note)
        }
    }

    fun addEvaluation(studentId: Long, title: String, scoreType: String, note: String = "") {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = currentDate.value,
                title = title,
                scoreType = scoreType,
                note = note
            )
            repository.addEvaluation(record)
        }
    }

    fun addCustomEvaluation(title: String, isPositive: Boolean) {
        viewModelScope.launch {
            repository.addCustomEvaluation(CustomEvaluation(title = title, isPositive = isPositive))
        }
    }

    fun importStudentsFromFile(uri: Uri) {
        viewModelScope.launch {
            val parsedStudents = ExcelImporter.parseFile(
                getApplication(),
                uri,
                selectedGrade.value,
                selectedSection.value
            )
            if (parsedStudents.isNotEmpty()) {
                repository.insertStudents(parsedStudents)
            }
        }
    }

    fun addStudent(student: Student) {
        viewModelScope.launch {
            repository.insertStudent(student)
        }
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

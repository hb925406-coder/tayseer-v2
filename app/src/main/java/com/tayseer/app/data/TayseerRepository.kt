package com.tayseer.app.data

import kotlinx.coroutines.flow.Flow

class TayseerRepository(private val database: AppDatabase) {

    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()
    private val evaluationDao = database.evaluationDao()
    private val customEvaluationDao = database.customEvaluationDao()

    // Student Operations
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()

    fun getStudentsBySection(grade: String, section: String): Flow<List<Student>> {
        return studentDao.getStudentsByGradeAndSection(grade, section)
    }

    suspend fun getStudentById(id: Long): Student? {
        return studentDao.getStudentById(id)
    }

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun insertStudents(students: List<Student>) {
        studentDao.insertStudents(students)
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    // Attendance Operations
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceByDate(date)
    }

    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceForStudent(studentId)
    }

    suspend fun getRecordForStudent(studentId: Long, date: String): AttendanceRecord? {
        return attendanceDao.getRecordForStudent(studentId, date)
    }

    val pendingAlerts: Flow<List<AttendanceRecord>> = attendanceDao.getPendingAlerts()

    suspend fun recordAttendance(record: AttendanceRecord): Long {
        return attendanceDao.insertAttendance(record)
    }

    suspend fun recordAttendanceBulk(records: List<AttendanceRecord>) {
        attendanceDao.insertAttendanceList(records)
    }

    suspend fun resolveFollowUp(recordId: Long, method: String, note: String) {
        attendanceDao.resolveFollowUp(recordId, method, note)
    }

    // Evaluation Operations
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>> {
        return evaluationDao.getEvaluationsForStudent(studentId)
    }

    suspend fun addEvaluation(evaluation: EvaluationRecord): Long {
        return evaluationDao.insertEvaluation(evaluation)
    }

    suspend fun deleteEvaluation(evaluation: EvaluationRecord) {
        evaluationDao.deleteEvaluation(evaluation)
    }

    // Custom Evaluations
    val allCustomEvaluations: Flow<List<CustomEvaluation>> = customEvaluationDao.getAllCustomEvaluations()

    suspend fun addCustomEvaluation(customEvaluation: CustomEvaluation): Long {
        return customEvaluationDao.insertCustomEvaluation(customEvaluation)
    }

    suspend fun deleteCustomEvaluation(customEvaluation: CustomEvaluation) {
        customEvaluationDao.deleteCustomEvaluation(customEvaluation)
    }
}

package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluation_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Query("SELECT * FROM evaluation_records WHERE studentId = :studentId")
    suspend fun getEvaluationsForStudentSync(studentId: Long): List<EvaluationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationRecord): Long

    @Delete
    suspend fun deleteEvaluation(evaluation: EvaluationRecord)
}

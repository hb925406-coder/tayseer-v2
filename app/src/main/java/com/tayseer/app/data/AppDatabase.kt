package com.tayseer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Student::class,
        AttendanceRecord::class,
        EvaluationRecord::class,
        CustomEvaluation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun customEvaluationDao(): CustomEvaluationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tayseer_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialEvaluations(database.customEvaluationDao())
                    }
                }
            }

            suspend fun populateInitialEvaluations(customEvaluationDao: CustomEvaluationDao) {
                customEvaluationDao.insertCustomEvaluation(CustomEvaluation(title = "حفظه", isPositive = true))
                customEvaluationDao.insertCustomEvaluation(CustomEvaluation(title = "لم يحفظ", isPositive = false))
                customEvaluationDao.insertCustomEvaluation(CustomEvaluation(title = "مميز", isPositive = true))
                customEvaluationDao.insertCustomEvaluation(CustomEvaluation(title = "مؤدب", isPositive = true))
                customEvaluationDao.insertCustomEvaluation(CustomEvaluation(title = "نسيان الكتب", isPositive = false))
            }
        }
    }
}

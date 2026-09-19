package com.application.cadence.data.backup

import androidx.room.withTransaction
import com.application.cadence.data.local.AppDatabase
import com.application.cadence.data.local.LessonEntity
import com.application.cadence.data.local.ScheduleEntity
import com.application.cadence.data.local.StudentEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val students: List<StudentEntity>,
    val lessons: List<LessonEntity>,
    val schedules: List<ScheduleEntity>
)

class BackupManager(private val db: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exportJson(): String {
        val data = BackupData(
            students = db.studentDao().getAll(),
            lessons = db.lessonDao().getAll(),
            schedules = db.scheduleDao().getAll()
        )
        return json.encodeToString(data)
    }

    suspend fun importJson(text: String) {
        val data = json.decodeFromString<BackupData>(text)
        db.withTransaction {
            db.studentDao().deleteAll()
            data.students.forEach { db.studentDao().insert(it) }
            data.lessons.forEach { db.lessonDao().insert(it) }
            data.schedules.forEach { db.scheduleDao().insert(it) }
        }
    }
}

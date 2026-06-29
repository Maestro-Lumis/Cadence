package com.application.cadence

import android.app.Application
import androidx.room.Room
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonPackageRepository
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.data.local.AppDatabase
import com.application.cadence.data.repository.LessonPackageRepositoryImpl
import com.application.cadence.data.repository.LessonRepositoryImpl
import com.application.cadence.data.repository.StudentRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class CadenceApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "cadence.db").build()
    }

    val studentRepository: StudentRepository by lazy { StudentRepositoryImpl(database.studentDao()) }
    val lessonRepository: LessonRepository by lazy { LessonRepositoryImpl(database.lessonDao()) }
    val packageRepository: LessonPackageRepository by lazy { LessonPackageRepositoryImpl(database.packageDao()) }

    override fun onCreate() {
        super.onCreate()
        seedDebugDataIfEmpty()
    }

    private fun seedDebugDataIfEmpty() {
        applicationScope.launch {
            val existing = studentRepository.observeAll().first()
            if (existing.isEmpty()) {
                val studentId = studentRepository.add(
                    Student(
                        id = 0,
                        name = "Тестовый Ученик",
                        course = "Kotlin",
                        createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                )
                lessonRepository.add(
                    Lesson(
                        id = 0,
                        studentId = studentId,
                        date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                        time = "18:00",
                        status = LessonStatus.SCHEDULED,
                        lessonNumber = 1,
                        packageId = null,
                        paid = false
                    )
                )
            }
        }
    }
}
package com.application.cadence

import android.app.Application
import androidx.room.Room
import com.application.cadence.core.LessonPackageRepository
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.ScheduleRepository
import com.application.cadence.core.StudentRepository
import com.application.cadence.data.local.AppDatabase
import com.application.cadence.data.local.MIGRATION_1_2
import com.application.cadence.data.local.MIGRATION_2_3
import com.application.cadence.data.local.MIGRATION_3_4
import com.application.cadence.data.repository.LessonPackageRepositoryImpl
import com.application.cadence.data.repository.LessonRepositoryImpl
import com.application.cadence.data.repository.ScheduleRepositoryImpl
import com.application.cadence.data.repository.StudentRepositoryImpl

class CadenceApplication : Application() {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "cadence.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val studentRepository: StudentRepository by lazy { StudentRepositoryImpl(database.studentDao()) }
    val lessonRepository: LessonRepository by lazy { LessonRepositoryImpl(database.lessonDao()) }
    val packageRepository: LessonPackageRepository by lazy { LessonPackageRepositoryImpl(database.packageDao()) }
    val scheduleRepository: ScheduleRepository by lazy { ScheduleRepositoryImpl(database.scheduleDao()) }
}

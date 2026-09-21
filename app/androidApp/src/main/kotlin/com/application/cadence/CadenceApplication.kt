package com.application.cadence

import android.app.Application
import androidx.room.Room
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.ScheduleRepository
import com.application.cadence.core.StudentRepository
import com.application.cadence.data.backup.BackupManager
import com.application.cadence.data.local.AppDatabase
import com.application.cadence.data.local.MIGRATION_1_2
import com.application.cadence.data.local.MIGRATION_2_3
import com.application.cadence.data.local.MIGRATION_3_4
import com.application.cadence.data.local.MIGRATION_4_5
import com.application.cadence.data.notifications.NotificationScheduler
import com.application.cadence.data.repository.LessonRepositoryImpl
import com.application.cadence.data.repository.ScheduleRepositoryImpl
import com.application.cadence.data.repository.StudentRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CadenceApplication : Application() {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "cadence.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    val studentRepository: StudentRepository by lazy { StudentRepositoryImpl(database.studentDao()) }
    val lessonRepository: LessonRepository by lazy { LessonRepositoryImpl(database.lessonDao()) }
    val scheduleRepository: ScheduleRepository by lazy { ScheduleRepositoryImpl(database.scheduleDao()) }
    val backupManager: BackupManager by lazy { BackupManager(database) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureChannel(this)
        appScope.launch {
            lessonRepository.observeAll().collectLatest { lessons ->
                NotificationScheduler.sync(this@CadenceApplication, lessons)
            }
        }
    }
}

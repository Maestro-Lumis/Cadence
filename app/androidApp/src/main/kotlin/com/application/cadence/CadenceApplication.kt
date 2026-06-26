package com.application.cadence

import android.app.Application
import androidx.room.Room
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.StudentRepository
import com.application.cadence.data.local.AppDatabase
import com.application.cadence.data.repository.LessonRepositoryImpl
import com.application.cadence.data.repository.StudentRepositoryImpl


class CadenceApplication : Application() {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "cadence.db").build()
    }

    val studentRepository: StudentRepository by lazy { StudentRepositoryImpl(database.studentDao()) }
    val lessonRepository: LessonRepository by lazy { LessonRepositoryImpl(database.lessonDao()) }
}
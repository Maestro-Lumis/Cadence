package com.application.cadence.data.repository

import com.application.cadence.data.local.LessonDao
import com.application.cadence.data.local.StudentDao
import com.application.cadence.data.mapper.toDomain
import com.application.cadence.data.mapper.toEntity
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.collections.map

class StudentRepositoryImpl(private val dao: StudentDao) : StudentRepository {
    override fun observeAll(): Flow<List<Student>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(studentId: Long): Flow<Student?> =
        dao.observeById(studentId).map { it?.toDomain() }

    override suspend fun add(student: Student) = dao.insert(student.toEntity())
    override suspend fun update(student: Student) = dao.update(student.toEntity())
    override suspend fun delete(studentId: Long) = dao.deleteById(studentId)
}

class LessonRepositoryImpl(private val dao: LessonDao) : LessonRepository {
    override fun observeByDate(date: LocalDate): Flow<List<Lesson>> =
        dao.observeByDate(date.toString()).map { list -> list.map { it.toDomain() } }

    override fun observeByStudent(studentId: Long): Flow<List<Lesson>> =
        dao.observeByStudent(studentId).map { list -> list.map { it.toDomain() } }

    override suspend fun add(lesson: Lesson) = dao.insert(lesson.toEntity())
    override suspend fun update(lesson: Lesson) = dao.update(lesson.toEntity())
    override suspend fun delete(lessonId: Long) = dao.deleteById(lessonId)
}
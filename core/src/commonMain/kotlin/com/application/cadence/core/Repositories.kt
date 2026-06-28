package com.application.cadence.core

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface StudentRepository {
    fun observeAll(): Flow<List<Student>>
    fun observeById(studentId: Long): Flow<Student?>
    suspend fun add(student: Student): Long
    suspend fun update(student: Student)
    suspend fun delete(studentId: Long)
}

interface LessonRepository {
    fun observeByDate(date: LocalDate): Flow<List<Lesson>>
    fun observeByStudent(studentId: Long): Flow<List<Lesson>>
    suspend fun add(lesson: Lesson): Long
    suspend fun update(lesson: Lesson)
    suspend fun delete(lessonId: Long)
}
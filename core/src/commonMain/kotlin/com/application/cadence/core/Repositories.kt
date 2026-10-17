package com.application.cadence.core

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface StudentRepository {
    fun observeAll(): Flow<List<Student>>
    fun observeActive(): Flow<List<Student>>
    fun observeArchived(): Flow<List<Student>>
    fun observeById(studentId: Long): Flow<Student?>
    suspend fun setArchived(studentId: Long, archived: Boolean)
    suspend fun add(student: Student): Long
    suspend fun update(student: Student)
    suspend fun delete(studentId: Long)
}

interface LessonRepository {
    fun observeByDate(date: LocalDate): Flow<List<Lesson>>
    fun observeInDateRange(from: LocalDate, to: LocalDate): Flow<List<Lesson>>
    fun observeByStudent(studentId: Long): Flow<List<Lesson>>
    fun observeByGroup(groupId: Long): Flow<List<Lesson>>
    fun observeScheduledUpTo(date: LocalDate): Flow<List<Lesson>>
    fun observeUnpaidHeld(): Flow<List<Lesson>>
    fun observeAll(): Flow<List<Lesson>>
    fun observeById(lessonId: Long): Flow<Lesson?>
    suspend fun getById(lessonId: Long): Lesson?
    suspend fun markStudentDebtsPaid(studentId: Long)
    suspend fun add(lesson: Lesson): Long
    suspend fun update(lesson: Lesson)
    suspend fun delete(lessonId: Long)
    suspend fun countScheduledBySlot(studentId: Long, fromDate: LocalDate, time: String, dayOfWeek: Weekday): Int
    suspend fun deleteScheduledBySlot(studentId: Long, fromDate: LocalDate, time: String, dayOfWeek: Weekday)
    suspend fun countScheduledByStudent(studentId: Long, fromDate: LocalDate): Int
    suspend fun deleteScheduledByStudent(studentId: Long, fromDate: LocalDate)
}



interface ScheduleRepository {
    fun observeByStudent(studentId: Long): Flow<List<Schedule>>
    suspend fun add(schedule: Schedule): Long
    suspend fun delete(scheduleId: Long)
    suspend fun deleteByStudent(studentId: Long)
}
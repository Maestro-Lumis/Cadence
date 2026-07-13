package com.application.cadence.core

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Serializable
data class Student(
    val id: Long,
    val name: String,
    val course: String,
    val timezone: String,
    val hourlyRate: Int,
    val createdAt: LocalDateTime
)

@Serializable
enum class Weekday { MON, TUE, WED, THU, FRI, SAT, SUN }

@Serializable
data class Schedule(
    val id: Long,
    val studentId: Long,
    val dayOfWeek: Weekday,
    val time: String,
    val durationMinutes: Int,
    val active: Boolean
)

@Serializable
data class LessonPackage(
    val id: Long,
    val studentId: Long,
    val totalLessons: Int,
    val usedLessons: Int,
    val paid: Boolean,
    val createdAt: LocalDateTime
)

@Serializable
enum class LessonStatus { SCHEDULED, HELD, CANCELLED, RESCHEDULED }

@Serializable
data class Lesson(
    val id: Long,
    val studentId: Long,
    val date: LocalDate,
    val time: String,
    val durationMinutes: Int,
    val status: LessonStatus,
    val lessonNumber: Int? = null,
    val packageId: Long? = null,
    val paid: Boolean = false
)
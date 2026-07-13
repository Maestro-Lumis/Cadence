package com.application.cadence.data.mapper

import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonPackage
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Schedule
import com.application.cadence.core.Student
import com.application.cadence.core.Weekday
import com.application.cadence.data.local.LessonEntity
import com.application.cadence.data.local.PackageEntity
import com.application.cadence.data.local.ScheduleEntity
import com.application.cadence.data.local.StudentEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun StudentEntity.toDomain() = Student(
    id = id,
    name = name,
    course = course,
    timezone = timezone,
    hourlyRate = hourlyRate,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
)

fun Student.toEntity() = StudentEntity(
    id = id,
    name = name,
    course = course,
    timezone = timezone,
    hourlyRate = hourlyRate,
    createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
)

fun LessonEntity.toDomain() = Lesson(
    id = id,
    studentId = studentId,
    date = LocalDate.parse(date),
    time = time,
    durationMinutes = durationMinutes,
    status = LessonStatus.valueOf(status),
    lessonNumber = lessonNumber,
    packageId = packageId,
    paid = paid
)

fun Lesson.toEntity() = LessonEntity(
    id = id,
    studentId = studentId,
    date = date.toString(),
    time = time,
    durationMinutes = durationMinutes,
    status = status.name,
    lessonNumber = lessonNumber,
    packageId = packageId,
    paid = paid
)

fun PackageEntity.toDomain() = LessonPackage(
    id = id,
    studentId = studentId,
    totalLessons = totalLessons,
    usedLessons = usedLessons,
    paid = paid,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
)

fun LessonPackage.toEntity() = PackageEntity(
    id = id,
    studentId = studentId,
    totalLessons = totalLessons,
    usedLessons = usedLessons,
    paid = paid,
    createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
)

fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    studentId = studentId,
    dayOfWeek = Weekday.valueOf(dayOfWeek),
    time = time,
    durationMinutes = durationMinutes,
    active = active
)

fun Schedule.toEntity() = ScheduleEntity(
    id = id,
    studentId = studentId,
    dayOfWeek = dayOfWeek.name,
    time = time,
    durationMinutes = durationMinutes,
    active = active
)
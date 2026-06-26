package com.application.cadence.data.mapper

import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.data.local.LessonEntity
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
    createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
)

fun Student.toEntity() = StudentEntity(
    id = id,
    name = name,
    course = course,
    createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
)

fun LessonEntity.toDomain() = Lesson(
    id = id,
    studentId = studentId,
    date = LocalDate.parse(date),
    time = time,
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
    status = status.name,
    lessonNumber = lessonNumber,
    packageId = packageId,
    paid = paid
)
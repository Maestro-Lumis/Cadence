package com.application.cadence.presentation.common

import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

fun findOverlappingLesson(
    proposedStart: Instant,
    proposedEnd: Instant,
    nearbyLessons: List<Lesson>,
    excludeLessonId: Long? = null
): Lesson? = nearbyLessons.firstOrNull { existing ->
    if (existing.id == excludeLessonId) return@firstOrNull false
    if (existing.status == LessonStatus.CANCELLED) return@firstOrNull false
    val existingTime = runCatching { LocalTime.parse(existing.time) }.getOrNull() ?: return@firstOrNull false
    val existingStart = LocalDateTime(existing.date, existingTime).toInstant(MSK)
    val existingEnd = existingStart + existing.durationMinutes.minutes
    existingStart < proposedEnd && existingEnd > proposedStart
}

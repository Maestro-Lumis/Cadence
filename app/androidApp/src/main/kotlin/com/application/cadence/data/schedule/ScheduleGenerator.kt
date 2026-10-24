package com.application.cadence.data.schedule

import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.ScheduleRepository
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

object ScheduleGenerator {

    private const val HORIZON_WEEKS = 35

    data class Result(val created: Int, val skipped: Int)

    suspend fun generateForSlot(
        lessonRepository: LessonRepository,
        studentId: Long,
        day: Weekday,
        time: String,
        durationMinutes: Int
    ): Result {
        val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull()
            ?: return Result(0, 0)

        val today = Clock.System.todayIn(MSK)
        val horizon = today.plus(HORIZON_WEEKS * 7, DateTimeUnit.DAY)
        val existing = lessonRepository.observeInDateRange(today, horizon).first()

        var created = 0
        var skipped = 0
        val toInsert = mutableListOf<Lesson>()

        var date = today
        while (date <= horizon) {
            val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
            if (weekday == day) {
                val dup = existing.any { it.studentId == studentId && it.date == date }
                if (dup) {
                    skipped++
                } else {
                    val lesson = Lesson(
                        id = 0,
                        studentId = studentId,
                        date = date,
                        time = time,
                        durationMinutes = durationMinutes,
                        status = LessonStatus.SCHEDULED
                    )
                    toInsert.add(lesson)
                    created++
                }
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }

        toInsert.forEach { lessonRepository.add(it) }
        return Result(created, skipped)
    }

    suspend fun topUpAll(
        scheduleRepository: ScheduleRepository,
        lessonRepository: LessonRepository,
        studentIds: List<Long>
    ) {
        for (studentId in studentIds) {
            val slots = scheduleRepository.observeByStudent(studentId).first()
            for (slot in slots.filter { it.active }) {
                generateForSlot(lessonRepository, studentId, slot.dayOfWeek, slot.time, slot.durationMinutes)
            }
        }
    }
}

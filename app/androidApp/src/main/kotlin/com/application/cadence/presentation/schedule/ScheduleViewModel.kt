package com.application.cadence.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Schedule
import com.application.cadence.core.ScheduleRepository
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.findOverlappingLesson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private const val HORIZON_WEEKS = 35 // ~8 months

data class GenerationResult(val created: Int, val skipped: Int)

class ScheduleViewModel(
    private val studentId: Long,
    private val scheduleRepository: ScheduleRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    val slots: StateFlow<List<Schedule>> = scheduleRepository.observeByStudent(studentId)
        .map { list -> list.sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.time })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSlot(day: Weekday, time: String, durationMinutes: Int, onResult: (GenerationResult) -> Unit) {
        viewModelScope.launch {
            scheduleRepository.add(
                Schedule(
                    id = 0,
                    studentId = studentId,
                    dayOfWeek = day,
                    time = time,
                    durationMinutes = durationMinutes,
                    active = true
                )
            )
            val result = generateForSlot(day, time, durationMinutes)
            onResult(result)
        }
    }

    fun countFutureLessons(slot: Schedule, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val today = Clock.System.todayIn(MSK)
            val count = lessonRepository.countScheduledBySlot(studentId, today, slot.time, slot.dayOfWeek)
            onResult(count)
        }
    }

    fun deleteSlot(slot: Schedule) {
        viewModelScope.launch {
            val today = Clock.System.todayIn(MSK)
            lessonRepository.deleteScheduledBySlot(studentId, today, slot.time, slot.dayOfWeek)
            scheduleRepository.delete(slot.id)
        }
    }

    private suspend fun generateForSlot(day: Weekday, time: String, durationMinutes: Int): GenerationResult {
        val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull()
            ?: return GenerationResult(0, 0)

        val today = Clock.System.todayIn(MSK)
        val horizon = today.plus(HORIZON_WEEKS * 7, DateTimeUnit.DAY)
        val existing = lessonRepository.observeInDateRange(today, horizon).first().toMutableList()

        var created = 0
        var skipped = 0
        val toInsert = mutableListOf<Lesson>()

        var date = today
        while (date <= horizon) {
            val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
            if (weekday == day) {
                val start = LocalDateTime(date, parsedTime).toInstant(MSK)
                val end = start + durationMinutes.minutes
                val dup = existing.any { it.studentId == studentId && it.date == date && it.time == time }
                val overlap = findOverlappingLesson(start, end, existing) != null
                if (dup || overlap) {
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
                    existing.add(lesson)
                    toInsert.add(lesson)
                    created++
                }
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }

        toInsert.forEach { lessonRepository.add(it) }
        return GenerationResult(created, skipped)
    }
}

class ScheduleViewModelFactory(
    private val studentId: Long,
    private val scheduleRepository: ScheduleRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScheduleViewModel(studentId, scheduleRepository, lessonRepository) as T
    }
}

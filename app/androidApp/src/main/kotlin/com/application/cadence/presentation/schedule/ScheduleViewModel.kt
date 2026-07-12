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

data class GenerationResult(val created: Int, val skipped: Int)

class ScheduleViewModel(
    private val studentId: Long,
    private val scheduleRepository: ScheduleRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    val slots: StateFlow<List<Schedule>> = scheduleRepository.observeByStudent(studentId)
        .map { list -> list.sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.time })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSlot(day: Weekday, time: String, durationMinutes: Int, onAdded: () -> Unit) {
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
            onAdded()
        }
    }

    fun deleteSlot(scheduleId: Long) {
        viewModelScope.launch { scheduleRepository.delete(scheduleId) }
    }

    fun generate(onResult: (GenerationResult) -> Unit) {
        viewModelScope.launch {
            val activeSlots = slots.value.filter { it.active }
            if (activeSlots.isEmpty()) {
                onResult(GenerationResult(0, 0))
                return@launch
            }

            val today = Clock.System.todayIn(MSK)
            val horizon = today.plus(28, DateTimeUnit.DAY)
            val existing = lessonRepository.observeInDateRange(today, horizon).first().toMutableList()
            var nextNumber = (lessonRepository.observeByStudent(studentId).first()
                .mapNotNull { it.lessonNumber }.maxOrNull() ?: 0) + 1

            var created = 0
            var skipped = 0
            val toInsert = mutableListOf<Lesson>()

            var date = today
            while (date <= horizon) {
                val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
                val daySlots = activeSlots.filter { it.dayOfWeek == weekday }.sortedBy { it.time }
                for (slot in daySlots) {
                    val time = runCatching { LocalTime.parse(slot.time) }.getOrNull() ?: continue
                    val start = LocalDateTime(date, time).toInstant(MSK)
                    val end = start + slot.durationMinutes.minutes
                    val dup = existing.any { it.studentId == studentId && it.date == date && it.time == slot.time }
                    val overlap = findOverlappingLesson(start, end, existing) != null
                    if (dup || overlap) {
                        skipped++
                        continue
                    }
                    val lesson = Lesson(
                        id = 0,
                        studentId = studentId,
                        date = date,
                        time = slot.time,
                        durationMinutes = slot.durationMinutes,
                        status = LessonStatus.SCHEDULED,
                        lessonNumber = nextNumber++,
                        packageId = null,
                        paid = false
                    )
                    existing.add(lesson)
                    toInsert.add(lesson)
                    created++
                }
                date = date.plus(1, DateTimeUnit.DAY)
            }

            toInsert.forEach { lessonRepository.add(it) }
            onResult(GenerationResult(created, skipped))
        }
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

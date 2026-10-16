package com.application.cadence.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.Schedule
import com.application.cadence.core.ScheduleRepository
import com.application.cadence.core.Weekday
import com.application.cadence.data.schedule.ScheduleGenerator
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.todayIn
import kotlin.time.Clock

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
            val result = ScheduleGenerator.generateForSlot(lessonRepository, studentId, day, time, durationMinutes)
            onResult(GenerationResult(result.created, result.skipped))
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

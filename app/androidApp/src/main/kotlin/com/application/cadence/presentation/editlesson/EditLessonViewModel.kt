package com.application.cadence.presentation.editlesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.findOverlappingLesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

class EditLessonViewModel(
    private val lessonId: Long,
    private val lessonRepository: LessonRepository,
    studentRepository: StudentRepository
) : ViewModel() {

    val students: StateFlow<List<Student>> = studentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _initialLesson = MutableStateFlow<Lesson?>(null)
    val initialLesson: StateFlow<Lesson?> = _initialLesson

    init {
        viewModelScope.launch {
            _initialLesson.value = lessonRepository.getById(lessonId)
        }
    }

    fun save(
        studentId: Long,
        date: LocalDate,
        time: String,
        durationMinutes: Int,
        status: LessonStatus,
        lessonNumber: Int?,
        paid: Boolean,
        onError: (String) -> Unit,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val studentList = students.value
            val student = studentList.find { it.id == studentId }
            if (student == null) {
                onError("Ученик не найден")
                return@launch
            }
            val lessonTime = runCatching { LocalTime.parse(time) }.getOrNull()
            if (lessonTime == null) {
                onError("Неверный формат времени")
                return@launch
            }
            val newStart = LocalDateTime(date, lessonTime).toInstant(MSK)
            val newEnd = newStart + durationMinutes.minutes

            if (status != LessonStatus.CANCELLED) {
                val nearby = lessonRepository.observeInDateRange(
                    date.minus(1, DateTimeUnit.DAY),
                    date.plus(1, DateTimeUnit.DAY)
                ).first()
                val overlap = findOverlappingLesson(newStart, newEnd, nearby, excludeLessonId = lessonId)
                if (overlap != null) {
                    val ovStudent = studentList.find { it.id == overlap.studentId }
                    onError("Пересечение с занятием: ${ovStudent?.name ?: "?"} ${overlap.date} ${overlap.time} МСК")
                    return@launch
                }
            }

            lessonRepository.update(
                Lesson(
                    id = lessonId,
                    studentId = studentId,
                    date = date,
                    time = time,
                    durationMinutes = durationMinutes,
                    status = status,
                    lessonNumber = lessonNumber,
                    packageId = _initialLesson.value?.packageId,
                    paid = paid
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            lessonRepository.delete(lessonId)
            onDeleted()
        }
    }
}

class EditLessonViewModelFactory(
    private val lessonId: Long,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditLessonViewModel(lessonId, lessonRepository, studentRepository) as T
    }
}

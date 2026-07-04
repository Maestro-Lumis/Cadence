package com.application.cadence.presentation.addlesson

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddLessonViewModel(
    private val lessonRepository: LessonRepository,
    studentRepository: StudentRepository
) : ViewModel() {

    val students: StateFlow<List<Student>> = studentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedStudentId = MutableStateFlow<Long?>(null)

    val suggestedLessonNumber: StateFlow<Int?> = selectedStudentId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else lessonRepository.observeByStudent(id).map { list ->
                (list.mapNotNull { it.lessonNumber }.maxOrNull() ?: 0) + 1
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectStudent(studentId: Long?) {
        selectedStudentId.value = studentId
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
                val overlap = findOverlappingLesson(newStart, newEnd, nearby)
                if (overlap != null) {
                    val ovStudent = studentList.find { it.id == overlap.studentId }
                    onError("Пересечение с занятием: ${ovStudent?.name ?: "?"} ${overlap.date} ${overlap.time} МСК")
                    return@launch
                }
            }

            lessonRepository.add(
                Lesson(
                    id = 0,
                    studentId = studentId,
                    date = date,
                    time = time,
                    durationMinutes = durationMinutes,
                    status = status,
                    lessonNumber = lessonNumber,
                    packageId = null,
                    paid = paid
                )
            )
            onSaved()
        }
    }
}

class AddLessonViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddLessonViewModel(lessonRepository, studentRepository) as T
    }
}

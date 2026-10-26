package com.application.cadence.presentation.editlesson

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.R
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.findOverlappingLesson
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
    studentRepository: StudentRepository,
    private val ctx: Context
) : ViewModel() {

    val students: StateFlow<List<Student>> = studentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val initialLesson: StateFlow<Lesson?> = lessonRepository.observeById(lessonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(
        studentId: Long,
        date: LocalDate,
        time: String,
        durationMinutes: Int,
        status: LessonStatus,
        paid: Boolean,
        onError: (String) -> Unit,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val studentList = students.value
            val student = studentList.find { it.id == studentId }
            if (student == null) {
                onError(ctx.getString(R.string.lesson_err_student_missing))
                return@launch
            }
            val lessonTime = runCatching { LocalTime.parse(time) }.getOrNull()
            if (lessonTime == null) {
                onError(ctx.getString(R.string.lesson_err_time_format))
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
                    onError(ctx.getString(R.string.lesson_err_overlap, "${ovStudent?.name ?: "?"} ${overlap.date} ${overlap.time}"))
                    return@launch
                }
            }

            val current = initialLesson.value
            if (current != null && current.status == LessonStatus.SCHEDULED &&
                (current.date != date || current.time != time)
            ) {
                lessonRepository.add(current.copy(id = 0, status = LessonStatus.CANCELLED))
            }
            lessonRepository.update(
                Lesson(
                    id = lessonId,
                    studentId = studentId,
                    date = date,
                    time = time,
                    durationMinutes = durationMinutes,
                    status = status,
                    lessonNumber = null,
                    packageId = current?.packageId,
                    paid = paid,
                    groupId = current?.groupId
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val lesson = initialLesson.value
            if (lesson != null && lesson.status == LessonStatus.SCHEDULED) {
                lessonRepository.update(lesson.copy(status = LessonStatus.CANCELLED))
            } else {
                lessonRepository.delete(lessonId)
            }
            onDeleted()
        }
    }
}

class EditLessonViewModelFactory(
    private val lessonId: Long,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val ctx: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditLessonViewModel(lessonId, lessonRepository, studentRepository, ctx) as T
    }
}

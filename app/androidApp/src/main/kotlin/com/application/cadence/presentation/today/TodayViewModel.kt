package com.application.cadence.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class TodayLessonUi(
    val lessonId: Long,
    val studentId: Long,
    val studentName: String,
    val course: String,
    val time: String,
    val status: LessonStatus,
    val lessonNumber: Int?,
    val paid: Boolean
)

class TodayViewModel(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val uiState: StateFlow<List<TodayLessonUi>> = combine(
        lessonRepository.observeByDate(today),
        studentRepository.observeAll()
    ) { lessons, students ->
        val byId = students.associateBy { it.id }
        lessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            TodayLessonUi(
                lessonId = lesson.id,
                studentId = lesson.studentId,
                studentName = student.name,
                course = student.course,
                time = lesson.time,
                status = lesson.status,
                lessonNumber = lesson.lessonNumber,
                paid = lesson.paid
            )
        }.sortedBy { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
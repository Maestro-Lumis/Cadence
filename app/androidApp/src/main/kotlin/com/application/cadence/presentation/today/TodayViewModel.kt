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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.application.cadence.presentation.common.timezoneLabel
import kotlin.time.Clock

data class TodayLessonUi(
    val lessonId: Long,
    val studentId: Long,
    val studentName: String,
    val course: String,
    val time: String,
    val studentLocalTime: String?,
    val durationMinutes: Int,
    val status: LessonStatus,
    val lessonNumber: Int?,
    val paid: Boolean
)

class TodayViewModel(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val tutorTz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tutorTz)
    private val startOfToday = today.atStartOfDayIn(tutorTz)
    private val startOfTomorrow = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tutorTz)

    val uiState: StateFlow<List<TodayLessonUi>> = combine(
        lessonRepository.observeInDateRange(
            today.plus(-1, DateTimeUnit.DAY),
            today.plus(1, DateTimeUnit.DAY)
        ),
        studentRepository.observeAll()
    ) { lessons, students ->
        val byId = students.associateBy { it.id }
        lessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val studentTz = runCatching { TimeZone.of(student.timezone) }.getOrDefault(tutorTz)
            val lessonTime = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val lessonInstant = LocalDateTime(lesson.date, lessonTime).toInstant(studentTz)
            if (lessonInstant < startOfToday || lessonInstant >= startOfTomorrow) return@mapNotNull null

            val tutorLocalDateTime = lessonInstant.toLocalDateTime(tutorTz)
            val tutorTimeStr = "%02d:%02d".format(tutorLocalDateTime.hour, tutorLocalDateTime.minute)
            val studentTimeStr = if (student.timezone == tutorTz.id) null
                else "%02d:%02d · ${timezoneLabel(student.timezone)}".format(lessonTime.hour, lessonTime.minute)

            TodayLessonUi(
                lessonId = lesson.id,
                studentId = lesson.studentId,
                studentName = student.name,
                course = student.course,
                time = tutorTimeStr,
                studentLocalTime = studentTimeStr,
                durationMinutes = lesson.durationMinutes,
                status = lesson.status,
                lessonNumber = lesson.lessonNumber,
                paid = lesson.paid
            )
        }.sortedBy { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

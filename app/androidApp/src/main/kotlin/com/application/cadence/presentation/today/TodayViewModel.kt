package com.application.cadence.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

data class TodayLessonUi(
    val lessonId: Long,
    val studentId: Long,
    val studentName: String,
    val course: String,
    val time: String,
    val endTime: String,
    val mskTime: String?,
    val status: LessonStatus,
    val lessonNumber: Int?,
    val paid: Boolean
)

data class ReviewLessonUi(
    val lessonId: Long,
    val studentName: String,
    val course: String,
    val whenLabel: String
)

data class TodayUi(
    val reviewQueue: List<ReviewLessonUi>,
    val lessons: List<TodayLessonUi>
)

data class DebtSummary(
    val lessonCount: Int,
    val studentCount: Int
)

class TodayViewModel(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val tutorTz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tutorTz)
    private val mskToday = Clock.System.todayIn(MSK)
    private val startOfToday = today.atStartOfDayIn(tutorTz)
    private val startOfTomorrow = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tutorTz)

    val uiState: StateFlow<TodayUi> = combine(
        lessonRepository.observeInDateRange(
            today.plus(-1, DateTimeUnit.DAY),
            today.plus(1, DateTimeUnit.DAY)
        ),
        lessonRepository.observeScheduledUpTo(mskToday),
        studentRepository.observeAll()
    ) { rangeLessons, scheduledUpTo, students ->
        val byId = students.associateBy { it.id }
        val now = Clock.System.now()

        val lessons = rangeLessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val lessonTime = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val lessonStart = LocalDateTime(lesson.date, lessonTime).toInstant(MSK)
            if (lessonStart < startOfToday || lessonStart >= startOfTomorrow) return@mapNotNull null

            val lessonEnd = lessonStart + lesson.durationMinutes.minutes
            val tutorStart = lessonStart.toLocalDateTime(tutorTz)
            val tutorEnd = lessonEnd.toLocalDateTime(tutorTz)
            val timeStr = "%02d:%02d".format(tutorStart.hour, tutorStart.minute)
            val endTimeStr = "%02d:%02d".format(tutorEnd.hour, tutorEnd.minute)

            val mskHint = if (tutorTz.id == MSK.id) null
                else "%02d:%02d МСК".format(lessonTime.hour, lessonTime.minute)

            TodayLessonUi(
                lessonId = lesson.id,
                studentId = lesson.studentId,
                studentName = student.name,
                course = student.course,
                time = timeStr,
                endTime = endTimeStr,
                mskTime = mskHint,
                status = lesson.status,
                lessonNumber = lesson.lessonNumber,
                paid = lesson.paid
            )
        }.sortedBy { it.time }

        val reviewQueue = scheduledUpTo.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val lessonTime = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val lessonStart = LocalDateTime(lesson.date, lessonTime).toInstant(MSK)
            val lessonEnd = lessonStart + lesson.durationMinutes.minutes
            if (lessonEnd >= now) return@mapNotNull null

            val startLocal = lessonStart.toLocalDateTime(tutorTz)
            val dayWord = when (startLocal.date) {
                today -> "Сегодня"
                today.minus(1, DateTimeUnit.DAY) -> "Вчера"
                else -> startLocal.date.toString()
            }
            val timeStr = "%02d:%02d".format(startLocal.hour, startLocal.minute)

            lessonStart to ReviewLessonUi(
                lessonId = lesson.id,
                studentName = student.name,
                course = student.course,
                whenLabel = "$dayWord, $timeStr"
            )
        }.sortedBy { it.first }.map { it.second }

        TodayUi(reviewQueue = reviewQueue, lessons = lessons)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUi(emptyList(), emptyList()))

    val debtSummary: StateFlow<DebtSummary?> = lessonRepository.observeUnpaidHeld()
        .map { lessons ->
            if (lessons.isEmpty()) null
            else DebtSummary(
                lessonCount = lessons.size,
                studentCount = lessons.map { it.studentId }.distinct().size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun resolve(lessonId: Long, status: LessonStatus, paid: Boolean) {
        viewModelScope.launch {
            val lesson = lessonRepository.getById(lessonId) ?: return@launch
            lessonRepository.update(lesson.copy(status = status, paid = paid))
        }
    }
}

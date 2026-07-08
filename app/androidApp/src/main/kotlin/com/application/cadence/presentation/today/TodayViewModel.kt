package com.application.cadence.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.monthGenitive
import com.application.cadence.presentation.common.monthNominative
import com.application.cadence.presentation.common.weekdayLabel
import com.application.cadence.presentation.common.weekdayShort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

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

data class WeekDayUi(
    val date: LocalDate,
    val shortLabel: String,
    val dayNumber: Int,
    val lessonCount: Int,
    val isSelected: Boolean,
    val isToday: Boolean
)

data class DayUi(
    val monthTitle: String,
    val week: List<WeekDayUi>,
    val selectedLabel: String,
    val lessons: List<TodayLessonUi>
)

class TodayViewModel(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val tutorTz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tutorTz)
    private val mskToday = Clock.System.todayIn(MSK)

    private val _selectedDate = MutableStateFlow(today)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun shiftWeek(deltaWeeks: Int) {
        _selectedDate.value = _selectedDate.value.plus(deltaWeeks * 7, DateTimeUnit.DAY)
    }

    val dayState: StateFlow<DayUi> = combine(
        _selectedDate,
        lessonRepository.observeInDateRange(
            today.minus(60, DateTimeUnit.DAY),
            today.plus(180, DateTimeUnit.DAY)
        ),
        studentRepository.observeAll()
    ) { selected, lessons, students ->
        val weekStart = selected.minus(selected.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        buildDayUi(selected, weekStart, lessons, students)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DayUi(monthNominative(today.monthNumber), emptyList(), "", emptyList())
    )

    val reviewQueue: StateFlow<List<ReviewLessonUi>> = combine(
        lessonRepository.observeScheduledUpTo(mskToday),
        studentRepository.observeAll()
    ) { lessons, students ->
        buildReviewQueue(lessons, students)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolve(lessonId: Long, status: LessonStatus, paid: Boolean) {
        viewModelScope.launch {
            val lesson = lessonRepository.getById(lessonId) ?: return@launch
            lessonRepository.update(lesson.copy(status = status, paid = paid))
        }
    }

    private fun buildDayUi(
        selected: LocalDate,
        weekStart: LocalDate,
        lessons: List<Lesson>,
        students: List<Student>
    ): DayUi {
        val byId = students.associateBy { it.id }
        val enriched = lessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val start = LocalDateTime(lesson.date, time).toInstant(MSK)
            val localDate = start.toLocalDateTime(tutorTz).date
            Triple(localDate, start, buildLessonUi(lesson, student, start, time))
        }
        val countByDate = enriched.groupingBy { it.first }.eachCount()
        val selectedLessons = enriched.filter { it.first == selected }.sortedBy { it.second }.map { it.third }

        val week = (0..6).map { offset ->
            val date = weekStart.plus(offset, DateTimeUnit.DAY)
            val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
            WeekDayUi(
                date = date,
                shortLabel = weekdayShort(weekday),
                dayNumber = date.dayOfMonth,
                lessonCount = countByDate[date] ?: 0,
                isSelected = date == selected,
                isToday = date == today
            )
        }

        val selectedWeekday = Weekday.entries[selected.dayOfWeek.isoDayNumber - 1]
        val selectedLabel =
            "${weekdayLabel(selectedWeekday)}, ${selected.dayOfMonth} ${monthGenitive(selected.monthNumber)}"

        return DayUi(monthNominative(selected.monthNumber), week, selectedLabel, selectedLessons)
    }

    private fun buildLessonUi(lesson: Lesson, student: Student, start: Instant, time: LocalTime): TodayLessonUi {
        val end = start + lesson.durationMinutes.minutes
        val startLocal = start.toLocalDateTime(tutorTz)
        val endLocal = end.toLocalDateTime(tutorTz)
        return TodayLessonUi(
            lessonId = lesson.id,
            studentId = lesson.studentId,
            studentName = student.name,
            course = student.course,
            time = "%02d:%02d".format(startLocal.hour, startLocal.minute),
            endTime = "%02d:%02d".format(endLocal.hour, endLocal.minute),
            mskTime = if (tutorTz.id == MSK.id) null else "%02d:%02d МСК".format(time.hour, time.minute),
            status = lesson.status,
            lessonNumber = lesson.lessonNumber,
            paid = lesson.paid
        )
    }

    private fun buildReviewQueue(lessons: List<Lesson>, students: List<Student>): List<ReviewLessonUi> {
        val byId = students.associateBy { it.id }
        val now = Clock.System.now()
        return lessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val start = LocalDateTime(lesson.date, time).toInstant(MSK)
            val end = start + lesson.durationMinutes.minutes
            if (end >= now) return@mapNotNull null

            val startLocal = start.toLocalDateTime(tutorTz)
            val dayWord = when (startLocal.date) {
                today -> "Сегодня"
                today.minus(1, DateTimeUnit.DAY) -> "Вчера"
                else -> startLocal.date.toString()
            }
            val timeStr = "%02d:%02d".format(startLocal.hour, startLocal.minute)
            start to ReviewLessonUi(
                lessonId = lesson.id,
                studentName = student.name,
                course = student.course,
                whenLabel = "$dayWord, $timeStr"
            )
        }.sortedBy { it.first }.map { it.second }
    }
}

package com.application.cadence.presentation.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.cadence.R
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.AgendaCardUi
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.buildLessonCards
import com.application.cadence.presentation.common.dateFull
import com.application.cadence.presentation.common.lessonCountByDate
import com.application.cadence.presentation.common.numberLessons
import com.application.cadence.presentation.common.peopleWord
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

data class ReviewLessonUi(
    val lessonId: Long,
    val studentName: String,
    val course: String,
    val whenLabel: String,
    val groupId: Long? = null,
    val lessonIds: List<Long> = emptyList()
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
    val lessons: List<AgendaCardUi>
)

private data class EnrichedLesson(
    val date: LocalDate,
    val start: Instant,
    val time: LocalTime,
    val inReview: Boolean,
    val lesson: Lesson,
    val student: Student
)

class TodayViewModel(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val ctx: Context
) : ViewModel() {

    private val tutorTz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tutorTz)
    private val mskToday = Clock.System.todayIn(MSK)

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = today
    }

    fun shiftWeek(deltaWeeks: Int) {
        _selectedDate.value = _selectedDate.value.plus(deltaWeeks * 7, DateTimeUnit.DAY)
    }

    val dayState: StateFlow<DayUi> = combine(
        _selectedDate,
        lessonRepository.observeAll(),
        studentRepository.observeAll()
    ) { selected, lessons, students ->
        val weekStart = selected.minus(selected.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        buildDayUi(selected, weekStart, lessons, students)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DayUi(monthNominative(ctx, today.monthNumber), emptyList(), "", emptyList())
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

    fun resolveAll(lessonIds: List<Long>, status: LessonStatus) {
        viewModelScope.launch {
            lessonIds.forEach { id ->
                val lesson = lessonRepository.getById(id) ?: return@forEach
                lessonRepository.update(lesson.copy(status = status, paid = false))
            }
        }
    }

    private fun buildDayUi(
        selected: LocalDate,
        weekStart: LocalDate,
        lessons: List<Lesson>,
        students: List<Student>
    ): DayUi {
        val byId = students.associateBy { it.id }
        val numberByLessonId = numberLessons(lessons)
        val now = Clock.System.now()
        val enriched = lessons.mapNotNull { lesson ->
            val student = byId[lesson.studentId] ?: return@mapNotNull null
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
            val start = LocalDateTime(lesson.date, time).toInstant(MSK)
            val end = start + lesson.durationMinutes.minutes
            val localDate = start.toLocalDateTime(tutorTz).date
            val inReview = lesson.status == LessonStatus.SCHEDULED && end < now
            EnrichedLesson(localDate, start, time, inReview, lesson, student)
        }
        val countByDate = lessonCountByDate(lessons, tutorTz)
        val selectedDayLessons = enriched
            .filter { it.date == selected && !it.inReview }
            .map { it.lesson }
        val selectedLessons = buildLessonCards(ctx, selectedDayLessons, byId, tutorTz, numberByLessonId)

        val week = (0..6).map { offset ->
            val date = weekStart.plus(offset, DateTimeUnit.DAY)
            val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
            WeekDayUi(
                date = date,
                shortLabel = weekdayShort(ctx, weekday),
                dayNumber = date.dayOfMonth,
                lessonCount = countByDate[date] ?: 0,
                isSelected = date == selected,
                isToday = date == today
            )
        }

        val selectedWeekday = Weekday.entries[selected.dayOfWeek.isoDayNumber - 1]
        val selectedLabel =
            dateFull(ctx, weekdayLabel(ctx, selectedWeekday), selected.dayOfMonth, monthGenitive(ctx, selected.monthNumber))

        return DayUi(monthNominative(ctx, selected.monthNumber), week, selectedLabel, selectedLessons)
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
            EnrichedLesson(start.toLocalDateTime(tutorTz).date, start, time, true, lesson, student)
        }
            .sortedBy { it.start }
            .groupBy { it.lesson.groupId ?: -it.lesson.id - 1 }
            .values
            .map { rows -> buildReviewUi(rows) }
    }

    private fun buildReviewUi(rows: List<EnrichedLesson>): ReviewLessonUi {
        val first = rows.first()
        val startLocal = first.start.toLocalDateTime(tutorTz)
        val dayWord = when (startLocal.date) {
            today -> ctx.getString(R.string.today_word)
            today.minus(1, DateTimeUnit.DAY) -> ctx.getString(R.string.yesterday_word)
            else -> startLocal.date.toString()
        }
        val timeStr = "%02d:%02d".format(startLocal.hour, startLocal.minute)
        val isGroup = rows.size > 1
        return ReviewLessonUi(
            lessonId = first.lesson.id,
            studentName = if (isGroup) rows.joinToString(", ") { it.student.name } else first.student.name,
            course = if (isGroup) ctx.getString(R.string.group_label, rows.size, peopleWord(ctx, rows.size)) else first.student.course,
            whenLabel = "$dayWord, $timeStr",
            groupId = if (isGroup) first.lesson.groupId else null,
            lessonIds = rows.map { it.lesson.id }
        )
    }
}

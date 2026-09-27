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
    val paid: Boolean,
    val groupId: Long? = null
)

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
    val lessons: List<TodayLessonUi>
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
    private val studentRepository: StudentRepository
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
        val countByDate = enriched.groupingBy { it.date }.eachCount()
        val selectedLessons = enriched
            .filter { it.date == selected && !it.inReview }
            .sortedBy { it.start }
            .groupBy { it.lesson.groupId ?: -it.lesson.id - 1 }
            .values
            .map { rows -> buildCardUi(rows, numberByLessonId) }

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

    private fun buildCardUi(rows: List<EnrichedLesson>, numberByLessonId: Map<Long, Int>): TodayLessonUi {
        val first = rows.first()
        val lesson = first.lesson
        val start = first.start
        val time = first.time
        val end = start + lesson.durationMinutes.minutes
        val startLocal = start.toLocalDateTime(tutorTz)
        val endLocal = end.toLocalDateTime(tutorTz)
        val isGroup = rows.size > 1

        val statuses = rows.map { it.lesson.status }
        val status = when {
            statuses.any { it == LessonStatus.SCHEDULED } -> LessonStatus.SCHEDULED
            statuses.all { it == LessonStatus.CANCELLED } -> LessonStatus.CANCELLED
            else -> LessonStatus.HELD
        }
        val paid = if (isGroup) {
            rows.filter { it.lesson.status == LessonStatus.HELD }.all { it.lesson.paid }
        } else {
            lesson.paid
        }

        return TodayLessonUi(
            lessonId = lesson.id,
            studentId = lesson.studentId,
            studentName = if (isGroup) rows.joinToString(", ") { it.student.name } else first.student.name,
            course = if (isGroup) "Группа · ${rows.size}" else first.student.course,
            time = "%02d:%02d".format(startLocal.hour, startLocal.minute),
            endTime = "%02d:%02d".format(endLocal.hour, endLocal.minute),
            mskTime = if (tutorTz.id == MSK.id) null else "%02d:%02d МСК".format(time.hour, time.minute),
            status = status,
            lessonNumber = if (isGroup) null else numberByLessonId[lesson.id],
            paid = paid,
            groupId = if (isGroup) lesson.groupId else null
        )
    }

    /**
     * Derives each lesson's display number: its 1-based position by date/time among the
     * student's non-cancelled lessons. Deleting or cancelling a lesson renumbers the rest.
     */
    private fun numberLessons(lessons: List<Lesson>): Map<Long, Int> =
        lessons
            .filter { it.status != LessonStatus.CANCELLED }
            .groupBy { it.studentId }
            .flatMap { (_, group) ->
                group
                    .sortedWith(compareBy({ it.date }, { it.time }))
                    .mapIndexed { index, lesson -> lesson.id to (index + 1) }
            }
            .toMap()

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
            today -> "Сегодня"
            today.minus(1, DateTimeUnit.DAY) -> "Вчера"
            else -> startLocal.date.toString()
        }
        val timeStr = "%02d:%02d".format(startLocal.hour, startLocal.minute)
        val isGroup = rows.size > 1
        return ReviewLessonUi(
            lessonId = first.lesson.id,
            studentName = if (isGroup) rows.joinToString(", ") { it.student.name } else first.student.name,
            course = if (isGroup) "Группа · ${rows.size}" else first.student.course,
            whenLabel = "$dayWord, $timeStr",
            groupId = if (isGroup) first.lesson.groupId else null,
            lessonIds = rows.map { it.lesson.id }
        )
    }
}

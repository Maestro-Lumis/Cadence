package com.application.cadence.presentation.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.StudentRepository
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.AgendaCardUi
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.buildLessonCards
import com.application.cadence.presentation.common.lessonCountByDate
import com.application.cadence.presentation.common.monthGenitive
import com.application.cadence.presentation.common.monthNominative
import com.application.cadence.presentation.common.numberLessons
import com.application.cadence.presentation.common.weekdayLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

data class CalDayUi(
    val date: LocalDate,
    val dayNumber: Int,
    val inMonth: Boolean,
    val lessonCount: Int,
    val isSelected: Boolean,
    val isToday: Boolean
)

data class AgendaDayUi(
    val label: String,
    val lessons: List<AgendaCardUi>
)

data class CalendarUi(
    val monthTitle: String,
    val weeks: List<List<CalDayUi>>,
    val selectedLabel: String,
    val lessons: List<AgendaCardUi>,
    val agenda: List<AgendaDayUi>
)

class CalendarViewModel(
    lessonRepository: LessonRepository,
    studentRepository: StudentRepository,
    private val ctx: Context
) : ViewModel() {

    private val tutorTz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tutorTz)

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun shiftMonth(delta: Int) {
        val current = _selectedDate.value
        val target = LocalDate(current.year, current.monthNumber, 1).plus(delta, DateTimeUnit.MONTH)
        val lastDay = target.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
        _selectedDate.value = LocalDate(target.year, target.monthNumber, current.dayOfMonth.coerceAtMost(lastDay))
    }

    val uiState: StateFlow<CalendarUi> = combine(
        _selectedDate,
        lessonRepository.observeAll(),
        studentRepository.observeAll()
    ) { selected, lessons, students ->
        val byId = students.associateBy { it.id }
        val numberByLessonId = numberLessons(lessons)
        val countByDate = lessonCountByDate(lessons, tutorTz)

        val monthStart = LocalDate(selected.year, selected.monthNumber, 1)
        val gridStart = monthStart.minus(monthStart.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val weeks = (0..5).map { week ->
            (0..6).map { dow ->
                val date = gridStart.plus(week * 7 + dow, DateTimeUnit.DAY)
                CalDayUi(
                    date = date,
                    dayNumber = date.dayOfMonth,
                    inMonth = date.monthNumber == selected.monthNumber && date.year == selected.year,
                    lessonCount = countByDate[date] ?: 0,
                    isSelected = date == selected,
                    isToday = date == today
                )
            }
        }

        val dayLessons = lessons.filter { lesson ->
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@filter false
            LocalDateTime(lesson.date, time).toInstant(MSK).toLocalDateTime(tutorTz).date == selected
        }
        val cards = buildLessonCards(ctx, dayLessons, byId, tutorTz, numberByLessonId)

        val agenda = lessons
            .mapNotNull { lesson ->
                val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
                val date = LocalDateTime(lesson.date, time).toInstant(MSK).toLocalDateTime(tutorTz).date
                if (date.year == selected.year && date.monthNumber == selected.monthNumber) date to lesson else null
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { (date, dayLessons) ->
                val weekday = Weekday.entries[date.dayOfWeek.isoDayNumber - 1]
                AgendaDayUi(
                    label = "${weekdayLabel(ctx, weekday)}, ${date.dayOfMonth} ${monthGenitive(ctx, date.monthNumber)}",
                    lessons = buildLessonCards(ctx, dayLessons, byId, tutorTz, numberByLessonId)
                )
            }

        val selectedWeekday = Weekday.entries[selected.dayOfWeek.isoDayNumber - 1]
        CalendarUi(
            monthTitle = "${monthNominative(ctx, selected.monthNumber)} ${selected.year}",
            weeks = weeks,
            selectedLabel = "${weekdayLabel(ctx, selectedWeekday)}, ${selected.dayOfMonth} ${monthGenitive(ctx, selected.monthNumber)}",
            lessons = cards,
            agenda = agenda
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUi("", emptyList(), "", emptyList(), emptyList())
    )
}

class CalendarViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val ctx: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalendarViewModel(lessonRepository, studentRepository, ctx) as T
    }
}

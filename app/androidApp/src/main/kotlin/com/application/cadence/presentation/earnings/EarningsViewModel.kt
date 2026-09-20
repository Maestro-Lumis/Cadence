package com.application.cadence.presentation.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.PeriodPreset
import com.application.cadence.presentation.common.monthGenitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class EarningsRowUi(
    val studentId: Long,
    val name: String,
    val heldCount: Int,
    val earned: Int,
    val paid: Int,
    val debt: Int
)

data class EarningsUi(
    val periodLabel: String,
    val rows: List<EarningsRowUi>,
    val totalEarned: Int,
    val totalPaid: Int,
    val totalDebt: Int
)

class EarningsViewModel(
    studentRepository: StudentRepository,
    lessonRepository: LessonRepository
) : ViewModel() {

    private val mskToday = Clock.System.todayIn(MSK)

    private val _from = MutableStateFlow(LocalDate(mskToday.year, mskToday.monthNumber, 1))
    private val _to = MutableStateFlow(mskToday)

    val from: StateFlow<LocalDate> = _from
    val to: StateFlow<LocalDate> = _to

    fun setFrom(date: LocalDate) {
        _from.value = date
    }

    fun setTo(date: LocalDate) {
        _to.value = date
    }

    fun applyPreset(preset: PeriodPreset) {
        val (from, to) = preset.range(mskToday)
        _from.value = from
        _to.value = to
    }

    val uiState: StateFlow<EarningsUi> = combine(
        studentRepository.observeAll(),
        lessonRepository.observeAll(),
        _from,
        _to
    ) { students, lessons, from, to ->
        val byStudent = lessons.groupBy { it.studentId }
        val rows = students.mapNotNull { student ->
            val held = byStudent[student.id].orEmpty()
                .filter { it.status == LessonStatus.HELD && it.date >= from && it.date <= to }
            if (held.isEmpty()) return@mapNotNull null

            val earned = held.sumOf { (student.hourlyRate * it.durationMinutes) / 60 }
            val paid = held.filter { it.paid }.sumOf { (student.hourlyRate * it.durationMinutes) / 60 }
            EarningsRowUi(
                studentId = student.id,
                name = student.name,
                heldCount = held.size,
                earned = earned,
                paid = paid,
                debt = earned - paid
            )
        }.sortedByDescending { it.earned }

        EarningsUi(
            periodLabel = periodLabel(from, to),
            rows = rows,
            totalEarned = rows.sumOf { it.earned },
            totalPaid = rows.sumOf { it.paid },
            totalDebt = rows.sumOf { it.debt }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EarningsUi("", emptyList(), 0, 0, 0)
    )

    private fun periodLabel(from: LocalDate, to: LocalDate): String {
        val fromStr = "${from.dayOfMonth} ${monthGenitive(from.monthNumber)}"
        val toStr = "${to.dayOfMonth} ${monthGenitive(to.monthNumber)} ${to.year}"
        return "$fromStr – $toStr"
    }
}

class EarningsViewModelFactory(
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EarningsViewModel(studentRepository, lessonRepository) as T
    }
}

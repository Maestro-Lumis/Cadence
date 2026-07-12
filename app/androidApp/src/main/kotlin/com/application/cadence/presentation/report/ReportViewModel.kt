package com.application.cadence.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.formatDuration
import com.application.cadence.presentation.common.monthGenitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class ReportRowUi(
    val dateLabel: String,
    val durationLabel: String
)

data class ReportUi(
    val studentName: String,
    val course: String,
    val periodLabel: String,
    val rows: List<ReportRowUi>,
    val totalCount: Int,
    val totalLabel: String,
    val fileName: String
)

class ReportViewModel(
    studentId: Long,
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

    val uiState: StateFlow<ReportUi?> = combine(
        studentRepository.observeById(studentId),
        lessonRepository.observeByStudent(studentId),
        _from,
        _to
    ) { student, lessons, from, to ->
        student?.let { s ->
            val held = lessons
                .filter { it.status == LessonStatus.HELD && it.date >= from && it.date <= to }
                .sortedBy { it.date }
            val rows = held.map { lesson ->
                ReportRowUi(
                    dateLabel = "${lesson.date.dayOfMonth} ${monthGenitive(lesson.date.monthNumber)}",
                    durationLabel = formatDuration(lesson.durationMinutes)
                )
            }
            val totalMinutes = held.sumOf { it.durationMinutes }
            ReportUi(
                studentName = s.name,
                course = s.course,
                periodLabel = periodLabel(from, to),
                rows = rows,
                totalCount = held.size,
                totalLabel = formatDuration(totalMinutes),
                fileName = "${sanitizeFileName(s.name)}_${from}_$to.pdf"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun sanitizeFileName(name: String): String =
        name.trim().replace(Regex("""[\\/:*?"<>|\s]+"""), "_").ifBlank { "otchet" }

    private fun periodLabel(from: LocalDate, to: LocalDate): String {
        val fromStr = "${from.dayOfMonth} ${monthGenitive(from.monthNumber)}"
        val toStr = "${to.dayOfMonth} ${monthGenitive(to.monthNumber)} ${to.year}"
        return "$fromStr – $toStr"
    }
}

class ReportViewModelFactory(
    private val studentId: Long,
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReportViewModel(studentId, studentRepository, lessonRepository) as T
    }
}

package com.application.cadence.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.monthNominative
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class ProfileUi(
    val studentCount: Int,
    val debtTotal: Int,
    val debtorCount: Int,
    val earningsMonthLabel: String,
    val earningsMonthTotal: Int
)

class ProfileViewModel(
    private val ctx: Context,
    studentRepository: StudentRepository,
    lessonRepository: LessonRepository
) : ViewModel() {

    private val mskToday = Clock.System.todayIn(MSK)

    val uiState: StateFlow<ProfileUi> = combine(
        studentRepository.observeAll(),
        lessonRepository.observeAll()
    ) { students, lessons ->
        val byId = students.associateBy { it.id }

        val unpaid = lessons.filter { it.status == LessonStatus.HELD && !it.paid }
        val debtTotal = unpaid.sumOf { l -> byId[l.studentId]?.let { (it.hourlyRate * l.durationMinutes) / 60 } ?: 0 }
        val debtorCount = unpaid.map { it.studentId }.distinct().size

        val monthHeld = lessons.filter {
            it.status == LessonStatus.HELD &&
                it.date.year == mskToday.year &&
                it.date.monthNumber == mskToday.monthNumber
        }
        val earned = monthHeld.sumOf { l -> byId[l.studentId]?.let { (it.hourlyRate * l.durationMinutes) / 60 } ?: 0 }

        ProfileUi(
            studentCount = students.size,
            debtTotal = debtTotal,
            debtorCount = debtorCount,
            earningsMonthLabel = monthNominative(ctx, mskToday.monthNumber),
            earningsMonthTotal = earned
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUi(0, 0, 0, "", 0))
}

class ProfileViewModelFactory(
    private val ctx: Context,
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(ctx, studentRepository, lessonRepository) as T
    }
}

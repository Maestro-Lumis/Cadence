package com.application.cadence.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtRowUi(
    val studentId: Long,
    val name: String,
    val course: String,
    val unpaidCount: Int,
    val oldestDate: String
)

class DebtsViewModel(
    private val lessonRepository: LessonRepository,
    studentRepository: StudentRepository
) : ViewModel() {

    fun markPaid(studentId: Long) {
        viewModelScope.launch {
            lessonRepository.markStudentDebtsPaid(studentId)
        }
    }

    val uiState: StateFlow<List<DebtRowUi>> = combine(
        lessonRepository.observeUnpaidHeld(),
        studentRepository.observeAll()
    ) { lessons, students ->
        val byId = students.associateBy { it.id }
        lessons.groupBy { it.studentId }.mapNotNull { (studentId, group) ->
            val student = byId[studentId] ?: return@mapNotNull null
            DebtRowUi(
                studentId = studentId,
                name = student.name,
                course = student.course,
                unpaidCount = group.size,
                oldestDate = group.minOf { it.date }.toString()
            )
        }.sortedBy { it.oldestDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class DebtsViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DebtsViewModel(lessonRepository, studentRepository) as T
    }
}

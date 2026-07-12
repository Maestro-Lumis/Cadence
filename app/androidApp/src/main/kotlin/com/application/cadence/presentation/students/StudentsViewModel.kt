package com.application.cadence.presentation.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StudentRowUi(
    val id: Long,
    val name: String,
    val course: String,
    val timezone: String
)

class StudentsViewModel(
    studentRepository: StudentRepository,
    lessonRepository: LessonRepository
) : ViewModel() {

    val uiState: StateFlow<List<StudentRowUi>> = studentRepository.observeAll()
        .map { list -> list.map { StudentRowUi(it.id, it.name, it.course, it.timezone) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtCount: StateFlow<Int> = lessonRepository.observeUnpaidHeld()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

class StudentsViewModelFactory(
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StudentsViewModel(studentRepository, lessonRepository) as T
    }
}

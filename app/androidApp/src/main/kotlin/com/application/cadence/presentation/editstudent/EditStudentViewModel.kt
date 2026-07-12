package com.application.cadence.presentation.editstudent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditStudentViewModel(
    studentId: Long,
    private val studentRepository: StudentRepository
) : ViewModel() {

    val student: StateFlow<Student?> = studentRepository.observeById(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(name: String, course: String, timezone: String, onSaved: () -> Unit) {
        if (name.isBlank()) return
        val current = student.value ?: return
        viewModelScope.launch {
            studentRepository.update(
                current.copy(
                    name = name.trim(),
                    course = course.trim().ifBlank { "Без курса" },
                    timezone = timezone
                )
            )
            onSaved()
        }
    }
}

class EditStudentViewModelFactory(
    private val studentId: Long,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditStudentViewModel(studentId, studentRepository) as T
    }
}

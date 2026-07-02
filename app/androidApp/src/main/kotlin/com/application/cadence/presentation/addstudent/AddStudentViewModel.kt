package com.application.cadence.presentation.addstudent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AddStudentViewModel(
    private val studentRepository: StudentRepository
) : ViewModel() {

    fun save(name: String, course: String, timezone: String, onSaved: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            studentRepository.add(
                Student(
                    id = 0,
                    name = name.trim(),
                    course = course.trim().ifBlank { "Без курса" },
                    timezone = timezone,
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            )
            onSaved()
        }
    }
}

class AddStudentViewModelFactory(
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddStudentViewModel(studentRepository) as T
    }
}
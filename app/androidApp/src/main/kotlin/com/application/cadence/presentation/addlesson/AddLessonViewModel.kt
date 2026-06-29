package com.application.cadence.presentation.addlesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class AddLessonViewModel(
    private val lessonRepository: LessonRepository,
    studentRepository: StudentRepository
) : ViewModel() {

    val students: StateFlow<List<Student>> = studentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        studentId: Long,
        date: LocalDate,
        time: String,
        status: LessonStatus,
        lessonNumber: Int?,
        paid: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            lessonRepository.add(
                Lesson(
                    id = 0,
                    studentId = studentId,
                    date = date,
                    time = time,
                    status = status,
                    lessonNumber = lessonNumber,
                    packageId = null,
                    paid = paid
                )
            )
            onSaved()
        }
    }
}

class AddLessonViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddLessonViewModel(lessonRepository, studentRepository) as T
    }
}
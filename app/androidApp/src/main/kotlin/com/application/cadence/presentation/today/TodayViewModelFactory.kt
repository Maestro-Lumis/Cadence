package com.application.cadence.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.StudentRepository

class TodayViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TodayViewModel(lessonRepository, studentRepository) as T
    }
}
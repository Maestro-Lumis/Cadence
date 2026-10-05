package com.application.cadence.presentation.addstudent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.R
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AddStudentViewModel(
    private val ctx: Context,
    private val studentRepository: StudentRepository
) : ViewModel() {

    fun save(
        name: String,
        course: String,
        timezone: String,
        hourlyRate: Int,
        lessonDurationMinutes: Int,
        onSaved: () -> Unit
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            studentRepository.add(
                Student(
                    id = 0,
                    name = name.trim(),
                    course = course.trim().ifBlank { ctx.getString(R.string.student_default_course) },
                    timezone = timezone,
                    hourlyRate = hourlyRate,
                    lessonDurationMinutes = lessonDurationMinutes.coerceAtLeast(1),
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            )
            onSaved()
        }
    }
}

class AddStudentViewModelFactory(
    private val ctx: Context,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddStudentViewModel(ctx, studentRepository) as T
    }
}
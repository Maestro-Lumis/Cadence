package com.application.cadence.presentation.studentprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentProfileUi(
    val studentName: String,
    val course: String,
    val timezone: String,
    val totalLessons: Int,
    val heldLessons: Int,
    val unpaidLessons: Int,
    val history: List<Lesson>
)

class StudentProfileViewModel(
    private val studentId: Long,
    private val studentRepository: StudentRepository,
    lessonRepository: LessonRepository
) : ViewModel() {

    val uiState: StateFlow<StudentProfileUi?> = combine(
        studentRepository.observeById(studentId),
        lessonRepository.observeByStudent(studentId)
    ) { student, lessons ->
        student?.let {
            StudentProfileUi(
                studentName = it.name,
                course = it.course,
                timezone = it.timezone,
                totalLessons = lessons.size,
                heldLessons = lessons.count { l -> l.status == LessonStatus.HELD },
                unpaidLessons = lessons.count { l -> l.status != LessonStatus.CANCELLED && !l.paid },
                history = lessons.sortedByDescending { l -> l.date }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            studentRepository.delete(studentId)
            onDeleted()
        }
    }
}

class StudentProfileViewModelFactory(
    private val studentId: Long,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StudentProfileViewModel(studentId, studentRepository, lessonRepository) as T
    }
}

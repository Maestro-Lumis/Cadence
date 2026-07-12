package com.application.cadence.presentation.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class StudentRowUi(
    val id: Long,
    val name: String,
    val course: String,
    val timezone: String
)

data class LostStudentUi(
    val id: Long,
    val name: String,
    val detail: String
)

private const val LOST_THRESHOLD_DAYS = 14

class StudentsViewModel(
    studentRepository: StudentRepository,
    lessonRepository: LessonRepository
) : ViewModel() {

    private val today = Clock.System.todayIn(MSK)

    val uiState: StateFlow<List<StudentRowUi>> = studentRepository.observeAll()
        .map { list -> list.map { StudentRowUi(it.id, it.name, it.course, it.timezone) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtCount: StateFlow<Int> = lessonRepository.observeUnpaidHeld()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lostStudents: StateFlow<List<LostStudentUi>> = combine(
        studentRepository.observeAll(),
        lessonRepository.observeAll()
    ) { students, lessons ->
        buildLost(students, lessons)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildLost(students: List<Student>, lessons: List<Lesson>): List<LostStudentUi> {
        val byStudent = lessons.groupBy { it.studentId }
        return students.mapNotNull { student ->
            val theirs = byStudent[student.id].orEmpty()
            if (theirs.isEmpty()) return@mapNotNull null

            val hasUpcoming = theirs.any { it.status == LessonStatus.SCHEDULED && it.date >= today }
            if (hasUpcoming) return@mapNotNull null

            val lastDate = theirs.maxOf { it.date }
            val daysSince = lastDate.daysUntil(today)
            if (daysSince < LOST_THRESHOLD_DAYS) return@mapNotNull null

            LostStudentUi(
                id = student.id,
                name = student.name,
                detail = "Последнее занятие $lastDate"
            ) to daysSince
        }.sortedByDescending { it.second }.map { it.first }
    }
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

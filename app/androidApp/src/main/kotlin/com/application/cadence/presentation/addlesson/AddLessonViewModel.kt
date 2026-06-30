package com.application.cadence.presentation.addlesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonPackage
import com.application.cadence.core.LessonPackageRepository
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddLessonViewModel(
    private val lessonRepository: LessonRepository,
    studentRepository: StudentRepository,
    packageRepository: LessonPackageRepository
) : ViewModel() {

    val students: StateFlow<List<Student>> = studentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedStudentId = MutableStateFlow<Long?>(null)

    val packages: StateFlow<List<LessonPackage>> = selectedStudentId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else packageRepository.observeByStudent(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectStudent(studentId: Long?) {
        selectedStudentId.value = studentId
    }

    fun save(
        studentId: Long,
        date: LocalDate,
        time: String,
        status: LessonStatus,
        lessonNumber: Int?,
        packageId: Long?,
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
                    packageId = packageId,
                    paid = if (packageId != null) false else paid
                )
            )
            onSaved()
        }
    }
}

class AddLessonViewModelFactory(
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val packageRepository: LessonPackageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddLessonViewModel(lessonRepository, studentRepository, packageRepository) as T
    }
}
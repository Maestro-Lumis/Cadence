package com.application.cadence.presentation.studentprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonPackageRepository
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PackageUi(
    val id: Long,
    val total: Int,
    val used: Int,
    val paid: Boolean
)

data class HistoryItemUi(
    val lesson: Lesson,
    val unpaid: Boolean
)

data class StudentProfileUi(
    val studentName: String,
    val course: String,
    val totalLessons: Int,
    val heldLessons: Int,
    val unpaidLessons: Int,
    val packages: List<PackageUi>,
    val history: List<HistoryItemUi>
)

class StudentProfileViewModel(
    studentId: Long,
    studentRepository: StudentRepository,
    lessonRepository: LessonRepository,
    packageRepository: LessonPackageRepository
) : ViewModel() {

    val uiState: StateFlow<StudentProfileUi?> = combine(
        studentRepository.observeById(studentId),
        lessonRepository.observeByStudent(studentId),
        packageRepository.observeByStudent(studentId)
    ) { student, lessons, packages ->
        student?.let {
            val paidByPackageId = packages.associate { p -> p.id to p.paid }
            fun Lesson.isUnpaid(): Boolean {
                if (status == LessonStatus.CANCELLED) return false
                val pkgPaid = packageId?.let { paidByPackageId[it] }
                return if (pkgPaid != null) !pkgPaid else !paid
            }
            val packageUi = packages.map { p ->
                PackageUi(
                    id = p.id,
                    total = p.totalLessons,
                    used = lessons.count { l -> l.packageId == p.id && l.status == LessonStatus.HELD },
                    paid = p.paid
                )
            }
            StudentProfileUi(
                studentName = it.name,
                course = it.course,
                totalLessons = lessons.size,
                heldLessons = lessons.count { l -> l.status == LessonStatus.HELD },
                unpaidLessons = lessons.count { l -> l.isUnpaid() },
                packages = packageUi,
                history = lessons.sortedByDescending { l -> l.date }
                    .map { l -> HistoryItemUi(l, l.isUnpaid()) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

class StudentProfileViewModelFactory(
    private val studentId: Long,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val packageRepository: LessonPackageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StudentProfileViewModel(studentId, studentRepository, lessonRepository, packageRepository) as T
    }
}
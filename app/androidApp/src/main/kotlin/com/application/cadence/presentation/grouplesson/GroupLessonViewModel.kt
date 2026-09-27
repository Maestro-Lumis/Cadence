package com.application.cadence.presentation.grouplesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonRepository
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.StudentRepository
import com.application.cadence.presentation.common.formatDuration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupParticipantUi(
    val lessonId: Long,
    val name: String,
    val course: String,
    val status: LessonStatus,
    val paid: Boolean
)

data class GroupLessonUi(
    val dateLabel: String,
    val timeLabel: String,
    val durationLabel: String,
    val participants: List<GroupParticipantUi>,
    val addable: List<Pair<Long, String>>
)

class GroupLessonViewModel(
    private val groupId: Long,
    studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    val uiState: StateFlow<GroupLessonUi?> = combine(
        lessonRepository.observeByGroup(groupId),
        studentRepository.observeAll()
    ) { lessons, students ->
        if (lessons.isEmpty()) return@combine null
        val byId = students.associateBy { it.id }
        val ordered = lessons.sortedBy { it.id }
        val first = ordered.first()
        val participants = ordered.mapNotNull { l ->
            byId[l.studentId]?.let { s ->
                GroupParticipantUi(l.id, s.name, s.course, l.status, l.paid)
            }
        }
        val memberIds = lessons.map { it.studentId }.toSet()
        val addable = students.filter { it.id !in memberIds }.map { it.id to it.name }
        GroupLessonUi(
            dateLabel = first.date.toString(),
            timeLabel = "${first.time} МСК",
            durationLabel = formatDuration(first.durationMinutes),
            participants = participants,
            addable = addable
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAttendance(lessonId: Long, attended: Boolean) {
        viewModelScope.launch {
            val lesson = lessonRepository.getById(lessonId) ?: return@launch
            val status = if (attended) LessonStatus.HELD else LessonStatus.CANCELLED
            lessonRepository.update(lesson.copy(status = status))
        }
    }

    fun setPaid(lessonId: Long, paid: Boolean) {
        viewModelScope.launch {
            val lesson = lessonRepository.getById(lessonId) ?: return@launch
            lessonRepository.update(lesson.copy(paid = paid))
        }
    }

    fun addStudent(studentId: Long) {
        viewModelScope.launch {
            val template = lessonRepository.observeByGroup(groupId).first().firstOrNull() ?: return@launch
            lessonRepository.add(
                Lesson(
                    id = 0,
                    studentId = studentId,
                    date = template.date,
                    time = template.time,
                    durationMinutes = template.durationMinutes,
                    status = LessonStatus.SCHEDULED,
                    lessonNumber = null,
                    packageId = null,
                    paid = false,
                    groupId = groupId
                )
            )
        }
    }

    fun removeStudent(lessonId: Long) {
        viewModelScope.launch {
            lessonRepository.delete(lessonId)
        }
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            lessonRepository.observeByGroup(groupId).first().forEach { lessonRepository.delete(it.id) }
            onDeleted()
        }
    }
}

class GroupLessonViewModelFactory(
    private val groupId: Long,
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GroupLessonViewModel(groupId, studentRepository, lessonRepository) as T
    }
}

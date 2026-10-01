package com.application.cadence.presentation.common

import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** One agenda card: either a single lesson or a collapsed group. Shared by Today and Calendar. */
data class AgendaCardUi(
    val lessonId: Long,
    val studentId: Long,
    val studentName: String,
    val course: String,
    val time: String,
    val endTime: String,
    val durationLabel: String,
    val mskTime: String?,
    val status: LessonStatus,
    val lessonNumber: Int?,
    val paid: Boolean,
    val groupId: Long? = null
)

/** 1-based lesson number per student, by date/time, ignoring cancelled lessons. */
fun numberLessons(lessons: List<Lesson>): Map<Long, Int> =
    lessons
        .filter { it.status != LessonStatus.CANCELLED }
        .groupBy { it.studentId }
        .flatMap { (_, group) ->
            group
                .sortedWith(compareBy({ it.date }, { it.time }))
                .mapIndexed { index, lesson -> lesson.id to (index + 1) }
        }
        .toMap()

/** Distinct lessons per local date, counting a whole group as one. */
fun lessonCountByDate(lessons: List<Lesson>, displayTz: TimeZone): Map<LocalDate, Int> =
    lessons
        .mapNotNull { l ->
            val t = runCatching { LocalTime.parse(l.time) }.getOrNull() ?: return@mapNotNull null
            val date = LocalDateTime(l.date, t).toInstant(MSK).toLocalDateTime(displayTz).date
            date to (l.groupId ?: -l.id - 1)
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.distinct().size }

private data class Row(val start: Instant, val time: LocalTime, val lesson: Lesson, val student: Student)

/** Builds agenda cards from a set of lessons, collapsing each group into one card, sorted by start. */
fun buildLessonCards(
    lessons: List<Lesson>,
    students: Map<Long, Student>,
    displayTz: TimeZone,
    numberByLessonId: Map<Long, Int>
): List<AgendaCardUi> {
    val rows = lessons.mapNotNull { lesson ->
        val student = students[lesson.studentId] ?: return@mapNotNull null
        val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return@mapNotNull null
        val start = LocalDateTime(lesson.date, time).toInstant(MSK)
        Row(start, time, lesson, student)
    }.sortedBy { it.start }

    return rows
        .groupBy { it.lesson.groupId ?: -it.lesson.id - 1 }
        .values
        .map { group -> buildCard(group, displayTz, numberByLessonId) }
}

private fun buildCard(rows: List<Row>, displayTz: TimeZone, numberByLessonId: Map<Long, Int>): AgendaCardUi {
    val first = rows.first()
    val lesson = first.lesson
    val start = first.start
    val time = first.time
    val end = start + lesson.durationMinutes.minutes
    val startLocal = start.toLocalDateTime(displayTz)
    val endLocal = end.toLocalDateTime(displayTz)
    val isGroup = rows.size > 1

    val statuses = rows.map { it.lesson.status }
    val status = when {
        statuses.any { it == LessonStatus.SCHEDULED } -> LessonStatus.SCHEDULED
        statuses.all { it == LessonStatus.CANCELLED } -> LessonStatus.CANCELLED
        else -> LessonStatus.HELD
    }
    val paid = if (isGroup) {
        rows.filter { it.lesson.status == LessonStatus.HELD }.all { it.lesson.paid }
    } else {
        lesson.paid
    }

    return AgendaCardUi(
        lessonId = lesson.id,
        studentId = lesson.studentId,
        studentName = if (isGroup) rows.joinToString(", ") { it.student.name } else first.student.name,
        course = if (isGroup) "Группа · ${rows.size} ${peopleWord(rows.size)}" else first.student.course,
        time = "%02d:%02d".format(startLocal.hour, startLocal.minute),
        endTime = "%02d:%02d".format(endLocal.hour, endLocal.minute),
        durationLabel = formatDuration(lesson.durationMinutes),
        mskTime = if (displayTz.id == MSK.id) null else "%02d:%02d МСК".format(time.hour, time.minute),
        status = status,
        lessonNumber = if (isGroup) null else numberByLessonId[lesson.id],
        paid = paid,
        groupId = if (isGroup) lesson.groupId else null
    )
}

fun peopleWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "человек"
        mod10 in 2..4 && mod100 !in 12..14 -> "человека"
        else -> "человек"
    }
}

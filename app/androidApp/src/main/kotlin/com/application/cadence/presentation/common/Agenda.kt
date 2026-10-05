package com.application.cadence.presentation.common

import android.content.Context
import com.application.cadence.R
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

fun buildLessonCards(
    ctx: Context,
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
        .map { group -> buildCard(ctx, group, displayTz, numberByLessonId) }
}

private fun buildCard(ctx: Context, rows: List<Row>, displayTz: TimeZone, numberByLessonId: Map<Long, Int>): AgendaCardUi {
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

    val pw = peopleWord(ctx, rows.size)
    return AgendaCardUi(
        lessonId = lesson.id,
        studentId = lesson.studentId,
        studentName = if (isGroup) rows.joinToString(", ") { it.student.name } else first.student.name,
        course = if (isGroup) ctx.getString(R.string.group_label, rows.size, pw) else first.student.course,
        time = "%02d:%02d".format(startLocal.hour, startLocal.minute),
        endTime = "%02d:%02d".format(endLocal.hour, endLocal.minute),
        durationLabel = formatDuration(ctx, lesson.durationMinutes),
        mskTime = if (displayTz.id == MSK.id) null else ctx.getString(R.string.msk_time, time.hour, time.minute),
        status = status,
        lessonNumber = if (isGroup) null else numberByLessonId[lesson.id],
        paid = paid,
        groupId = if (isGroup) lesson.groupId else null
    )
}

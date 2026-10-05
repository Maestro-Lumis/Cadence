package com.application.cadence.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.application.cadence.R
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.MSK
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object NotificationScheduler {

    const val CHANNEL_ID = "lesson_reminders"
    const val LEAD_MINUTES = 60
    private const val HORIZON_DAYS = 30

    const val LESSON_ID_KEY = "lessonId"
    const val KIND_KEY = "kind"
    const val KIND_REMINDER = "reminder"
    const val KIND_REVIEW = "review"

    private const val REMINDER_PREFIX = "lesson-reminder-"
    private const val REVIEW_PREFIX = "lesson-review-"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun sync(context: Context, lessons: List<Lesson>) {
        val wm = WorkManager.getInstance(context)
        val now = Clock.System.now()
        val horizon = now + HORIZON_DAYS.days
        val seenGroups = mutableSetOf<Long>()

        lessons.forEach { lesson ->
            val gid = lesson.groupId
            if (gid != null && !seenGroups.add(gid)) {
                wm.cancelUniqueWork(REMINDER_PREFIX + lesson.id)
                wm.cancelUniqueWork(REVIEW_PREFIX + lesson.id)
                return@forEach
            }
            val scheduled = lesson.status == LessonStatus.SCHEDULED
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull()
            val start = time?.let { LocalDateTime(lesson.date, it).toInstant(MSK) }
            val end = start?.plus(lesson.durationMinutes.minutes)

            plan(
                wm, REMINDER_PREFIX + lesson.id, KIND_REMINDER, lesson.id,
                trigger = start?.minus(LEAD_MINUTES.minutes),
                enabled = scheduled, now = now, horizon = horizon
            )
            plan(
                wm, REVIEW_PREFIX + lesson.id, KIND_REVIEW, lesson.id,
                trigger = end,
                enabled = scheduled, now = now, horizon = horizon
            )
        }
    }

    private fun plan(
        wm: WorkManager,
        workName: String,
        kind: String,
        lessonId: Long,
        trigger: Instant?,
        enabled: Boolean,
        now: Instant,
        horizon: Instant
    ) {
        if (!enabled || trigger == null || trigger <= now || trigger > horizon) {
            wm.cancelUniqueWork(workName)
            return
        }
        val request = OneTimeWorkRequestBuilder<LessonReminderWorker>()
            .setInitialDelay((trigger - now).inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(LESSON_ID_KEY to lessonId, KIND_KEY to kind))
            .build()
        wm.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }
}

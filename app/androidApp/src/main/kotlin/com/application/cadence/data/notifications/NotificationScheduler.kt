package com.application.cadence.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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

/**
 * Schedules a local notification ahead of each upcoming lesson via WorkManager.
 * WorkManager persists enqueued jobs across reboots, so no boot receiver is needed,
 * and the worker re-reads the lesson before showing anything, so stale reminders
 * (deleted / cancelled / rescheduled lessons) simply do nothing.
 */
object NotificationScheduler {

    const val CHANNEL_ID = "lesson_reminders"
    const val LEAD_MINUTES = 60
    private const val HORIZON_DAYS = 30
    private const val WORK_PREFIX = "lesson-reminder-"
    const val LESSON_ID_KEY = "lessonId"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания об уроках",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Напоминания незадолго до начала занятия"
        }
        manager.createNotificationChannel(channel)
    }

    /** Re-plans reminders for every scheduled lesson in the next [HORIZON_DAYS]. Idempotent. */
    fun sync(context: Context, lessons: List<Lesson>) {
        val wm = WorkManager.getInstance(context)
        val now = Clock.System.now()
        val horizon = now + HORIZON_DAYS.days

        lessons.forEach { lesson ->
            val workName = WORK_PREFIX + lesson.id
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull()
            val start = time?.let { LocalDateTime(lesson.date, it).toInstant(MSK) }
            val trigger = start?.minus(LEAD_MINUTES.minutes)

            val plannable = lesson.status == LessonStatus.SCHEDULED &&
                start != null && trigger != null &&
                trigger > now && start <= horizon

            if (!plannable) {
                wm.cancelUniqueWork(workName)
                return@forEach
            }

            val request = OneTimeWorkRequestBuilder<LessonReminderWorker>()
                .setInitialDelay((trigger!! - now).inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(LESSON_ID_KEY to lesson.id))
                .build()
            wm.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

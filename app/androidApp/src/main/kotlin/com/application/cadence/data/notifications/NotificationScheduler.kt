package com.application.cadence.data.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.application.cadence.R
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.MSK
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
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
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = Clock.System.now()
        val horizon = now + HORIZON_DAYS.days
        val seenGroups = mutableSetOf<Long>()

        lessons.forEach { lesson ->
            val gid = lesson.groupId
            if (gid != null && !seenGroups.add(gid)) {
                cancel(context, am, lesson.id, KIND_REMINDER)
                cancel(context, am, lesson.id, KIND_REVIEW)
                return@forEach
            }
            val scheduled = lesson.status == LessonStatus.SCHEDULED
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull()
            val start = time?.let { LocalDateTime(lesson.date, it).toInstant(MSK) }
            val end = start?.plus(lesson.durationMinutes.minutes)

            schedule(
                context, am, lesson.id, KIND_REMINDER,
                trigger = start?.minus(LEAD_MINUTES.minutes),
                enabled = scheduled, now = now, horizon = horizon
            )
            schedule(
                context, am, lesson.id, KIND_REVIEW,
                trigger = end,
                enabled = scheduled, now = now, horizon = horizon
            )
        }
    }

    private fun schedule(
        context: Context,
        am: AlarmManager,
        lessonId: Long,
        kind: String,
        trigger: Instant?,
        enabled: Boolean,
        now: Instant,
        horizon: Instant
    ) {
        val pi = pendingIntent(context, lessonId, kind)
        if (!enabled || trigger == null || trigger <= now || trigger > horizon) {
            am.cancel(pi)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.cancel(pi)
            return
        }
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toEpochMilliseconds(),
            pi
        )
    }

    private fun cancel(context: Context, am: AlarmManager, lessonId: Long, kind: String) {
        am.cancel(pendingIntent(context, lessonId, kind))
    }

    private fun pendingIntent(context: Context, lessonId: Long, kind: String): PendingIntent {
        val intent = Intent(context, LessonReminderReceiver::class.java).apply {
            action = "com.application.cadence.LESSON_ALARM"
            putExtra(LESSON_ID_KEY, lessonId)
            putExtra(KIND_KEY, kind)
        }
        val requestCode = ("$kind-$lessonId").hashCode()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

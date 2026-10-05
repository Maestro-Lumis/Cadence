package com.application.cadence.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.application.cadence.CadenceApplication
import com.application.cadence.MainActivity
import com.application.cadence.R
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class LessonReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lessonId = inputData.getLong(NotificationScheduler.LESSON_ID_KEY, -1L)
        if (lessonId < 0) return Result.success()
        val kind = inputData.getString(NotificationScheduler.KIND_KEY) ?: NotificationScheduler.KIND_REMINDER

        val app = applicationContext as CadenceApplication
        val lesson = app.lessonRepository.getById(lessonId) ?: return Result.success()
        if (lesson.status != LessonStatus.SCHEDULED) return Result.success()

        val student = app.studentRepository.observeById(lesson.studentId).first()
            ?: return Result.success()

        val ctx = applicationContext
        val title: String
        if (kind == NotificationScheduler.KIND_REVIEW) {
            title = ctx.getString(R.string.notif_review_title)
        } else {
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull()
                ?: return Result.success()
            val startLocal = LocalDateTime(lesson.date, time)
                .toInstant(MSK)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            title = ctx.getString(R.string.notif_reminder_title, startLocal.hour, startLocal.minute)
        }

        val text = if (lesson.groupId != null) ctx.getString(R.string.notif_group_text) else "${student.name} · ${student.course}"
        showNotification(
            tag = kind,
            id = lessonId.toInt(),
            title = title,
            text = text
        )
        return Result.success()
    }

    private fun showNotification(tag: String, id: Int, title: String, text: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (tag + id).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(tag, id, notification)
    }
}

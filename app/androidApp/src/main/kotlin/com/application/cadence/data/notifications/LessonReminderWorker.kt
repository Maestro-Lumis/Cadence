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

        val app = applicationContext as CadenceApplication
        val lesson = app.lessonRepository.getById(lessonId) ?: return Result.success()
        // The lesson may have been cancelled, held or rescheduled since we planned this.
        if (lesson.status != LessonStatus.SCHEDULED) return Result.success()

        val student = app.studentRepository.observeById(lesson.studentId).first()
            ?: return Result.success()

        val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return Result.success()
        val startLocal = LocalDateTime(lesson.date, time)
            .toInstant(MSK)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val timeStr = "%02d:%02d".format(startLocal.hour, startLocal.minute)

        showNotification(
            id = lessonId.toInt(),
            title = "Скоро урок в $timeStr",
            text = "${student.name} · ${student.course}"
        )
        return Result.success()
    }

    private fun showNotification(id: Int, title: String, text: String) {
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
            id,
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

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

package com.application.cadence.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.application.cadence.CadenceApplication
import com.application.cadence.MainActivity
import com.application.cadence.R
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.MSK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class LessonReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lessonId = intent.getLongExtra(NotificationScheduler.LESSON_ID_KEY, -1L)
        if (lessonId < 0) return
        val kind = intent.getStringExtra(NotificationScheduler.KIND_KEY)
            ?: NotificationScheduler.KIND_REMINDER

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deliver(context, lessonId, kind)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun deliver(context: Context, lessonId: Long, kind: String) {
        val app = context.applicationContext as CadenceApplication
        val lesson = app.lessonRepository.getById(lessonId) ?: return
        if (lesson.status != LessonStatus.SCHEDULED) return

        val student = app.studentRepository.observeById(lesson.studentId).first() ?: return

        val title: String
        if (kind == NotificationScheduler.KIND_REVIEW) {
            title = context.getString(R.string.notif_review_title)
        } else {
            val time = runCatching { LocalTime.parse(lesson.time) }.getOrNull() ?: return
            val startLocal = LocalDateTime(lesson.date, time)
                .toInstant(MSK)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            title = context.getString(R.string.notif_reminder_title, startLocal.hour, startLocal.minute)
        }

        val text = if (lesson.groupId != null) {
            context.getString(R.string.notif_group_text)
        } else {
            "${student.name} · ${student.course}"
        }

        showNotification(context, kind, lessonId.toInt(), title, text)
    }

    private fun showNotification(context: Context, tag: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

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

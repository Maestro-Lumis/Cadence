package com.application.cadence.presentation.common

import android.content.Context
import com.application.cadence.R
import com.application.cadence.core.Weekday
import kotlinx.datetime.TimeZone

val MSK: TimeZone = TimeZone.of("Europe/Moscow")

fun weekdayLabel(ctx: Context, day: Weekday): String =
    ctx.resources.getStringArray(R.array.weekdays_full)[day.ordinal]

fun weekdayShort(ctx: Context, day: Weekday): String =
    ctx.resources.getStringArray(R.array.weekdays_short)[day.ordinal]

fun monthNominative(ctx: Context, month: Int): String =
    ctx.resources.getStringArray(R.array.months_nominative)[(month - 1).coerceIn(0, 11)]

fun monthGenitive(ctx: Context, month: Int): String =
    ctx.resources.getStringArray(R.array.months_genitive)[(month - 1).coerceIn(0, 11)]

fun monthShort(ctx: Context, month: Int): String =
    ctx.resources.getStringArray(R.array.months_short)[(month - 1).coerceIn(0, 11)]

fun formatDuration(ctx: Context, minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> ctx.getString(R.string.duration_min, m)
        m == 0 -> ctx.getString(R.string.duration_hr, h)
        else -> ctx.getString(R.string.duration_hr_min, h, m)
    }
}

fun timezonePresets(ctx: Context): List<Pair<String, String>> = listOf(
    "Europe/Moscow" to ctx.getString(R.string.tz_moscow),
    "Asia/Yekaterinburg" to ctx.getString(R.string.tz_yekaterinburg),
    "Asia/Tbilisi" to ctx.getString(R.string.tz_tbilisi),
    "America/Montevideo" to ctx.getString(R.string.tz_montevideo),
    "Europe/Kaliningrad" to ctx.getString(R.string.tz_kaliningrad),
    "Asia/Novosibirsk" to ctx.getString(R.string.tz_novosibirsk),
)

fun timezoneLabel(ctx: Context, id: String): String =
    timezonePresets(ctx).firstOrNull { it.first == id }?.second ?: id

fun peopleWord(ctx: Context, n: Int): String =
    ctx.resources.getQuantityString(R.plurals.people, n)

fun lessonWord(ctx: Context, n: Int): String =
    ctx.resources.getQuantityString(R.plurals.lessons_count, n)

fun statusLabel(ctx: Context, status: com.application.cadence.core.LessonStatus): String = when (status) {
    com.application.cadence.core.LessonStatus.HELD -> ctx.getString(R.string.status_held)
    com.application.cadence.core.LessonStatus.CANCELLED -> ctx.getString(R.string.status_cancelled)
    com.application.cadence.core.LessonStatus.SCHEDULED -> ctx.getString(R.string.status_scheduled)
    com.application.cadence.core.LessonStatus.RESCHEDULED -> ctx.getString(R.string.status_rescheduled)
}

fun dateFull(ctx: Context, weekdayFull: String, dayOfMonth: Int, monthGen: String): String =
    ctx.getString(R.string.date_full, weekdayFull, dayOfMonth, monthGen)

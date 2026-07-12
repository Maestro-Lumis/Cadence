package com.application.cadence.presentation.common

import com.application.cadence.core.Weekday
import kotlinx.datetime.TimeZone

val MSK: TimeZone = TimeZone.of("Europe/Moscow")

fun weekdayLabel(day: Weekday): String = when (day) {
    Weekday.MON -> "Понедельник"
    Weekday.TUE -> "Вторник"
    Weekday.WED -> "Среда"
    Weekday.THU -> "Четверг"
    Weekday.FRI -> "Пятница"
    Weekday.SAT -> "Суббота"
    Weekday.SUN -> "Воскресенье"
}

fun weekdayShort(day: Weekday): String = when (day) {
    Weekday.MON -> "пн"
    Weekday.TUE -> "вт"
    Weekday.WED -> "ср"
    Weekday.THU -> "чт"
    Weekday.FRI -> "пт"
    Weekday.SAT -> "сб"
    Weekday.SUN -> "вс"
}

private val monthsNominative = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private val monthsGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)

fun monthNominative(month: Int): String = monthsNominative[(month - 1).coerceIn(0, 11)]

fun monthGenitive(month: Int): String = monthsGenitive[(month - 1).coerceIn(0, 11)]

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m мин"
        m == 0 -> "$h ч"
        else -> "$h ч $m мин"
    }
}

val TIMEZONE_PRESETS: List<Pair<String, String>> = listOf(
    "Europe/Moscow" to "Москва",
    "Asia/Yekaterinburg" to "Екатеринбург",
    "Asia/Tbilisi" to "Тбилиси",
    "America/Montevideo" to "Монтевидео",
    "Europe/Kaliningrad" to "Калининград",
    "Asia/Novosibirsk" to "Новосибирск",
)

fun timezoneLabel(id: String): String =
    TIMEZONE_PRESETS.firstOrNull { it.first == id }?.second ?: id

package com.application.cadence.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class DateProvider {

    private val _mskToday = MutableStateFlow(Clock.System.todayIn(MSK))
    val mskToday: StateFlow<LocalDate> = _mskToday

    private val _tutorToday = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val tutorToday: StateFlow<LocalDate> = _tutorToday

    fun refresh() {
        _mskToday.value = Clock.System.todayIn(MSK)
        _tutorToday.value = Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
}

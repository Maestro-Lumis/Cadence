package com.application.cadence.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Quick period presets that roll back from today (in MSK). */
enum class PeriodPreset(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    THREE_MONTHS("3 мес"),
    SIX_MONTHS("6 мес");

    /** Returns the [from, to] range for this preset ending on [today]. */
    fun range(today: LocalDate): Pair<LocalDate, LocalDate> {
        val from = when (this) {
            WEEK -> today.minus(DatePeriod(days = 7))
            MONTH -> today.minus(DatePeriod(months = 1))
            THREE_MONTHS -> today.minus(DatePeriod(months = 3))
            SIX_MONTHS -> today.minus(DatePeriod(months = 6))
        }
        return from to today
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodPresetChips(
    from: LocalDate,
    to: LocalDate,
    onSelect: (PeriodPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.todayIn(MSK)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PeriodPreset.entries.forEach { preset ->
            val selected = preset.range(today) == (from to to)
            FilterChip(
                selected = selected,
                onClick = { onSelect(preset) },
                label = { Text(preset.label) }
            )
        }
    }
}

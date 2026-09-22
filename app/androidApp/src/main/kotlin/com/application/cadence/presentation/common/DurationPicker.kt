package com.application.cadence.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val DURATION_PRESETS = listOf(30, 45, 60, 90, 120)

/**
 * Duration input combining quick preset chips with a manual field. Both edit the same
 * value: tapping a chip sets it, typing sets it, and the chip matching the current value
 * is highlighted. 0 means "empty" (nothing typed yet).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Длительность (мин)"
) {
    Column(modifier = modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DURATION_PRESETS.forEach { preset ->
                FilterChip(
                    selected = minutes == preset,
                    onClick = { onMinutesChange(preset) },
                    label = { Text(preset.toString()) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = if (minutes > 0) minutes.toString() else "",
            onValueChange = { text -> onMinutesChange(text.filter { it.isDigit() }.toIntOrNull() ?: 0) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.application.cadence.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.Schedule
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.formatDuration
import com.application.cadence.presentation.common.weekdayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    studentName: String,
    onBack: () -> Unit
) {
    val slots by viewModel.slots.collectAsState()

    var selectedDay by remember { mutableStateOf(Weekday.MON) }
    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timeText by remember { mutableStateOf("10:00") }
    var durationText by remember { mutableStateOf("60") }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0)

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    timeText = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                "← Назад",
                modifier = Modifier.clickable { onBack() },
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Text("Расписание", style = MaterialTheme.typography.titleLarge)
            Text(
                studentName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (slots.isEmpty()) {
                Text("Слотов пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    slots.forEach { slot ->
                        SlotRow(slot, onDelete = { viewModel.deleteSlot(slot.id) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Добавить слот", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = dayMenuExpanded,
                onExpandedChange = { dayMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = weekdayLabel(selectedDay),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("День недели") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dayMenuExpanded,
                    onDismissRequest = { dayMenuExpanded = false }
                ) {
                    Weekday.entries.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(weekdayLabel(day)) },
                            onClick = {
                                selectedDay = day
                                dayMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время (МСК)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Мин") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = {
                    val duration = durationText.toIntOrNull()
                    val timeValid = Regex("""^\d{1,2}:\d{2}$""").matches(timeText)
                    error = when {
                        !timeValid -> "Неверное время"
                        duration == null || duration <= 0 -> "Длительность больше 0"
                        else -> null
                    }
                    if (error == null && duration != null) {
                        viewModel.addSlot(selectedDay, timeText, duration) {}
                        resultText = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить слот")
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.generate { result ->
                        resultText = if (result.created == 0 && result.skipped == 0) {
                            "Нет слотов для генерации"
                        } else {
                            "Создано ${result.created}, пропущено ${result.skipped}"
                        }
                    }
                },
                enabled = slots.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Заполнить занятия на 4 недели")
            }
            resultText?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SlotRow(slot: Schedule, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(weekdayLabel(slot.dayOfWeek), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${slot.time} МСК · ${formatDuration(slot.durationMinutes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "✕",
            modifier = Modifier.clickable { onDelete() },
            color = Color(0xFF995A1D)
        )
    }
}

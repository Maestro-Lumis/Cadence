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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.application.cadence.R
import com.application.cadence.core.Schedule
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.formatDuration
import com.application.cadence.presentation.common.weekdayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val studentName by viewModel.studentName.collectAsState()
    val slots by viewModel.slots.collectAsState()

    var selectedDay by remember { mutableStateOf(Weekday.MON) }
    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timeText by remember { mutableStateOf("10:00") }
    var durationText by remember { mutableStateOf("60") }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    var editingSlot by remember { mutableStateOf<Schedule?>(null) }
    var slotToDelete by remember { mutableStateOf<Schedule?>(null) }
    var deleteLessonCount by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearLessonCount by remember { mutableIntStateOf(0) }

    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0)

    fun resetForm() {
        editingSlot = null
        selectedDay = Weekday.MON
        timeText = "10:00"
        durationText = "60"
        timePickerState.hour = 10
        timePickerState.minute = 0
        error = null
    }

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
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    slotToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            title = { Text(stringResource(R.string.schedule_delete_title)) },
            text = {
                Text(
                    if (deleteLessonCount > 0)
                        context.resources.getQuantityString(R.plurals.schedule_delete_body, deleteLessonCount, deleteLessonCount)
                    else
                        stringResource(R.string.schedule_delete_body_zero)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSlot(slot)
                    if (editingSlot?.id == slot.id) resetForm()
                    slotToDelete = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { slotToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.schedule_clear_title)) },
            text = {
                Text(
                    if (clearLessonCount > 0)
                        context.resources.getQuantityString(R.plurals.schedule_clear_body, clearLessonCount, clearLessonCount)
                    else
                        stringResource(R.string.schedule_clear_body_zero)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAll { onBack() }
                }) { Text(stringResource(R.string.schedule_clear_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
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
                stringResource(R.string.back),
                modifier = Modifier.clickable { onBack() },
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleLarge)
            Text(
                studentName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (slots.isEmpty()) {
                Text(stringResource(R.string.schedule_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    slots.forEach { slot ->
                        SlotRow(
                            slot = slot,
                            selected = editingSlot?.id == slot.id,
                            onClick = {
                                editingSlot = slot
                                selectedDay = slot.dayOfWeek
                                timeText = slot.time
                                durationText = slot.durationMinutes.toString()
                                error = null
                                resultText = null
                                slot.time.split(":").let { parts ->
                                    timePickerState.hour = parts.getOrNull(0)?.toIntOrNull() ?: timePickerState.hour
                                    timePickerState.minute = parts.getOrNull(1)?.toIntOrNull() ?: timePickerState.minute
                                }
                            },
                            onDelete = {
                                viewModel.countFutureLessons(slot) { count ->
                                    deleteLessonCount = count
                                    slotToDelete = slot
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.schedule_clear),
                    modifier = Modifier.clickable {
                        viewModel.countAllFutureLessons { count ->
                            clearLessonCount = count
                            showClearConfirm = true
                        }
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(if (editingSlot != null) R.string.schedule_edit_slot else R.string.schedule_add_slot),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = dayMenuExpanded,
                onExpandedChange = { dayMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = weekdayLabel(LocalContext.current, selectedDay),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.schedule_day_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dayMenuExpanded,
                    onDismissRequest = { dayMenuExpanded = false }
                ) {
                    Weekday.entries.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(weekdayLabel(LocalContext.current, day)) },
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
                        label = { Text(stringResource(R.string.schedule_time_label)) },
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
                    label = { Text(stringResource(R.string.schedule_min_label)) },
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
                    val duplicate = slots.any {
                        it.id != editingSlot?.id && it.dayOfWeek == selectedDay && it.time == timeText
                    }
                    error = when {
                        !timeValid -> context.getString(R.string.schedule_bad_time)
                        duration == null || duration <= 0 -> context.getString(R.string.schedule_bad_duration)
                        duplicate -> context.getString(R.string.schedule_duplicate)
                        else -> null
                    }
                    if (error == null && duration != null) {
                        val onResult: (GenerationResult) -> Unit = { result ->
                            resultText = context.getString(R.string.schedule_auto_result, result.created, result.skipped)
                        }
                        val editing = editingSlot
                        if (editing != null) {
                            viewModel.updateSlot(editing, selectedDay, timeText, duration, onResult)
                            resetForm()
                        } else {
                            viewModel.addSlot(selectedDay, timeText, duration, onResult)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (editingSlot != null) R.string.save else R.string.schedule_add_slot))
            }
            if (editingSlot != null) {
                TextButton(
                    onClick = { resetForm() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.cancel)) }
            }
            resultText?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SlotRow(slot: Schedule, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(weekdayLabel(context, slot.dayOfWeek), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.schedule_slot_msk, slot.time, formatDuration(context, slot.durationMinutes)),
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

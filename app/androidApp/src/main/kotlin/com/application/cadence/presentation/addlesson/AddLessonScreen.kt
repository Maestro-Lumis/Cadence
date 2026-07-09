package com.application.cadence.presentation.addlesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonScreen(
    viewModel: AddLessonViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialDate: String? = null
) {
    val students by viewModel.students.collectAsState()
    val suggestedNumber by viewModel.suggestedLessonNumber.collectAsState()

    val defaultDate = initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: Clock.System.todayIn(MSK)

    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf(defaultDate.toString()) }
    var timeText by remember { mutableStateOf("18:00") }
    var durationText by remember { mutableStateOf("60") }

    var status by remember { mutableStateOf(LessonStatus.SCHEDULED) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    var lessonNumberText by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = defaultDate
            .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    )
    val timePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateText = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            Text("← Назад", modifier = Modifier.clickable { onBack() }, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Text("Новое занятие", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (students.isEmpty()) {
                Text(
                    "Сначала добавьте хотя бы одного ученика",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ученик") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = studentMenuExpanded,
                        onDismissRequest = { studentMenuExpanded = false }
                    ) {
                        students.forEach { student ->
                            DropdownMenuItem(
                                text = { Text("${student.name} (${student.course})") },
                                onClick = {
                                    selectedStudent = student
                                    viewModel.selectStudent(student.id)
                                    studentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Дата (МСК)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
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
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Длительность (мин)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = statusMenuExpanded,
                    onExpandedChange = { statusMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = status.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Статус") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        LessonStatus.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label()) },
                                onClick = {
                                    status = option
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                LaunchedEffect(selectedStudent?.id, suggestedNumber) {
                    val id = selectedStudent?.id
                    val s = suggestedNumber
                    if (id != null && s != null) {
                        lessonNumberText = s.toString()
                    }
                }
                OutlinedTextField(
                    value = lessonNumberText,
                    onValueChange = { lessonNumberText = it },
                    label = { Text("Номер урока (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = paid, onCheckedChange = { paid = it })
                    Text("Оплачено")
                }
                Spacer(Modifier.height(8.dp))

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val student = selectedStudent
                        val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
                        val timeValid = Regex("""^\d{1,2}:\d{2}$""").matches(timeText)
                        val parsedDuration = durationText.toIntOrNull()

                        error = when {
                            student == null -> "Выбери ученика"
                            parsedDate == null -> "Неверная дата, формат ГГГГ-ММ-ДД"
                            !timeValid -> "Неверное время, формат ЧЧ:MM"
                            parsedDuration == null || parsedDuration <= 0 -> "Длительность в минутах, больше 0"
                            else -> null
                        }

                        if (error == null && student != null && parsedDate != null && parsedDuration != null) {
                            viewModel.save(
                                studentId = student.id,
                                date = parsedDate,
                                time = timeText,
                                durationMinutes = parsedDuration,
                                status = status,
                                lessonNumber = lessonNumberText.toIntOrNull(),
                                paid = paid,
                                onError = { msg -> error = msg },
                                onSaved = onSaved
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

private fun LessonStatus.label(): String = when (this) {
    LessonStatus.HELD -> "Проведён"
    LessonStatus.CANCELLED -> "Отменён"
    LessonStatus.SCHEDULED -> "Запланирован"
    LessonStatus.RESCHEDULED -> "Перенесён"
}

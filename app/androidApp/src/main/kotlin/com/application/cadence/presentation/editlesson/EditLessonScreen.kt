package com.application.cadence.presentation.editlesson

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.core.Student
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun EditLessonScreen(
    viewModel: EditLessonViewModel,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val initial by viewModel.initialLesson.collectAsState()

    val lesson = initial
    if (lesson == null) {
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
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    EditLessonForm(viewModel, lesson, students, onSaved, onDeleted, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLessonForm(
    viewModel: EditLessonViewModel,
    initial: Lesson,
    students: List<Student>,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit
) {
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(students) {
        if (selectedStudent == null) {
            selectedStudent = students.find { it.id == initial.studentId }
        }
    }

    var dateText by remember { mutableStateOf(initial.date.toString()) }
    var timeText by remember { mutableStateOf(initial.time) }
    var durationText by remember { mutableStateOf(initial.durationMinutes.toString()) }

    var status by remember { mutableStateOf(initial.status) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    var lessonNumberText by remember { mutableStateOf(initial.lessonNumber?.toString().orEmpty()) }
    var paid by remember { mutableStateOf(initial.paid) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    )
    val initialTime = runCatching { LocalTime.parse(initial.time) }.getOrNull()
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 18,
        initialMinute = initialTime?.minute ?: 0
    )

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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить занятие?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
                }) { Text("Удалить", color = Color(0xFFB71C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
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
                "← Назад",
                modifier = Modifier.clickable { onBack() },
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Text("Редактировать занятие", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

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
                        parsedDate == null -> "Неверная дата"
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
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалить занятие")
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

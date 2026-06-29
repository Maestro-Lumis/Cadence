package com.application.cadence.presentation.addlesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonScreen(viewModel: AddLessonViewModel, onSaved: () -> Unit, onBack: () -> Unit) {
    val students by viewModel.students.collectAsState()

    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
    var timeText by remember { mutableStateOf("18:00") }

    var status by remember { mutableStateOf(LessonStatus.SCHEDULED) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    var lessonNumberText by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                                    studentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Дата (ГГГГ-ММ-ДД)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Время") },
                        modifier = Modifier.weight(1f)
                    )
                }
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

                        error = when {
                            student == null -> "Выбери ученика"
                            parsedDate == null -> "Неверная дата, формат ГГГГ-ММ-ДД"
                            !timeValid -> "Неверное время, формат ЧЧ:MM"
                            else -> null
                        }

                        if (error == null && student != null && parsedDate != null) {
                            viewModel.save(
                                studentId = student.id,
                                date = parsedDate,
                                time = timeText,
                                status = status,
                                lessonNumber = lessonNumberText.toIntOrNull(),
                                paid = paid,
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
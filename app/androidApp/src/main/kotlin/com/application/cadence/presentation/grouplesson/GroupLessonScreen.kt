package com.application.cadence.presentation.grouplesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.ScreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupLessonScreen(
    viewModel: GroupLessonViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var addMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить занятие?") },
            text = { Text("Удалится у всех участников группы.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteGroup(onDeleted)
                }) { Text("Удалить", color = Color(0xFFB71C1C)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") } }
        )
    }

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("← Назад", modifier = Modifier.clickable { onBack() }, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Удалить",
                    modifier = Modifier.clickable { showDeleteConfirm = true },
                    color = Color(0xFFB71C1C),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(12.dp))

            val group = state
            if (group == null) {
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Групповое занятие", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${group.dateLabel} · ${group.timeLabel} · ${group.durationLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                if (group.addable.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = addMenuExpanded,
                        onExpandedChange = { addMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Добавить ученика") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = addMenuExpanded,
                            onDismissRequest = { addMenuExpanded = false }
                        ) {
                            group.addable.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.addStudent(id)
                                        addMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(group.participants, key = { it.lessonId }) { p ->
                        ParticipantCard(
                            participant = p,
                            onAttended = { viewModel.setAttendance(p.lessonId, it) },
                            onPaid = { viewModel.setPaid(p.lessonId, it) },
                            onRemove = { viewModel.removeStudent(p.lessonId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: GroupParticipantUi,
    onAttended: (Boolean) -> Unit,
    onPaid: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(participant.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    participant.course,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Убрать",
                modifier = Modifier.clickable { onRemove() },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB71C1C)
            )
        }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleChip("Был", participant.status == LessonStatus.HELD) { onAttended(true) }
            ToggleChip("Не пришёл", participant.status == LessonStatus.CANCELLED) { onAttended(false) }
            if (participant.status == LessonStatus.HELD) {
                ToggleChip(
                    if (participant.paid) "Оплачен" else "Оплатить",
                    participant.paid
                ) { onPaid(!participant.paid) }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

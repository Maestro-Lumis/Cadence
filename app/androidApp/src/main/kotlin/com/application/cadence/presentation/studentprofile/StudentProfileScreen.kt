package com.application.cadence.presentation.studentprofile

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.formatDuration
import com.application.cadence.presentation.common.timezoneLabel

@Composable
fun StudentProfileScreen(
    viewModel: StudentProfileViewModel,
    onBack: () -> Unit,
    onLessonClick: (Long) -> Unit,
    onScheduleClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить ученика?") },
            text = { Text("Будут удалены и все его занятия (${state?.totalLessons ?: 0}).") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "← Назад",
                    modifier = Modifier.clickable { onBack() },
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Изменить",
                        modifier = Modifier.clickable { onEditClick() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "Удалить",
                        modifier = Modifier.clickable { showDeleteConfirm = true },
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            val profile = state
            if (profile == null) {
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(profile.studentName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${profile.course} · ${timezoneLabel(profile.timezone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatBox("Всего", profile.totalLessons.toString(), Modifier.weight(1f))
                    StatBox("Проведено", profile.heldLessons.toString(), Modifier.weight(1f))
                    StatBox(
                        "Долг",
                        profile.unpaidLessons.toString(),
                        Modifier.weight(1f),
                        highlight = profile.unpaidLessons > 0
                    )
                }
                Spacer(Modifier.height(12.dp))

                Text(
                    "Расписание →",
                    modifier = Modifier.clickable { onScheduleClick(profile.studentName) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(12.dp))

                Text("История", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profile.history, key = { it.id }) { lesson ->
                        HistoryRow(lesson, onClick = { onLessonClick(lesson.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier = modifier
            .background(
                if (highlight) Color(0xFFFAEEDA) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HistoryRow(lesson: Lesson, onClick: () -> Unit) {
    val statusLabel = when (lesson.status) {
        LessonStatus.HELD -> "Проведён"
        LessonStatus.CANCELLED -> "Отменён"
        LessonStatus.SCHEDULED -> "Запланирован"
        LessonStatus.RESCHEDULED -> "Перенесён"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(lesson.date.toString())
            Text(
                "$statusLabel · ${formatDuration(lesson.durationMinutes)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (lesson.status == LessonStatus.HELD && !lesson.paid) {
            Text("Не оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF995A1D))
        }
    }
}

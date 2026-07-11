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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onReportClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить ученика?") },
            text = { Text("Будут удалены и все его занятия.") },
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
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        "Расписание →",
                        modifier = Modifier.clickable { onScheduleClick(profile.studentName) },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "Отчёт →",
                        modifier = Modifier.clickable { onReportClick() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TabChip(
                        label = "Проведено",
                        count = profile.heldLessons,
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabChip(
                        label = "Долг",
                        count = profile.unpaidLessons,
                        selected = selectedTab == 1,
                        highlight = profile.unpaidLessons > 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
                Spacer(Modifier.height(12.dp))

                val held = profile.history.filter { it.status == LessonStatus.HELD }
                val list = if (selectedTab == 0) held else held.filter { !it.paid }

                if (list.isEmpty()) {
                    Text(
                        if (selectedTab == 0) "Проведённых занятий нет" else "Долгов нет",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list, key = { it.id }) { lesson ->
                            LessonRow(lesson, onClick = { onLessonClick(lesson.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        selected && highlight -> Color(0xFFFAEEDA)
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        selected && highlight -> Color(0xFF995A1D)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = fg)
    }
}

@Composable
private fun LessonRow(lesson: Lesson, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(lesson.date.toString())
            Text(
                "Проведён · ${formatDuration(lesson.durationMinutes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (lesson.paid) {
            Text("Оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
        } else {
            Text("Не оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF995A1D))
        }
    }
}

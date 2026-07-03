package com.application.cadence.presentation.today

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.ScreenContainer

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onLessonClick: (Long) -> Unit,
    onAddStudentClick: () -> Unit,
    onAddLessonClick: () -> Unit,
    onAllStudentsClick: () -> Unit
) {
    val lessons by viewModel.uiState.collectAsState()

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Занятия", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Все ученики",
                    modifier = Modifier.clickable { onAllStudentsClick() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(16.dp))

            if (lessons.isEmpty()) {
                Text("Сегодня занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessons, key = { it.lessonId }) { lesson ->
                        LessonCard(lesson, onClick = { onLessonClick(lesson.lessonId) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onAddLessonClick, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить занятие")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddStudentClick, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить ученика")
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: TodayLessonUi, onClick: () -> Unit) {
    val statusLabel = when (lesson.status) {
        LessonStatus.HELD -> "Проведён"
        LessonStatus.CANCELLED -> "Отменён"
        LessonStatus.SCHEDULED -> "Запланирован"
        LessonStatus.RESCHEDULED -> "Перенесён"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(lesson.time, style = MaterialTheme.typography.titleLarge)
                Text(
                    "до ${lesson.endTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (lesson.status == LessonStatus.SCHEDULED && !lesson.paid) {
                Text(
                    "Не оплачен",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF995A1D)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(lesson.studentName, style = MaterialTheme.typography.titleMedium)
        Text(
            buildString {
                append(lesson.course)
                lesson.lessonNumber?.let { append(" · Урок $it") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        lesson.studentLocalTime?.let {
            Text(
                "у ученика $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
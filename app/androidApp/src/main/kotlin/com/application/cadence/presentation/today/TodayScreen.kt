package com.application.cadence.presentation.today

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.LessonStatus
import androidx.compose.runtime.getValue

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val lessons by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(text = "Занятия", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (lessons.isEmpty()) {
            Text("Сегодня занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lessons, key = { it.lessonId }) { lesson -> LessonCard(lesson) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { /* TODO: форма добавления */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить занятие")
        }
    }
}

@Composable
private fun LessonCard(lesson: TodayLessonUi) {
    val statusLabel = when (lesson.status) {
        LessonStatus.HELD -> "Проведён"
        LessonStatus.CANCELLED -> "Отменён"
        LessonStatus.SCHEDULED -> "Запланирован"
        LessonStatus.RESCHEDULED -> "Перенесён"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lesson.time, modifier = Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.studentName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    lesson.course,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            lesson.lessonNumber?.let { Text("Урок $it", style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(4.dp))
        Text(statusLabel, style = MaterialTheme.typography.labelSmall)
        if (lesson.status == LessonStatus.SCHEDULED && !lesson.paid) {
            Text("Не оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF995A1D))
        }
    }
}
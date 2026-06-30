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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.core.Lesson
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.ScreenContainer

@Composable
fun StudentProfileScreen(
    viewModel: StudentProfileViewModel,
    onBack: () -> Unit,
    onAddPackageClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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

            val profile = state
            if (profile == null) {
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(profile.studentName, style = MaterialTheme.typography.titleLarge)
                Text(
                    profile.course,
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
                Spacer(Modifier.height(16.dp))

                Text("Пакеты", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                if (profile.packages.isEmpty()) {
                    Text(
                        "Нет активных пакетов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.packages.forEach { pkg -> PackageRow(pkg) }
                    }
                }
                TextButton(onClick = onAddPackageClick) {
                    Text("+ Добавить пакет")
                }
                Spacer(Modifier.height(8.dp))

                Text("История", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profile.history, key = { it.lesson.id }) { item -> HistoryRow(item) }
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
private fun PackageRow(pkg: PackageUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Пакет на ${pkg.total} занятий")
            Text(
                "Использовано: ${pkg.used} из ${pkg.total}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pkg.paid) {
            Text("Оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
        } else {
            Text("Не оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF995A1D))
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItemUi) {
    val lesson = item.lesson
    val statusLabel = when (lesson.status) {
        LessonStatus.HELD -> "Проведён"
        LessonStatus.CANCELLED -> "Отменён"
        LessonStatus.SCHEDULED -> "Запланирован"
        LessonStatus.RESCHEDULED -> "Перенесён"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(lesson.date.toString())
            Text(statusLabel, style = MaterialTheme.typography.labelSmall)
        }
        if (item.unpaid) {
            Text("Не оплачен", style = MaterialTheme.typography.labelSmall, color = Color(0xFF995A1D))
        }
    }
}
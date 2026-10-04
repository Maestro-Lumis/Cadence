package com.application.cadence.presentation.students

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.timezoneLabel

@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel,
    onStudentClick: (Long) -> Unit,
    onAddStudentClick: () -> Unit
) {
    val students by viewModel.uiState.collectAsState()
    val lost by viewModel.lostStudents.collectAsState()

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("Ученики", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (lost.isNotEmpty()) {
                Text("Пропали", style = MaterialTheme.typography.titleSmall, color = Color(0xFF995A1D))
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lost.forEach { student ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onStudentClick(student.id) }
                                .background(Color(0xFFFAEEDA))
                                .padding(12.dp)
                        ) {
                            Text(student.name, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF995A1D))
                            Text(
                                student.detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF995A1D)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (students.isEmpty()) {
                Text("Учеников пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentRow(student, onClick = { onStudentClick(student.id) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onAddStudentClick, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить ученика")
            }
        }
    }
}

@Composable
private fun StudentRow(student: StudentRowUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(student.name, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${student.course} · ${timezoneLabel(student.timezone)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


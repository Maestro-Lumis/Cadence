package com.application.cadence.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.cadence.core.Weekday
import com.application.cadence.presentation.common.AgendaLessonCard
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.weekdayShort
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onLessonClick: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onAddLessonClick: (LocalDate) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var listMode by rememberSaveable { mutableStateOf(false) }

    fun openCard(lessonId: Long, groupId: Long?) {
        if (groupId != null) onGroupClick(groupId) else onLessonClick(lessonId)
    }

    ScreenContainer {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    Text(state.monthTitle, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MonthArrow("‹") { viewModel.shiftMonth(-1) }
                        MonthArrow("›") { viewModel.shiftMonth(1) }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeChip("Месяц", !listMode, Modifier.weight(1f)) { listMode = false }
                    ModeChip("Список", listMode, Modifier.weight(1f)) { listMode = true }
                }
                Spacer(Modifier.height(12.dp))

                if (!listMode) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (0..6).forEach { i ->
                            Text(
                                weekdayShort(Weekday.entries[i]),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    state.weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                MonthDayCell(day, Modifier.weight(1f)) { viewModel.selectDate(day.date) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(
                        state.selectedLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    if (state.lessons.isEmpty()) {
                        Text("В этот день занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.lessons, key = { it.groupId ?: it.lessonId }) { lesson ->
                                AgendaLessonCard(lesson, onClick = { openCard(lesson.lessonId, lesson.groupId) })
                            }
                        }
                    }
                } else {
                    if (state.agenda.isEmpty()) {
                        Text("В этом месяце занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            state.agenda.forEach { day ->
                                item(key = "h-${day.label}") {
                                    Text(
                                        day.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                items(day.lessons, key = { it.groupId ?: it.lessonId }) { lesson ->
                                    AgendaLessonCard(lesson, onClick = { openCard(lesson.lessonId, lesson.groupId) })
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { onAddLessonClick(selectedDate) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        label,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(vertical = 8.dp),
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun MonthArrow(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MonthDayCell(day: CalDayUi, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (day.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val numberColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isToday -> MaterialTheme.colorScheme.primary
        !day.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day.dayNumber.toString(), style = MaterialTheme.typography.bodyMedium, color = numberColor)
        Row(
            modifier = Modifier.height(8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(minOf(day.lessonCount, 3)) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

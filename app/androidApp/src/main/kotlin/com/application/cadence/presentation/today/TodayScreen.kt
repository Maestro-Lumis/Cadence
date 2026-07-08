package com.application.cadence.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onLessonClick: (Long) -> Unit,
    onAddLessonClick: () -> Unit
) {
    val day by viewModel.dayState.collectAsState()
    val reviewQueue by viewModel.reviewQueue.collectAsState()

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
                Text(day.monthTitle, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        "‹",
                        modifier = Modifier.clickable { viewModel.shiftWeek(-1) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "›",
                        modifier = Modifier.clickable { viewModel.shiftWeek(1) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            WeekStrip(
                week = day.week,
                onSelectDate = viewModel::selectDate,
                onPrevWeek = { viewModel.shiftWeek(-1) },
                onNextWeek = { viewModel.shiftWeek(1) }
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (reviewQueue.isNotEmpty()) {
                    item(key = "review-header") {
                        Text("Требует внимания", style = MaterialTheme.typography.titleMedium)
                    }
                    items(reviewQueue, key = { "review-${it.lessonId}" }) { review ->
                        ReviewCard(
                            review = review,
                            onHeldPaid = { viewModel.resolve(review.lessonId, LessonStatus.HELD, true) },
                            onHeldUnpaid = { viewModel.resolve(review.lessonId, LessonStatus.HELD, false) },
                            onCancelled = { viewModel.resolve(review.lessonId, LessonStatus.CANCELLED, false) },
                            onReschedule = { onLessonClick(review.lessonId) }
                        )
                    }
                    item(key = "review-gap") { Spacer(Modifier.height(8.dp)) }
                }

                item(key = "day-label") {
                    Text(
                        day.selectedLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (day.lessons.isEmpty()) {
                    item(key = "empty") {
                        Text("В этот день занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(day.lessons, key = { it.lessonId }) { lesson ->
                        LessonCard(lesson, onClick = { onLessonClick(lesson.lessonId) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onAddLessonClick, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить занятие")
            }
        }
    }
}

@Composable
private fun WeekStrip(
    week: List<WeekDayUi>,
    onSelectDate: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (total > 60f) onPrevWeek() else if (total < -60f) onNextWeek()
                        total = 0f
                    },
                    onHorizontalDrag = { _, delta -> total += delta }
                )
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        week.forEach { day ->
            DayCell(day, Modifier.weight(1f), onClick = { onSelectDate(day.date) })
        }
    }
}

@Composable
private fun DayCell(day: WeekDayUi, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (day.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val labelColor =
        if (day.isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val numberColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day.shortLabel, style = MaterialTheme.typography.labelSmall, color = labelColor)
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

@Composable
private fun ReviewCard(
    review: ReviewLessonUi,
    onHeldPaid: () -> Unit,
    onHeldUnpaid: () -> Unit,
    onCancelled: () -> Unit,
    onReschedule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("Что с этим уроком?", style = MaterialTheme.typography.bodyMedium)
        Text(
            "${review.whenLabel} · ${review.studentName} · ${review.course}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        ActionChip("Проведён и оплачен", Color(0xFF2E7D32), Color(0xFFE6F3E9), Modifier.fillMaxWidth(), onHeldPaid)
        Spacer(Modifier.height(6.dp))
        ActionChip("Проведён, не оплачен", Color(0xFF995A1D), Color(0xFFFAEEDA), Modifier.fillMaxWidth(), onHeldUnpaid)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionChip(
                "Отменён",
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.surface,
                Modifier.weight(1f),
                onCancelled
            )
            ActionChip(
                "Перенесён",
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.surface,
                Modifier.weight(1f),
                onReschedule
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        label,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LessonCard(lesson: TodayLessonUi, onClick: () -> Unit) {
    val statusColor = when {
        lesson.status == LessonStatus.SCHEDULED -> Color(0xFF3B82F6)
        lesson.status == LessonStatus.HELD && !lesson.paid -> Color(0xFFE0A400)
        lesson.status == LessonStatus.HELD && lesson.paid -> Color(0xFF2E7D32)
        else -> Color(0xFF9E9E9E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp)
        ) {
            Text(
                "${lesson.time} – ${lesson.endTime}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(lesson.studentName, style = MaterialTheme.typography.bodyMedium)
            Text(
                buildString {
                    append(lesson.course)
                    lesson.lessonNumber?.let { append(" · Урок $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lesson.mskTime?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxHeight()
                .background(statusColor)
        )
    }
}

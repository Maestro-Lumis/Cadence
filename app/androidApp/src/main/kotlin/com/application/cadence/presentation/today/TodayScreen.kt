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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.cadence.R
import com.application.cadence.core.LessonStatus
import com.application.cadence.presentation.common.AgendaLessonCard
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onLessonClick: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onAddLessonClick: (LocalDate) -> Unit
) {
    val day by viewModel.dayState.collectAsState()
    val reviewQueue by viewModel.reviewQueue.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

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
                Text(day.monthTitle, style = MaterialTheme.typography.titleLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val onToday = day.week.any { it.isSelected && it.isToday }
                    if (!onToday) {
                        Text(
                            stringResource(R.string.today_jump),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.goToToday() }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    WeekArrow("‹") { viewModel.shiftWeek(-1) }
                    WeekArrow("›") { viewModel.shiftWeek(1) }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (reviewQueue.isNotEmpty()) {
                    item(key = "review-header") {
                        Text(stringResource(R.string.today_review_header), style = MaterialTheme.typography.titleMedium)
                    }
                    items(reviewQueue, key = { "review-${it.lessonId}" }) { review ->
                        ReviewCard(
                            review = review,
                            onHeldPaid = { viewModel.resolve(review.lessonId, LessonStatus.HELD, true) },
                            onHeldUnpaid = { viewModel.resolve(review.lessonId, LessonStatus.HELD, false) },
                            onCancelled = { viewModel.resolve(review.lessonId, LessonStatus.CANCELLED, false) },
                            onReschedule = { onLessonClick(review.lessonId) },
                            onGroupHeld = { viewModel.resolveAll(review.lessonIds, LessonStatus.HELD) },
                            onGroupCancelled = { viewModel.resolveAll(review.lessonIds, LessonStatus.CANCELLED) },
                            onOpenGroup = { review.groupId?.let { onGroupClick(it) } }
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
                        Text(stringResource(R.string.today_no_lessons), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(day.lessons, key = { it.groupId ?: it.lessonId }) { lesson ->
                        AgendaLessonCard(
                            lesson,
                            onClick = {
                                val g = lesson.groupId
                                if (g != null) onGroupClick(g) else onLessonClick(lesson.lessonId)
                            }
                        )
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
private fun WeekArrow(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
    onReschedule: () -> Unit,
    onGroupHeld: () -> Unit,
    onGroupCancelled: () -> Unit,
    onOpenGroup: () -> Unit
) {
    val isGroup = review.groupId != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            if (isGroup) stringResource(R.string.today_group_question) else stringResource(R.string.today_lesson_question),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "${review.whenLabel} · ${review.studentName} · ${review.course}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (isGroup) {
            ActionChip(stringResource(R.string.today_held_all), Color(0xFF2E7D32), Color(0xFFE6F3E9), Modifier.fillMaxWidth(), onGroupHeld)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip(
                    stringResource(R.string.today_cancel_all),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surface,
                    Modifier.weight(1f),
                    onGroupCancelled
                )
                ActionChip(
                    stringResource(R.string.today_mark_each),
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.surface,
                    Modifier.weight(1f),
                    onOpenGroup
                )
            }
        } else {
            ActionChip(stringResource(R.string.today_held_paid), Color(0xFF2E7D32), Color(0xFFE6F3E9), Modifier.fillMaxWidth(), onHeldPaid)
            Spacer(Modifier.height(6.dp))
            ActionChip(stringResource(R.string.today_held_unpaid), Color(0xFF995A1D), Color(0xFFFAEEDA), Modifier.fillMaxWidth(), onHeldUnpaid)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip(
                    stringResource(R.string.status_cancelled),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surface,
                    Modifier.weight(1f),
                    onCancelled
                )
                ActionChip(
                    stringResource(R.string.status_rescheduled),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surface,
                    Modifier.weight(1f),
                    onReschedule
                )
            }
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


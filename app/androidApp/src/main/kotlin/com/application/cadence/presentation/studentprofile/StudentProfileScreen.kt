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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.application.cadence.R
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
    val ctx = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.sprofile_delete_title)) },
            text = { Text(stringResource(R.string.sprofile_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.delete), color = Color(0xFFB71C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
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
                    stringResource(R.string.back),
                    modifier = Modifier.clickable { onBack() },
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.edit),
                        modifier = Modifier.clickable { onEditClick() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(R.string.delete),
                        modifier = Modifier.clickable { showDeleteConfirm = true },
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            val profile = state
            if (profile == null) {
                Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(profile.studentName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${profile.course} · ${timezoneLabel(ctx, profile.timezone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        stringResource(R.string.sprofile_schedule),
                        modifier = Modifier.clickable { onScheduleClick(profile.studentName) },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(R.string.sprofile_report),
                        modifier = Modifier.clickable { onReportClick() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TabChip(
                        label = stringResource(R.string.sprofile_tab_held),
                        count = profile.heldLessons,
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabChip(
                        label = stringResource(R.string.sprofile_tab_debt),
                        count = profile.unpaidLessons,
                        selected = selectedTab == 1,
                        highlight = profile.unpaidLessons > 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (selectedTab == 1 && profile.unpaidLessons > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.sprofile_unpaid, profile.unpaidTotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.sprofile_pay_all),
                            modifier = Modifier.clickable { viewModel.markAllPaid() },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                val held = profile.history.filter { it.status == LessonStatus.HELD }
                val list = if (selectedTab == 0) held else held.filter { !it.paid }

                if (list.isEmpty()) {
                    Text(
                        if (selectedTab == 0) stringResource(R.string.sprofile_no_held) else stringResource(R.string.sprofile_no_debts),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list, key = { it.id }) { lesson ->
                            LessonRow(
                                lesson,
                                onClick = { onLessonClick(lesson.id) },
                                onPay = { viewModel.markPaid(lesson.id) }
                            )
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
private fun LessonRow(lesson: Lesson, onClick: () -> Unit, onPay: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column {
            Text(lesson.date.toString())
            Text(
                buildString {
                    append(stringResource(R.string.sprofile_held_prefix))
                    append(formatDuration(ctx, lesson.durationMinutes))
                    if (lesson.groupId != null) append(stringResource(R.string.sprofile_group_tag))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (lesson.paid) {
            Text(stringResource(R.string.sprofile_paid), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
        } else {
            Text(
                stringResource(R.string.paid_label),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPay)
                    .background(Color(0xFFE6F3E9))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

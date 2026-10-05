package com.application.cadence.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.application.cadence.R
import com.application.cadence.core.LessonStatus

@Composable
fun AgendaLessonCard(card: AgendaCardUi, onClick: () -> Unit) {
    val statusColor = when {
        card.status == LessonStatus.SCHEDULED -> Color(0xFF3B82F6)
        card.status == LessonStatus.HELD && !card.paid -> Color(0xFFE0A400)
        card.status == LessonStatus.HELD && card.paid -> Color(0xFF2E7D32)
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
                "${card.time} – ${card.endTime} · ${card.durationLabel}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(card.studentName, style = MaterialTheme.typography.bodyMedium)
            val lessonLabel = card.lessonNumber?.let { stringResource(R.string.lesson_prefix, it) }
            Text(
                buildString {
                    append(card.course)
                    lessonLabel?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            card.mskTime?.let {
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

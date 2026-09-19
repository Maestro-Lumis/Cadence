package com.application.cadence.presentation.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel,
    onBack: () -> Unit,
    onStudentClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val from by viewModel.from.collectAsState()
    val to by viewModel.to.collectAsState()

    var picker by remember { mutableStateOf<String?>(null) }

    if (picker != null) {
        val current = if (picker == "from") from else to
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { picker = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        if (picker == "from") viewModel.setFrom(date) else viewModel.setTo(date)
                    }
                    picker = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picker = null }) { Text("Отмена") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

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

            Text("Заработок", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                PeriodField("С", from.toString(), Modifier.weight(1f)) { picker = "from" }
                Spacer(Modifier.width(8.dp))
                PeriodField("По", to.toString(), Modifier.weight(1f)) { picker = "to" }
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TotalBox("Заработано", "${state.totalEarned} ₽", Modifier.weight(1f))
                TotalBox("Оплачено", "${state.totalPaid} ₽", Modifier.weight(1f), Color(0xFF2E7D32))
                TotalBox("Долг", "${state.totalDebt} ₽", Modifier.weight(1f), Color(0xFF995A1D))
            }
            Spacer(Modifier.height(16.dp))

            if (state.rows.isEmpty()) {
                Text("За период занятий нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.rows, key = { it.studentId }) { row ->
                        EarningsRow(row, onClick = { onStudentClick(row.studentId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EarningsRow(row: EarningsRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(row.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${row.heldCount} зан. · ${row.earned} ₽" + if (row.debt > 0) " · долг ${row.debt} ₽" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (row.debt > 0) Color(0xFF995A1D) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

package com.application.cadence.presentation.report

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val report by viewModel.uiState.collectAsState()
    val from by viewModel.from.collectAsState()
    val to by viewModel.to.collectAsState()
    val context = LocalContext.current

    var picker by remember { mutableStateOf<String?>(null) }

    if (picker != null) {
        val current = if (picker == "from") from else to
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { picker = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        if (picker == "from") viewModel.setFrom(date) else viewModel.setTo(date)
                    }
                    picker = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picker = null }) { Text("Отмена") } }
        ) {
            DatePicker(state = state)
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

            Text("Отчёт", style = MaterialTheme.typography.titleLarge)
            report?.let {
                Text(
                    "${it.studentName} · ${it.course}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                PeriodField("С", from.toString(), Modifier.weight(1f)) { picker = "from" }
                Spacer(Modifier.width(8.dp))
                PeriodField("По", to.toString(), Modifier.weight(1f)) { picker = "to" }
            }
            Spacer(Modifier.height(16.dp))

            val data = report
            if (data != null) {
                Text(
                    "Проведено: ${data.totalCount} · Всего: ${data.totalLabel}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(data.rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.dateLabel, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                row.durationLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { openReportPdf(context, data) },
                        modifier = Modifier.weight(1f),
                        enabled = data.rows.isNotEmpty()
                    ) {
                        Text("Открыть")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { shareReportPdf(context, data) },
                        modifier = Modifier.weight(1f),
                        enabled = data.rows.isNotEmpty()
                    ) {
                        Text("Поделиться")
                    }
                }
            } else {
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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

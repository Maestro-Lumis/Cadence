package com.application.cadence.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.cadence.R
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun monthBounds(year: Int, month: Int): Pair<LocalDate, LocalDate> {
    val first = LocalDate(year, month, 1)
    val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    return first to last
}

fun previousMonthOf(ref: LocalDate): Pair<Int, Int> {
    val lastPrev = LocalDate(ref.year, ref.monthNumber, 1).minus(1, DateTimeUnit.DAY)
    return lastPrev.year to lastPrev.monthNumber
}

private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val d = LocalDate(year, month, 1).plus(delta, DateTimeUnit.MONTH)
    return d.year to d.monthNumber
}

@Composable
fun MonthPeriodSelector(
    from: LocalDate,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val year = from.year
    val month = from.monthNumber
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Arrow("‹") {
            val (y, m) = shiftMonth(year, month, -1)
            onMonthSelected(y, m)
        }
        Text(
            "${monthNominative(ctx, month)} $year",
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPicker = true }
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Arrow("›") {
            val (y, m) = shiftMonth(year, month, 1)
            onMonthSelected(y, m)
        }
    }

    if (showPicker) {
        MonthYearPickerDialog(
            year = year,
            month = month,
            onDismiss = { showPicker = false },
            onPick = { y, m ->
                onMonthSelected(y, m)
                showPicker = false
            }
        )
    }
}

@Composable
private fun Arrow(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MonthYearPickerDialog(
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit
) {
    val ctx = LocalContext.current
    var pickedYear by remember { mutableIntStateOf(year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Arrow("‹") { pickedYear-- }
                Text("$pickedYear", style = MaterialTheme.typography.titleMedium)
                Arrow("›") { pickedYear++ }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..2).forEach { colIndex ->
                            val m = rowIndex * 3 + colIndex + 1
                            val selected = pickedYear == year && m == month
                            Text(
                                monthShort(ctx, m),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onPick(pickedYear, m) }
                                    .padding(vertical = 10.dp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    )
}

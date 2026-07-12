package com.application.cadence.presentation.debts

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.application.cadence.presentation.common.ScreenContainer

@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onBack: () -> Unit,
    onStudentClick: (Long) -> Unit
) {
    val debts by viewModel.uiState.collectAsState()
    var payConfirm by remember { mutableStateOf<DebtRowUi?>(null) }

    payConfirm?.let { debt ->
        AlertDialog(
            onDismissRequest = { payConfirm = null },
            title = { Text("Отметить оплаченными?") },
            text = { Text("${debt.unpaidCount} ${lessonWord(debt.unpaidCount)} у ${debt.name} станут оплаченными.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markPaid(debt.studentId)
                    payConfirm = null
                }) { Text("Оплачено", color = Color(0xFF2E7D32)) }
            },
            dismissButton = {
                TextButton(onClick = { payConfirm = null }) { Text("Отмена") }
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
            Text(
                "← Назад",
                modifier = Modifier.clickable { onBack() },
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Text("Долги", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (debts.isEmpty()) {
                Text("Долгов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(debts, key = { it.studentId }) { debt ->
                        DebtRow(
                            debt = debt,
                            onOpen = { onStudentClick(debt.studentId) },
                            onPay = { payConfirm = debt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtRow(debt: DebtRowUi, onOpen: () -> Unit, onPay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAEEDA), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.clickable { onOpen() }) {
            Text(debt.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${debt.unpaidCount} ${lessonWord(debt.unpaidCount)} · с ${debt.oldestDate}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF995A1D)
            )
        }
        Text(
            "Оплачено",
            modifier = Modifier
                .background(Color(0xFFE6F3E9), RoundedCornerShape(8.dp))
                .clickable { onPay() }
                .padding(vertical = 8.dp, horizontal = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF2E7D32)
        )
    }
}

private fun lessonWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "урок"
        mod10 in 2..4 && mod100 !in 12..14 -> "урока"
        else -> "уроков"
    }
}

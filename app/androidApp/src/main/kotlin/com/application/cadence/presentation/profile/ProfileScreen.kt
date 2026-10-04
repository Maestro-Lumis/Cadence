package com.application.cadence.presentation.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.peopleWord

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onDebts: () -> Unit,
    onEarnings: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("Профиль", style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.studentCount} ${peopleWord(state.studentCount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            HubRow(
                emoji = "💰",
                title = "Долги",
                subtitle = if (state.debtTotal > 0)
                    "${state.debtorCount} ${peopleWord(state.debtorCount)} · ${state.debtTotal} ₽"
                else "нет долгов",
                onClick = onDebts
            )
            Spacer(Modifier.height(8.dp))
            HubRow(
                emoji = "📊",
                title = "Заработок",
                subtitle = "${state.earningsMonthLabel} · ${state.earningsMonthTotal} ₽",
                onClick = onEarnings
            )
            Spacer(Modifier.height(8.dp))
            HubRow(
                emoji = "💾",
                title = "Управление данными",
                subtitle = "бэкап, экспорт и импорт",
                onClick = onBackup
            )
            Spacer(Modifier.height(8.dp))
            HubRow(
                emoji = "⚙️",
                title = "Настройки",
                subtitle = "что нового",
                onClick = onSettings
            )

            Spacer(Modifier.weight(1f))
            Text(
                "Cadence · $versionName",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HubRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(emoji, fontSize = 22.sp)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

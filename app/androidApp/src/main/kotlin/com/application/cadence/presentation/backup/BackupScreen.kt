package com.application.cadence.presentation.backup

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.application.cadence.data.backup.BackupManager
import com.application.cadence.presentation.common.MSK
import com.application.cadence.presentation.common.ScreenContainer
import kotlinx.coroutines.launch
import kotlinx.datetime.todayIn
import java.io.File
import kotlin.time.Clock

@Composable
fun BackupScreen(backupManager: BackupManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
            } else {
                pendingImport = text
            }
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Импортировать копию?") },
            text = { Text("Все текущие данные будут заменены данными из файла. Отменить это будет нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = runCatching { backupManager.importJson(text) }.isSuccess
                        Toast.makeText(
                            context,
                            if (ok) "Данные восстановлены" else "Файл повреждён или не подходит",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text("Заменить", color = Color(0xFFB71C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Отмена") }
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

            Text("Резервная копия", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Экспорт сохранит всех учеников, занятия и расписания в файл — отправь его себе (в облако или мессенджер). Импорт восстановит данные из такого файла, заменив текущие.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        val json = backupManager.exportJson()
                        shareBackup(context, json)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспорт")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Импорт")
            }
        }
    }
}

private fun shareBackup(context: android.content.Context, json: String) {
    val date = Clock.System.todayIn(MSK)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "cadence_backup_$date.json")
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Резервная копия"))
}

package com.application.cadence.presentation.whatsnew

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ReleaseNote(
    val versionCode: Long,
    val title: String,
    val changes: List<String>
)

/** Newest first. Add an entry (with a bumped versionCode) for each release. */
val RELEASE_NOTES: List<ReleaseNote> = listOf(
    ReleaseNote(
        versionCode = 2,
        title = "Версия 1.1",
        changes = listOf(
            "Групповые занятия: одно занятие на несколько учеников",
            "Вкладка «Календарь»: месяц и список",
            "Отчёт и заработок теперь по месяцам",
            "Напоминания до урока и вопрос «как прошёл» после",
            "Своя длительность занятия у каждого ученика",
            "Новая иконка приложения"
        )
    )
)

private const val PREFS = "cadence_prefs"
private const val KEY_SEEN_VERSION = "whats_new_seen_version"

/** Shows the "what's new" dialog once after the app is updated to a newer version. */
@Composable
fun WhatsNewGate() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(emptyList<ReleaseNote>()) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = currentVersionCode(context)
        val lastSeen = prefs.getLong(KEY_SEEN_VERSION, 0L)
        if (current > lastSeen) {
            notes = RELEASE_NOTES.filter { it.versionCode > lastSeen }.sortedByDescending { it.versionCode }
            prefs.edit().putLong(KEY_SEEN_VERSION, current).apply()
        }
    }

    if (notes.isNotEmpty()) {
        WhatsNewDialog(notes) { notes = emptyList() }
    }
}

@Composable
fun WhatsNewDialog(notes: List<ReleaseNote>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Понятно") } },
        title = { Text("Что нового") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notes.forEach { note ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall)
                        note.changes.forEach { change ->
                            Text("•  $change", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    )
}

private fun currentVersionCode(context: Context): Long =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    }.getOrDefault(0L)

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.application.cadence.R

data class ReleaseNote(
    val versionCode: Long,
    val titleRes: Int,
    val changeRes: List<Int>
)

val RELEASE_NOTES: List<ReleaseNote> = listOf(
    ReleaseNote(
        versionCode = 4,
        titleRes = R.string.whatsnew_v130_title,
        changeRes = listOf(R.string.whatsnew_v130_1, R.string.whatsnew_v130_2)
    ),
    ReleaseNote(
        versionCode = 3,
        titleRes = R.string.whatsnew_v120_title,
        changeRes = listOf(R.string.whatsnew_v120_1, R.string.whatsnew_v120_2)
    ),
    ReleaseNote(
        versionCode = 2,
        titleRes = R.string.whatsnew_v110_title,
        changeRes = listOf(
            R.string.whatsnew_v110_1, R.string.whatsnew_v110_2,
            R.string.whatsnew_v110_3, R.string.whatsnew_v110_4,
            R.string.whatsnew_v110_5, R.string.whatsnew_v110_6
        )
    )
)

private const val PREFS = "cadence_prefs"
private const val KEY_SEEN_VERSION = "whats_new_seen_version"

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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        title = { Text(stringResource(R.string.whatsnew_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notes.forEach { note ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(note.titleRes), style = MaterialTheme.typography.titleSmall)
                        note.changeRes.forEach { res ->
                            Text("•  ${stringResource(res)}", style = MaterialTheme.typography.bodyMedium)
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

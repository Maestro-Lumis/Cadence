package com.application.cadence.presentation.settings

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.cadence.MainActivity
import com.application.cadence.R
import com.application.cadence.presentation.common.LocaleHelper
import kotlin.system.exitProcess
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.whatsnew.RELEASE_NOTES
import com.application.cadence.presentation.whatsnew.WhatsNewDialog

private val LANGUAGE_LABELS = mapOf(
    "ru" to "Русский",
    "en" to "English",
    "es" to "Español"
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val currentLang = remember { LocaleHelper.getLanguage(context) }

    if (showWhatsNew) {
        WhatsNewDialog(RELEASE_NOTES.sortedByDescending { it.versionCode }) { showWhatsNew = false }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.settings_language)) },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(stringResource(R.string.cancel)) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LocaleHelper.SUPPORTED_LANGUAGES.forEach { lang ->
                        val selected = lang == currentLang
                        Text(
                            LANGUAGE_LABELS[lang] ?: lang,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    showLanguagePicker = false
                                    if (lang != currentLang) {
                                        LocaleHelper.setLanguage(context, lang)
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        context.startActivity(intent)
                                        exitProcess(0)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
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
            Text(stringResource(R.string.back), modifier = Modifier.clickable { onBack() }, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            SettingsRow("🌐", stringResource(R.string.settings_language), LANGUAGE_LABELS[currentLang] ?: currentLang) { showLanguagePicker = true }
            Spacer(Modifier.height(8.dp))
            SettingsRow("🆕", stringResource(R.string.settings_whats_new), stringResource(R.string.settings_whats_new_subtitle)) { showWhatsNew = true }
        }
    }
}

@Composable
private fun SettingsRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

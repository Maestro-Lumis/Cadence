package com.application.cadence.presentation.addstudent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.application.cadence.R
import com.application.cadence.presentation.common.DurationPicker
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.timezoneLabel
import com.application.cadence.presentation.common.timezonePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(viewModel: AddStudentViewModel, onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val presets = remember(context) { timezonePresets(context) }
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf(presets.first().first) }
    var timezoneMenuExpanded by remember { mutableStateOf(false) }
    var rateText by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(60) }

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.back), modifier = Modifier.clickable { onBack() }, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.student_new_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.student_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text(stringResource(R.string.student_course_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = timezoneMenuExpanded,
                onExpandedChange = { timezoneMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = timezoneLabel(context, timezone),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.student_tz_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timezoneMenuExpanded,
                    onDismissRequest = { timezoneMenuExpanded = false }
                ) {
                    presets.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                timezone = id
                                timezoneMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.student_rate_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.lesson_usual_duration), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            DurationPicker(
                minutes = durationMinutes,
                onMinutesChange = { durationMinutes = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.save(name, course, timezone, rateText.toIntOrNull() ?: 0, durationMinutes, onSaved)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

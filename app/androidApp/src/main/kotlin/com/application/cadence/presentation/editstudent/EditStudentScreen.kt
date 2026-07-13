package com.application.cadence.presentation.editstudent

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.application.cadence.presentation.common.ScreenContainer
import com.application.cadence.presentation.common.TIMEZONE_PRESETS
import com.application.cadence.presentation.common.timezoneLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(viewModel: EditStudentViewModel, onSaved: () -> Unit, onBack: () -> Unit) {
    val student by viewModel.student.collectAsState()

    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf(TIMEZONE_PRESETS.first().first) }
    var timezoneMenuExpanded by remember { mutableStateOf(false) }
    var rateText by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(student) {
        val s = student
        if (s != null && !initialized) {
            name = s.name
            course = s.course
            timezone = s.timezone
            rateText = if (s.hourlyRate > 0) s.hourlyRate.toString() else ""
            initialized = true
        }
    }

    ScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("← Назад", modifier = Modifier.clickable { onBack() }, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Text("Изменить ученика", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("Курс") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = timezoneMenuExpanded,
                onExpandedChange = { timezoneMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = timezoneLabel(timezone),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Часовой пояс") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timezoneMenuExpanded,
                    onDismissRequest = { timezoneMenuExpanded = false }
                ) {
                    TIMEZONE_PRESETS.forEach { (id, label) ->
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
                label = { Text("Ставка ₽/час") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save(name, course, timezone, rateText.toIntOrNull() ?: 0, onSaved) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}

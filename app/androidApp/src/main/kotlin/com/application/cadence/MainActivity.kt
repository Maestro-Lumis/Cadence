package com.application.cadence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.application.cadence.presentation.today.TodayScreen
import com.application.cadence.presentation.today.TodayViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as CadenceApplication
        val factory = TodayViewModelFactory(app.lessonRepository, app.studentRepository)

        setContent {
            TodayScreen(viewModel = viewModel(factory = factory))
        }
    }
}
package com.application.cadence.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
data class StudentProfileRoute(val studentId: Long)
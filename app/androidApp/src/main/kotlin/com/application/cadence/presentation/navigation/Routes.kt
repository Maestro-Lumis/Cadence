package com.application.cadence.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
object AddStudentRoute

@Serializable
object AddLessonRoute

@Serializable
data class StudentProfileRoute(val studentId: Long)
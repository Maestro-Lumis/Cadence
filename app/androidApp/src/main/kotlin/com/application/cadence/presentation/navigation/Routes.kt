package com.application.cadence.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
object StudentsRoute

@Serializable
object AddStudentRoute

@Serializable
object AddLessonRoute

@Serializable
data class StudentProfileRoute(val studentId: Long)

@Serializable
data class AddPackageRoute(val studentId: Long)